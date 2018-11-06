package edu.cs1013.yelp.data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

/**
 * <tt>Model</tt> representing a Yelp business.
 *
 * @author Jack O'Sullivan
 */
public class Business extends Model<String> {
	public static final String TABLE = "businesses";
	public static final Map<String, Boolean> TOP_RATED_ORDER;
	static {
		TOP_RATED_ORDER = new LinkedHashMap<>();
		TOP_RATED_ORDER.put("stars", true);
		TOP_RATED_ORDER.put("review_count", true);
		TOP_RATED_ORDER.put("name", false);
	}

	@Column(name = "name")
	private String name;
	@Column(name = "neighbourhood")
	private String neighbourhood;
	@Column(name = "address")
	private String address;
	@Column(name = "city")
	private String city;
	@Column(name = "state")
	private String state;
	@Column(name = "postal_code")
	private String postalCode;
	@Column(name = "latitude")
	private double latitude;
	@Column(name = "longitude")
	private double longitude;
	@Column(name = "stars")
	private double rating;
	@Column(name = "review_count")
	private int reviewCount;
	@Column(name = "is_open")
	private boolean isOpen;

	private List<Category> categories;

	public String getName() {
		return name;
	}
	public String getNeighbourhood() {
		return neighbourhood;
	}
	public String getAddress() {
		return address;
	}
	public String getCity() {
		return city;
	}
	public String getState() {
		return state;
	}
	public String getPostalCode() {
		return postalCode;
	}
	public double getLatitude() {
		return latitude;
	}
	public double getLongitude() {
		return longitude;
	}
	public double getRating() {
		return rating;
	}
	public int getReviewCount() {
		return reviewCount;
	}
	public boolean isOpen() {
		return isOpen;
	}
	// originally by sulimanm, updated by osullj19
	public String getFullAddress(String separator) {
		StringBuilder businessAddress = new StringBuilder();
		if (getNeighbourhood() != null) businessAddress.append(getNeighbourhood()+separator);
		if (getAddress() != null) businessAddress.append(getAddress()+separator);
		if (getCity() != null) businessAddress.append(getCity()+separator);
		if (getState() != null) businessAddress.append(getState());
		return businessAddress.toString();
	}

	public DataManager.AsyncQuery<Category> findCategories(Listener<Category> listener) {
		if (categories != null && listener != null) {
			listener.onFind(categories);
		}

		String sql = "SELECT categories.* FROM business_category LEFT JOIN " +
			"categories ON category_id = categories.id WHERE business_id = ?";
		return DataManager.postQuery(new DataManager.AsyncQuery<>(Category.class, sql, s -> s.setString(1, getId()), new Listener<Category>() {
			@Override
			public void onFind(List<Category> results) {
				categories = results;
				if (listener != null) {
					listener.onFind(categories);
				}
			}
			@Override
			public void onError(Exception ex) {
				if (listener != null) {
					listener.onError(ex);
				}
			}
		}));
	}
	public DataManager.AsyncQuery<Review> findTopReviews(int limit, Listener<Review> listener) {
		return Model.findByForeignIdSorted(Review.class, Review.TABLE, "business_id", getId(), Review.TOP_ORDER,
				limit, listener);
	}
	public DataManager.AsyncQuery<Tip> findTopTips(int limit, Listener<Tip> listener) {
		return Model.findbyForeignIdSorted(Tip.class, Tip.TABLE, "business_id", getId(), "likes", true, limit, listener);
	}
	// descending star distribution (starDistribution[0] == number of 5 star reviews)
	public void findReviewStarDistribution(StarDistributionListener listener) {
		String sql = String.format("SELECT stars, COUNT(*) FROM %s WHERE business_id = ? GROUP BY STARS",
				Review.TABLE);
		DataManager.postRawQuery(new DataManager.RawQuery(sql, s -> s.setString(1, getId()), new DataManager.RawListener() {
			@Override
			public DataManager.Result onResults(ResultSet results) throws SQLException {
				List<Integer> starDistribution = new ArrayList<>(5);
				for (int i = 0; i < 5; i++) {
					starDistribution.add(0);
				}
				while (results.next()) {
					starDistribution.set(5 - results.getInt("stars"), results.getInt("COUNT(*)"));
				}

				return () -> listener.onFind(starDistribution);
			}
			@Override
			public DataManager.Result onError(Exception ex) {
				return () -> listener.onError(ex);
			}
		}));
	}
	public void findScoreDistribution(ScoreDistributionListener listener) {
		SortedMap<LocalDate, Float> scoreDistribution = new TreeMap<>();

		String sql = null;
		switch (DataManager.getBackend()) {
		case "mysql":
			sql = String.format("SELECT YEAR(date) AS year, MONTH(date) as month, AVG(stars) " +
					"FROM %s WHERE business_id = ? GROUP BY year ASC, month ASC", Review.TABLE);
			break;
		case "sqlite":
			sql = String.format("SELECT strftime('%%Y', date) AS year, strftime('%%m', date) AS month, AVG(stars) " +
					"FROM %s WHERE business_id = ? GROUP BY year, month", Review.TABLE);
			break;
		}
		DataManager.postRawQuery(new DataManager.RawQuery(sql, s -> s.setString(1, getId()), new DataManager.RawListener() {
			@Override
			public DataManager.Result onResults(ResultSet results) throws SQLException {
				while (results.next()) {
					scoreDistribution.put(LocalDate.of(results.getInt("year"), results.getInt("month"), 1),
							results.getFloat("AVG(stars)"));
				}

				return () -> listener.onFind(scoreDistribution);
			}
			@Override
			public DataManager.Result onError(Exception ex) {
				return () -> listener.onError(ex);
			}
		}));
	}

	public static DataManager.AsyncQuery<Business> findById(String id, Listener<Business> listener) {
		return findById(Business.class, TABLE, id, listener);
	}
	public static DataManager.AsyncQuery<Business> findSome(int number, Listener<Business> listener) {
		return findSome(Business.class, TABLE, number, listener);
	}
	public static DataManager.AsyncQuery<Business> findMostReviewed(int limit, Listener<Business> listener) {
		return findSorted(Business.class, TABLE, "review_count", true, limit, listener);
	}
	public static DataManager.AsyncQuery<Business> findTopRated(int limit, Listener<Business> listener) {
		return findSorted(Business.class, TABLE, TOP_RATED_ORDER, limit, listener);
	}
	public static DataManager.AsyncQuery<Business> searchByName(String searchTerm, int limit, Listener<Business> listener) {
		return searchSorted(Business.class, TABLE, "name", searchTerm, "name", false, limit, listener);
	}

	/**
	 * <tt>Model</tt> representing a Yelp business category.
	 */
	public static class Category extends Model<Integer> {
		public static final String TABLE = "categories";

		@Column(name = "name")
		private String name;

		private static List<Category> cache;

		public String getName() {
			return name;
		}

		public static DataManager.AsyncQuery<Category> findAll(Listener<Category> listener) {
			if (cache != null && listener != null) {
				listener.onFind(cache);
			}

			return findAll(Category.class, TABLE, new Listener<Category>() {
				@Override
				public void onFind(List<Category> results) {
					cache = results;
					if (listener != null) {
						listener.onFind(cache);
					}
				}
				@Override
				public void onError(Exception ex) {
					if (listener != null) {
						listener.onError(ex);
					}
				}
			});
		}
	}
	public interface StarDistributionListener {
		void onFind(List<Integer> starDistribution);
		void onError(Exception ex);
	}
	public interface ScoreDistributionListener {
		void onFind(SortedMap<LocalDate, Float> starDistribution);
		void onError(Exception ex);
	}
}
