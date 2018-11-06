package edu.cs1013.yelp.data;

import java.util.Date;
import java.util.List;

/**
 * <tt>Model</tt> representing a Yelp user.
 *
 * @author Jack O'Sullivan
 */
public class User extends Model<String> {
	public static final String TABLE = "users";

	@Column(name = "name")
	private String name;
	@Column(name = "review_count")
	private int reviewCount;
	@Column(name = "creation_date")
	private Date creationDate;
	@Column(name = "friends")
	private String friendIdsString;
	@Column(name = "useful")
	private int useful;
	@Column(name = "funny")
	private int funny;
	@Column(name = "cool")
	private int cool;
	@Column(name = "fans")
	private int fans;
	@Column(name = "average_stars")
	private float averageStars;

	private String[] friendIds;
	private List<User> friends;

	public String getName() {
		return name;
	}
	public int getReviewCount() {
		return reviewCount;
	}
	public Date getCreationDate() {
		return creationDate;
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
	public int getFans() {
		return fans;
	}
	public float getAverageStars() {
		return averageStars;
	}
	public int getFriendCount() {
		if (friendIds != null) {
			return friendIds.length;
		}
		if (friendIdsString == null) {
			return 0;
		}

		friendIds = friendIdsString.split(",");
		return friendIds.length;
	}

	public DataManager.AsyncQuery<User> findFriends(Listener<User> listener) {
		if (friendIdsString == null) {
			return null;
		}
		if (friends != null && listener != null) {
			listener.onFind(friends);
			return null;
		}

		String[] friendIds = friendIdsString.split(",");
		String placeholders = DataManager.generatePlaceHolders(getFriendCount());

		return DataManager.postQuery(new DataManager.AsyncQuery<User>(User.class,
				"SELECT * FROM users WHERE id IN (" + placeholders + ")", s-> {
			for (int i = 0; i < getFriendCount(); i++) {
				s.setString(i + 1, friendIds[i]);
			}
		}, new Listener<User>() {
			@Override
			public void onFind(List<User> results) {
				friends = results;
				if (listener != null) {
					listener.onFind(friends);
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
	// added by sulimanm 
	public DataManager.AsyncQuery<Review> findReviewsWritten(int limit, Listener<Review> listener){
		return Model.findByForeignIdSorted(Review.class, Review.TABLE, "user_id", getId(), Review.TOP_ORDER,
				limit, listener);
	}

	public static DataManager.AsyncQuery<User> findById(String id, Listener<User> listener) {
		return findById(User.class, TABLE, id, listener);
	}
	public static DataManager.AsyncQuery<User> findSome(int number, Listener<User> listener) {
		return findSome(User.class, TABLE, number, listener);
	}
	public static DataManager.AsyncQuery<User> findMostReviews(int limit, Listener<User> listener) {
		return findSorted(User.class, TABLE, "review_count", true, limit, listener);
	}
    public static DataManager.AsyncQuery<User> searchByName(String searchTerm, int limit, Listener<User> listener){
        return search(User.class, TABLE, "name", searchTerm, limit, listener);
    }
}
