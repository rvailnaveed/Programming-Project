package edu.cs1013.yelp.data;

import edu.cs1013.yelp.Util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class representing a row from a table in a SQL database.
 *
 * <p>Provides several convenience methods for commonly used queries.</p>
 *
 * @author Jack O'Sullivan
 * @param <IdType> the Java type of the ID column for a subclass of <tt>Model</tt>
 * @see DataManager
 */
public abstract class Model<IdType> {
	@Column(name = "id")
	private IdType id;

	public Model() {}
	public Model(IdType id) {
		this.id = id;
	}
	public IdType getId() {
		return id;
	}

	private static final Map<Class<? extends Model>, Map<String, Field>> columnMappingsCache = new HashMap<>();
	private static final Map<Class<? extends Model>, Map<String, ForeignInfo>> foreignMappingsCache = new HashMap<>();
	public static Map<String, Field> getColumnMappings(Class<? extends Model> modelType) {
		if (columnMappingsCache.containsKey(modelType)) {
			return columnMappingsCache.get(modelType);
		}

		Map<String, Field> columnMappings = new HashMap<>();
		// getDeclaredFields() doesn't return inherited fields (e.g. id)
		List<Field> allFields = Util.getAllFields(modelType);
		for (Field declaredField : allFields) {
			if (declaredField.isAnnotationPresent(Column.class)) {
				declaredField.setAccessible(true);
				Column sqlColumn = declaredField.getAnnotation(Column.class);
				columnMappings.put(sqlColumn.name(), declaredField);
			}
		}

		columnMappingsCache.put(modelType, columnMappings);
		return columnMappings;
	}
	@SuppressWarnings("unchecked")
	public static Map<String, ForeignInfo> getForeignMappings(Class<? extends Model> modelType) {
		if (foreignMappingsCache.containsKey(modelType)) {
			return foreignMappingsCache.get(modelType);
		}

		Map<String, ForeignInfo> foreignMappings = new HashMap<>();
		// getDeclaredFields() doesn't return inherited fields (e.g. id)
		List<Field> allFields = Util.getAllFields(modelType);
		for (Field declaredField : allFields) {
			if (declaredField.isAnnotationPresent(Model.ForeignKey.class)) {
				declaredField.setAccessible(true);
				Model.ForeignKey foreignKey = declaredField.getAnnotation(Model.ForeignKey.class);
				foreignMappings.put(foreignKey.idColumn(),
						new ForeignInfo((Class<? extends Model>)declaredField.getType(),
								foreignKey.table(), declaredField));
			}
		}

		foreignMappingsCache.put(modelType, foreignMappings);
		return foreignMappings;
	}

	public static <T extends Model> T syncFindById(Class<T> modelType, Connection conn, String table, Object id)
			throws SQLException, ReflectiveOperationException {
		PreparedStatement s = conn.prepareStatement(String.format("SELECT * FROM %s WHERE ID = ?", table));
		s.setObject(1, id);
		List<T> results = DataManager.syncQuery(modelType, s);

		if (results.size() == 0) {
			return null;
		}
		return results.get(0);
	}
	public static <T extends Model> T syncFindById(Class<T> modelType, String table, Object id)
			throws SQLException, ReflectiveOperationException {
		PreparedStatement s = DataManager.createStatement(String.format("SELECT * FROM %s WHERE ID = ?", table));
		s.setObject(1, id);
		List<T> results = DataManager.syncQuery(modelType, s);
		Connection conn = s.getConnection();
		s.close();
		conn.close();

		if (results.size() == 0) {
			return null;
		}
		return results.get(0);
	}
	protected static <T extends Model> DataManager.AsyncQuery<T> findById(Class<T> modelType, String table, Object id, Listener<T> listener) {
		return DataManager.postQuery(new DataManager.AsyncQuery<>(modelType, String.format("SELECT * FROM %s WHERE id = ?", table),
				s -> s.setObject(1, id), listener));
	}
	protected static <T extends Model> DataManager.AsyncQuery<T> findAll(
			Class<T> modelType, String table, Listener<T> listener) {
		return DataManager.postQuery(new DataManager.AsyncQuery<>(modelType, String.format("SELECT * FROM %s", table), null, listener));
	}
	protected static <T extends Model> DataManager.AsyncQuery<T> findSome(
			Class<T> modelType, String table, int number, Listener<T> listener) {
		return DataManager.postQuery(new DataManager.AsyncQuery<>(modelType, String.format("SELECT * FROM %s LIMIT ?", table),
				s -> s.setInt(1, number), listener));
	}
	protected static <T extends Model> DataManager.AsyncQuery<T> findSorted(
			Class<T> modelType, String table, Map<String, Boolean> sortColumns, int limit, Listener<T> listener) {
		return DataManager.postQuery(new DataManager.AsyncQuery<>(modelType, String.format("SELECT * FROM %s ORDER BY %s LIMIT ?",
				table, DataManager.generateOrder(sortColumns)), s -> s.setInt(1, limit), listener));
	}
	protected static <T extends Model> DataManager.AsyncQuery<T> findSorted(
			Class<T> modelType, String table, String sortColumn, boolean descending, int limit,
			Listener<T> listener) {
		return findSorted(modelType, table, Util.singleEntryMap(sortColumn, descending), limit, listener);
	}
	protected static <T extends Model> DataManager.AsyncQuery<T> findByForeignIdSorted(
			Class<T> modelType, String table, String idColumn, Object id, Map<String, Boolean> sortColumns, int limit, Listener<T> listener) {
		String sql = String.format("SELECT * FROM %s WHERE %s = ? ORDER BY %s LIMIT ?", table, idColumn,
				DataManager.generateOrder(sortColumns));
		return DataManager.postQuery(new DataManager.AsyncQuery<>(modelType, sql, s -> {
			s.setObject(1, id);
			s.setInt(2, limit);
		}, listener));
	}
	protected static <T extends Model> DataManager.AsyncQuery<T> findbyForeignIdSorted(
			Class<T> modelType, String table, String idColumn, Object id, String sortColumn, boolean descending,
			int limit, Listener<T> listener) {
		return findByForeignIdSorted(modelType, table, idColumn, id, Util.singleEntryMap(sortColumn, descending), limit, listener);
	}
	protected static <T extends Model> DataManager.AsyncQuery<T> search(Class<T> modelType, String table, String searchColumn,
												   String searchTerm, int limit, Listener<T> listener) {
		String sql = String.format("SELECT * FROM %s WHERE %s LIKE ? ESCAPE '!' LIMIT ?", table, searchColumn);
		return DataManager.postQuery(new DataManager.AsyncQuery<>(modelType, sql, s -> {
			s.setString(1, Util.toSqlSearch(searchTerm));
			s.setInt(2, limit);
		}, listener));
	}
	protected static <T extends Model> DataManager.AsyncQuery<T> searchSorted(Class<T> modelType, String table, String searchColumn,
												   String searchTerm, String sortColumn, boolean descending, int limit,
														 Listener<T> listener) {
		String sql = String.format("SELECT * FROM %s WHERE %s LIKE ? ORDER BY %s LIMIT ?", table, searchColumn,
				DataManager.generateOrder(sortColumn, descending));

		return DataManager.postQuery(new DataManager.AsyncQuery<>(modelType, sql, s -> {
			s.setString(1, Util.toSqlSearch(searchTerm));
			s.setInt(2, limit);
		}, listener));
	}


	/**
	 * Listener for the results of a <tt>Model</tt> query run on a worker thread
	 *
	 * @see edu.cs1013.yelp.data.DataManager.AsyncQuery
	 */
	public interface Listener<T extends Model> {
		/**
		 * Called with the results of a query
		 * @param results list of Model results
		 */
		void onFind(List<T> results);

		/**
		 * Called when a query fails
		 *
		 * @param ex
		 */
		void onError(Exception ex);
	}

	/**
	 * Annotation to mark a field as a representation of a column in a SQL table
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface Column {
		/**
		 * @return the SQL column name
		 */
		String name();
	}

	/**
	 * Annoation to mark a field as an ID referencing a row in another table
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface ForeignKey {
		/**
		 * @return the foreign table name
		 */
		String table();

		/**
		 * @return the name of the column which represents the ID in the foreign table
		 */
		String idColumn();
	}
	public static class ForeignInfo {
		private Class<? extends Model> type;
		private String table;
		private Field target;
		public ForeignInfo(Class<? extends Model> type, String table, Field target) {
			this.type = type;
			this.table = table;
			this.target = target;
		}

		public Class<? extends Model> getType() {
			return type;
		}
		public String getTable() {
			return table;
		}
		public Field getTarget() {
			return target;
		}
	}
}
