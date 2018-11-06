package edu.cs1013.yelp.data;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <tt>Model</tt> representing a Yelp review.
 *
 * @author Jack O'Sullivan
 */
public class Review extends Model<String> {
	public static final String TABLE = "reviews";
	public static final Map<String, Boolean> TOP_ORDER;
	static {
		TOP_ORDER = new LinkedHashMap<>();
		TOP_ORDER.put("stars", true);
		TOP_ORDER.put("useful", true);
		TOP_ORDER.put("funny", true);
		TOP_ORDER.put("cool", true);
	}

	@Column(name = "stars")
	private int stars;
	@Column(name = "date")
	private Date date;
	@Column(name = "useful")
	private int useful;
	@Column(name = "funny")
	private int funny;
	@Column(name = "cool")
	private int cool;
	@Column(name = "text")
	private String text;

	@ForeignKey(table = User.TABLE, idColumn = "user_id")
	private User reviewer;
	@ForeignKey(table = Business.TABLE, idColumn = "business_id")
	private Business business;

	public int getStars() {
		return stars;
	}
	public String getStarsAsString() {
		StringBuilder starsBuilder = new StringBuilder();
		for (int star = getStars(); star > 0; star--) {
			starsBuilder.append('*');
		}
		return starsBuilder.toString();
	}
	public Date getDate() {
		return date;
	}
	public int getUseful() {
		return useful;
	}
	public int getFunny() {
		return funny;
	}
	public int getCool() {
		return cool;
	}
	public String getText() {
		return text;
	}
	public User getReviewer() {
		return reviewer;
	}
	public Business getBusiness() {
		return business;
	}

	public static DataManager.AsyncQuery<Review> findById(String id, Listener<Review> listener) {
		return findById(Review.class, TABLE, id, listener);
	}
	public static DataManager.AsyncQuery<Review> findSome(int number, Listener<Review> listener) {
		return findSome(Review.class, TABLE, number, listener);
	}
	public static DataManager.AsyncQuery<Review> findMostUseful(int limit, Listener<Review> listener) {
		return findSorted(Review.class, TABLE, "useful", true, limit, listener);
	}
	// added by sulimanm
	public static void findMostRecent(int limit, Listener<Review> listener) {
		findSorted(Review.class, TABLE, "date", true, limit, listener);
	}
}
