package edu.cs1013.yelp.data;

import edu.cs1013.yelp.Constants;
import edu.cs1013.yelp.Util;
import org.apache.commons.dbcp2.BasicDataSource;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.concurrent.*;

/**
 * Class to manage SQL database access and querying.
 *
 * @author Jack O'Sullivan
 */
public class DataManager {
	private static final String MSG_NOT_INITIALIZED = "Database connection not initialized!";

	private static BasicDataSource dataSource;
	private static final BlockingQueue<Result> resultsQueue = new ArrayBlockingQueue<>(Constants.QUEUE_SIZE);
	private static ExecutorService workerPool;
	private static String backend;

	/**
	 * Initialize a connection to the database via either MySQL or SQLite.
	 *
	 * <p>The configuration is normally read from <tt>database.properties</tt> (hostname, port, credentials etc. for
	 * MySQL and path for SQLite).</p>
	 *
	 * <p>This method also initializes the thread pool for queries.</p>
	 *
	 * @param sqliteDbPath optional path to force using SQLite backend
	 * @throws IOException if <tt>sqliteDbPath</tt> is not null but is unreadable
	 */
	public static void init(String sqliteDbPath) throws IOException {
		Properties dbSettings = new Properties();
		if (sqliteDbPath != null) {
			dbSettings.setProperty("type", "sqlite");
			dbSettings.setProperty("path", sqliteDbPath);
		} else {
			dbSettings.load(DataManager.class.getResourceAsStream("/database.properties"));
		}
		backend = dbSettings.getProperty("type");

		workerPool = Executors.newFixedThreadPool(8);
		dataSource = new BasicDataSource();
		dataSource.setInitialSize(2);
		dataSource.setMaxTotal(64);
		dataSource.setMinIdle(2);
		dataSource.setMaxIdle(8);
		switch (backend) {
		case "mysql":
			System.out.printf("Using MySQL backend @ %s:%s, db name '%s' (user %s)\n", dbSettings.getProperty("server"),
					dbSettings.getProperty("port"), dbSettings.getProperty("database"), dbSettings.getProperty("username"));
			dataSource.setDriverClassName("com.mysql.jdbc.Driver");
			dataSource.setUrl(String.format("jdbc:mysql://%s:%s/%s", dbSettings.getProperty("server"),
					dbSettings.getProperty("port"), dbSettings.getProperty("database")));
			dataSource.setUsername(dbSettings.getProperty("username"));
			dataSource.setPassword(dbSettings.getProperty("password"));
			break;
		case "sqlite":
			File dbFile = new File(dbSettings.getProperty("path"));
			if (!dbFile.canRead()) {
				throw new IOException(String.format("SQLite database at '%s' does not exist / is unreadable",
						dbFile.getAbsolutePath()));
			}

			System.out.printf("Using SQLite backend with database at '%s'\n", dbFile.getAbsolutePath());
			dataSource.setDriverClassName("org.sqlite.JDBC");
			dataSource.setUrl(String.format("jdbc:sqlite:%s", dbFile.getPath()));
			break;
		}
	}
	/**
	 * Cancel all running queries and disconnect from the database.
	 *
	 * @throws SQLException if closing the database connection failed
	 */
	public static void shutdown() throws SQLException {
		workerPool.shutdownNow();
		dataSource.close();
	}
	/**
	 * Create a series of placeholders for use with JDBC <tt>PreparedStatement</tt>
	 *
	 * @param n the number of placeholders to generate
	 * @return a comma-separated list of '?' placeholders
	 * @see PreparedStatement
	 */
	public static String generatePlaceHolders(int n) {
		StringBuilder placeholderBuilder = new StringBuilder();
		for (int i = 0; i < n; i++) {
			placeholderBuilder.append('?');
			if (i != n - 1) {
				placeholderBuilder.append(", ");
			}
		}

		return placeholderBuilder.toString();
	}
	/**
	 * Generate SQL GROUP BY parameters
	 *
	 * @param columnOrders a list of columns and whether the GROUP direction should be ascending or descending
	 * @return a comma-separated list of GROUP BY parameters
	 */
	public static String generateOrder(Map<String, Boolean> columnOrders) {
		StringBuilder orderBuilder = new StringBuilder();
		int i = 0;
		for (Map.Entry<String, Boolean> columnOrder : columnOrders.entrySet()) {
			orderBuilder.append(columnOrder.getKey());
			orderBuilder.append(columnOrder.getValue() ? " DESC" : " ASC");
			if (i != columnOrders.size() - 1) {
				orderBuilder.append(", ");
			}

			i++;
		}

		return orderBuilder.toString();
	}
	/**
	 * Generate SQL GROUP BY parameters for a single column
	 *
	 * @see #generateOrder(Map)
	 */
	public static String generateOrder(String column, boolean descending) {
		return column + (descending ? " DESC" : " ASC");
	}
	/**
	 * Create a JDBC <tt>PreparedStatement</tt>
	 *
	 * @param query the SQL query to create the <tt>PreparedStatement</tt> with
	 * @return a <tt>PreparedStatement</tt>
	 * @throws SQLException if a connection to the database has not yet been established
	 * @see PreparedStatement
	 */
	public static PreparedStatement createStatement(String query) throws SQLException {
		if (dataSource == null) {
			throw new SQLException(MSG_NOT_INITIALIZED);
		}

		return dataSource.getConnection().prepareStatement(query);
	}
	/**
	 * Perform a query for some <tt>Model</tt> on the current thread
	 *
	 * <p>Reflection on the annotations of the <tt>modelType</tt> are used to create a result from each SQL row</p>
	 *
	 * @param modelType the type of model to fill the data with
	 * @param statement a pre-filled statement representing the actual SQL query
	 * @return the results of the query
	 * @see #createStatement(String)
	 */
	public static <ModelType extends Model> List<ModelType> syncQuery(
			Class<ModelType> modelType, PreparedStatement statement) throws SQLException, ReflectiveOperationException, IllegalArgumentException {
		ResultSet sqlResults = null;
		try {
			sqlResults = statement.executeQuery();

			Class idType = (Class)((ParameterizedType)modelType.getGenericSuperclass()).getActualTypeArguments()[0];
			Map<String, Field> columnMappings = Model.getColumnMappings(modelType);
			Map<String, Model.ForeignInfo> foreignMappings = Model.getForeignMappings(modelType);

			List<ModelType> results = new ArrayList<>();
			while (sqlResults.next()) {
				ModelType result = modelType.newInstance();
				for (Map.Entry<String, Field> mapping : columnMappings.entrySet()) {
					int columnIndex = sqlResults.findColumn(mapping.getKey());
					Field modelField = mapping.getValue();
					Class reqType;
					if (modelField.getName().equals("id")) {
						reqType = idType;
					} else {
						reqType = modelField.getType();
					}

					// manual type checking required since (seemingly only with SQLite) types come back
					// wrong from JDBC
					if (reqType.equals(Integer.TYPE)) {
						modelField.setInt(result, sqlResults.getInt(columnIndex));
					} else if (reqType.equals(Boolean.TYPE)) {
						modelField.setBoolean(result, sqlResults.getBoolean(columnIndex));
					} else if (reqType.equals(Float.TYPE)) {
						modelField.setFloat(result, sqlResults.getFloat(columnIndex));
					} else if (reqType.equals(Double.TYPE)) {
						modelField.setDouble(result, sqlResults.getDouble(columnIndex));
					} else if (reqType.equals(String.class)) {
						modelField.set(result, sqlResults.getString(columnIndex));
					} else if (reqType.equals(Date.class)) {
						modelField.set(result, Util.dateFromString(sqlResults.getString(columnIndex)));
					}
				}
				for (Map.Entry<String, Model.ForeignInfo> mapping : foreignMappings.entrySet()) {
					Object foreignId = sqlResults.getObject(mapping.getKey());
					Model.ForeignInfo foreignInfo = mapping.getValue();
					Model foreign = Model.syncFindById(foreignInfo.getType(), foreignInfo.getTable(), foreignId);
					foreignInfo.getTarget().set(result, foreign);
				}

				results.add(result);
			}

			sqlResults.close();
			return results;
		} catch (SQLException | ReflectiveOperationException | IllegalArgumentException ex) {
			if (sqlResults != null) {
				sqlResults.close();
			}

			throw ex;
		}
	}
	/**
	 * Submit a <tt>Model</tt> query to be performed on a worker thread
	 *
	 * @param query the query to run in the background
	 * @param <T> type of <tt>Model</tt> this query will result in
	 * @return the given query
	 * @see AsyncQuery
	 */
	public static <T extends Model> AsyncQuery<T> postQuery(AsyncQuery<T> query) {
		workerPool.submit(query);
		return query;
	}
	/**
	 * Submit a raw query to be performed on a worker thread
	 *
	 * @param query the raw query
	 */
	public static void postRawQuery(RawQuery query) {
		workerPool.submit(query);
	}
	/**
	 * Dispatch any queued results to their relevant listeners on the current thread
	 *
	 * @see Result
	 */
	public static void dispatchResults() {
		while (!resultsQueue.isEmpty()) {
			Result result = resultsQueue.poll();
			if (result == null) {
				return;
			}

			result.dispatch();
		}
	}
	/**
	 * Get the type of database in use
	 *
	 * @return the backend in use by the <tt>DataManager</tt>
	 */
	public static String getBackend() {
		return backend;
	}

	/**
	 * Listener to allow a user to set the placeholder values of a <tt>PreparedStatement</tt>
	 */
	public interface StatementReadyListener {
		/**
		 * Called when the statement has been created
		 *
		 * @param statement a statement whose parameters can be filled
		 */
		void onPrepare(PreparedStatement statement) throws SQLException;
	}

	/**
	 * A class which represents a <tt>Model</tt> query to be run on a worker thread.
	 *
	 * <p>The results of a query will be put in a queue to be dispatched later</p>
	 *
	 * @param <ModelType> the type of <tt>Model</tt> this query will result in
	 * @see #syncQuery(Class, PreparedStatement)
	 * @see #dispatchResults()
	 */
	public static class AsyncQuery<ModelType extends Model> implements Runnable {
		private Class<ModelType> modelType;
		private String queryString;
		private StatementReadyListener sListener;
		private Model.Listener<ModelType> rListener;
		private boolean cancelled;
		public AsyncQuery(Class<ModelType> modelType, String sql, StatementReadyListener sListener,
						  Model.Listener<ModelType> rListener) {
			this.queryString = sql;
			this.modelType = modelType;
			this.sListener = sListener;
			this.rListener = rListener;
		}

		@Override
		public void run() {
			PreparedStatement statement = null;
			try {
				statement = createStatement(queryString);
				if (sListener != null) {
					sListener.onPrepare(statement);
				}

				ModelResult<ModelType> result = new ModelResult<>(syncQuery(modelType, statement), rListener);
				if (!cancelled) {
					resultsQueue.put(result);
				}
			} catch (SQLException | ReflectiveOperationException | InterruptedException | IllegalArgumentException ex) {
				resultsQueue.offer(new ModelResult<>(ex, rListener));
			} finally {
				try {
					if (statement != null) {
						Connection conn = statement.getConnection();
						statement.close();
						conn.close();
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		/**
		 * Cancel this query (in this case just prevents the results from being put in the queue)
		 */
		public void cancel() {
			this.cancelled = true;
		}
	}

	/**
	 * Listener for the results of a raw query run on a worker thread
	 *
	 * @see RawQuery
	 */
	public interface RawListener {
		/**
		 * Called with the results from JDBC
		 *
		 * @param results JDBC result rows
		 * @return some result to be placed in the results queue
		 */
		Result onResults(ResultSet results) throws SQLException;

		/**
		 * Called if this query failed
		 *
		 * @return some result to be placed in the results queue
		 */
		Result onError(Exception ex);
	}

	/**
	 * Class representing a raw query (not resulting in a <tt>Model</tt> to be run on a worker thread)
	 *
	 * @see AsyncQuery
	 */
	public static class RawQuery implements Runnable {
		private String queryString;
		private StatementReadyListener sListener;
		private RawListener rListener;
		public RawQuery(String sql, StatementReadyListener sListener, RawListener rListener) {
			this.queryString = sql;
			this.sListener = sListener;
			this.rListener = rListener;
		}

		@Override
		public void run() {
			PreparedStatement statement = null;
			ResultSet sqlResults = null;
			try {
				statement = createStatement(queryString);
				if (sListener != null) {
					sListener.onPrepare(statement);
				}

				sqlResults = statement.executeQuery();
				resultsQueue.put(rListener.onResults(sqlResults));
				sqlResults.close();
			} catch (SQLException | InterruptedException | IllegalArgumentException ex) {
				resultsQueue.offer(rListener.onError(ex));
			} finally {
				try {
					if (sqlResults != null) {
						sqlResults.close();
					}
					if (statement != null) {
						Connection conn = statement.getConnection();
						statement.close();
						conn.close();
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * An interface representing some kind of queueable query result
	 */
	public interface Result {
		void dispatch();
	}
	private static class ModelResult<ModelType extends Model> implements Result {
		private List<ModelType> results;
		private Model.Listener<ModelType> listener;
		private Exception ex;
		public ModelResult(List<ModelType> results, Model.Listener<ModelType> listener) {
			this.results = results;
			this.listener = listener;
		}
		public ModelResult(Exception ex, Model.Listener<ModelType> listener) {
			this.ex = ex;
			this.listener = listener;
		}

		@Override
		public void dispatch() {
			if (ex != null) {
				listener.onError(ex);
			} else {
				listener.onFind(results);
			}
		}
	}
}
