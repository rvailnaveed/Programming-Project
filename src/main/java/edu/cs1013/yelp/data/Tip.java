package edu.cs1013.yelp.data;

import java.util.Date;

/**
 * <tt>Model</tt> representing a Yelp business tip.
 *
 * @author Jack O'Sullivan
 */
public class Tip extends Model<Integer> {
	public static final String TABLE = "tips";

	@Column(name = "user_id")
	private String userId;
	@Column(name = "business_id")
	private String businessId;
	@Column(name = "likes")
	private int likes;
	@Column(name = "date")
	private Date date;
	@Column(name = "text")
	private String text;

	@ForeignKey(table = User.TABLE, idColumn = "user_id")
	private User user;
	@ForeignKey(table = Business.TABLE, idColumn = "business_id")
	private Business business;

	public int getLikes() {
		return likes;
	}
	public Date getDate() {
		return date;
	}
	public String getText() {
		return text;
	}
	public User getUser() {
		return user;
	}
	public Business getBusiness() {
		return business;
	}

	public static DataManager.AsyncQuery<Tip> findById(int id, Listener<Tip> listener) {
		return findById(Tip.class, TABLE, id, listener);
	}
	public static DataManager.AsyncQuery<Tip> findSome(int number, Listener<Tip> listener) {
		return findSome(Tip.class, TABLE, number, listener);
	}
	public static DataManager.AsyncQuery<Tip> findMostLiked(int limit, Listener<Tip> listener) {
		return findSorted(Tip.class, TABLE, "likes", true, limit, listener);
	}
}
