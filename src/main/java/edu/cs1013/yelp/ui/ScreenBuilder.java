package edu.cs1013.yelp.ui;

import edu.cs1013.yelp.Util;
import org.gicentre.utils.stat.BarChart;
import org.gicentre.utils.stat.XYChart;
import processing.core.PFont;
import processing.core.PApplet;
import edu.cs1013.yelp.data.*;
import edu.cs1013.yelp.Constants;

import java.util.*;

import java.time.LocalDate;

// originally by sulimanm, updated by osullj19
/**
 * Class to build user interface screens.
 *
 * @author Mohamed Suliman
 * @author Jack O'Sullivan
 */
public class ScreenBuilder {
	public static WidgetStack pushSingleBusiness(WidgetStack stack, PFont font, Business business) {
		WidgetGroup singleBusinessGroup = new WidgetGroup(stack, true);

		Label businessTitleLabel = new Label(singleBusinessGroup, font, business.getName());
		singleBusinessGroup.addItem(0.1f, businessTitleLabel);
		businessTitleLabel.setAlignment(PApplet.CENTER, PApplet.CENTER);
		businessTitleLabel.setSize(30);
		businessTitleLabel.setColor(0xff33adff);

		Label ratingLabel = new Label(singleBusinessGroup, font, "Average rating: "+business.getRating());
		singleBusinessGroup.addItem(0.05f, ratingLabel);
		ratingLabel.setAlignment(PApplet.CENTER, PApplet.CENTER);
		ratingLabel.setSize(20);
		ratingLabel.setColor(0xff33adff);

		Label businessAddressLabel = new Label(singleBusinessGroup, font, business.getFullAddress("\n"));
		singleBusinessGroup.addItem(0.1f, businessAddressLabel);

		Label categoriesLabel = new Label(singleBusinessGroup, font, "Loading categories...", false);
		categoriesLabel.setAlignment(PApplet.LEFT, PApplet.CENTER);
		singleBusinessGroup.addItem(0.05f, categoriesLabel);
		business.findCategories(new Model.Listener<Business.Category>() {
			@Override
			public void onFind(List<Business.Category> results) {
				if (results.size() == 0) {
					categoriesLabel.setText("Categories: none");
					return;
				}

				StringBuilder categoriesText = new StringBuilder("Categories: ");
				for (int i = 0; i < results.size(); i++) {
					categoriesText.append(results.get(i).getName());
					if (i != results.size() - 1) {
						categoriesText.append(", ");
					}
				}
				categoriesLabel.setText(categoriesText.toString());
			}
			@Override
			public void onError(Exception e) {
				e.printStackTrace();
			}
		});

		TabController tabs = new TabController(singleBusinessGroup, font, true, 0.1f);
		singleBusinessGroup.addItem(0.7f, tabs);

		WidgetList topList = new WidgetList(tabs, true);
		WidgetList businessTipsList = new WidgetList(tabs, true);
		WidgetList businessGraphsList = new WidgetList(tabs, true);
		topList.setEmptyText(font, Constants.LOADING_TEXT);
		businessTipsList.setEmptyText(font, Constants.LOADING_TEXT);
		businessGraphsList.setEmptyText(font, Constants.LOADING_TEXT);
		topList.setGlobalScroll(true);
		businessTipsList.setGlobalScroll(true);
		businessGraphsList.setGlobalScroll(true);
		tabs.addListener((oldTabName, oldContent) -> {
			if (tabs.getSelectedContent() == topList && topList.itemCount() == 0) {
				business.findTopReviews(Constants.REVIEW_QUERY_LIMIT, new Model.Listener<Review>() {
					@Override
					public void onFind(List<Review> results) {
						List<Widget> cards = new ArrayList<>(results.size());
						for (Review r : results) {
							cards.add(Card.createReviewCard(null, r, font, stack));
						}

						topList.addItems(Constants.REVIEW_CARD_SIZE, cards);
					}
					@Override
					public void onError(Exception e) {
						e.printStackTrace();
					}
				});
			}
			if (tabs.getSelectedContent() == businessTipsList && businessTipsList.itemCount() == 0) {
				business.findTopTips(Constants.TIP_QUERY_LIMIT, new Model.Listener<Tip>() {
					@Override
					public void onFind(List<Tip> results){
						List<Widget> cards = new ArrayList<>(results.size());
						for (Tip tip : results) {
							cards.add(Card.createTipCard(null, tip, font, stack));
						}

						businessTipsList.addItems(Constants.REVIEW_CARD_SIZE, cards);
					}
					@Override
					public void onError(Exception e){
						e.printStackTrace();
					}
				});
			}
			if (tabs.getSelectedContent() == businessGraphsList && businessGraphsList.itemCount() == 0) {
				business.findReviewStarDistribution(new Business.StarDistributionListener() {
					@Override
					public void onFind(List<Integer> starDistribution) {
						BarGraph graph = new BarGraph(businessGraphsList, font);

						//guess value
						businessGraphsList.addItem(500, graph);

						int max = 0;
						float[] data = new float[starDistribution.size()];
						for (int i = 0; i < data.length; i++) {
							int reviewCount = starDistribution.get(i);
							data[i] = reviewCount;

							if (reviewCount > max) {
								max = reviewCount;
							}
						}

						BarChart chart = graph.getChart();
						graph.setData(data, Util.numericLabels(5, 0, true), max, 0);
						chart.setCategoryAxisLabel("Stars");
						chart.setValueAxisLabel("Review Count");
						chart.setBarGap(30);
						chart.transposeAxes(true);
						chart.setBarColour(0xff0aaaaa);
					}
					@Override
					public void onError(Exception ex) {
						ex.printStackTrace();
					}
				});
				business.findScoreDistribution(new Business.ScoreDistributionListener() {
					@Override
					public void onFind(SortedMap<LocalDate, Float> starDistribution) {
						LineGraph graph = new LineGraph(businessGraphsList, font);
						businessGraphsList.addItem(500, graph);

						Label xLabel = new Label(businessGraphsList, font, "Time (months)");
						xLabel.setAlignment(PApplet.CENTER, PApplet.CENTER);
						businessGraphsList.addItem(30, xLabel);

						float maxYearMonth = 0, minYearMonth = Integer.MAX_VALUE, maxStars = 0, minStars = Integer.MAX_VALUE;
						float[] xValues = new float[starDistribution.size()];
						float[] yValues = new float[starDistribution.size()];
						Iterator<Map.Entry<LocalDate, Float>> points = starDistribution.entrySet().iterator();
						for (int i = 0; i < starDistribution.size(); i++) {
							Map.Entry<LocalDate, Float> point = points.next();
							float yearMonth = Util.dateToYearMonth(point.getKey());
							float stars = point.getValue();

							if (yearMonth > maxYearMonth) {
								maxYearMonth = yearMonth;
							}
							if (yearMonth < minYearMonth) {
								minYearMonth = yearMonth;
							}
							if (stars > maxStars) {
								maxStars = stars;
							}
							if (stars < minStars) {
								minStars = stars;
							}
							xValues[i] = i;
							yValues[i] = stars;
						}
						graph.setData(xValues, yValues, starDistribution.size(), Math.min(maxStars + 0.5f, 5.5f));

						XYChart chart = graph.getChart();
						chart.showXAxis(false);
						chart.setPointColour(0xff50a0a0);
						chart.setXAxisLabel("Time");
						chart.setYAxisLabel("Average review score");
						//chart.setMinX(minYearMonth);
						chart.setMinY(Math.max(minStars - 0.5f, 0.5f));
					}
					@Override
					public void onError(Exception e){
						e.printStackTrace();
					}
				});
			}
		});

		tabs.addTab(business.getName()+"'s Reviews", topList);
		tabs.addTab(business.getName()+"'s Tips", businessTipsList);
		tabs.addTab(business.getName()+"'s Graphs", businessGraphsList);

		stack.push(singleBusinessGroup);
		return stack;
	}

	public static WidgetStack pushSingleUser(WidgetStack stack, PFont font, User user) {
		WidgetGroup singleUserGroup = new WidgetGroup(stack, true);

		Label userNameLabel = new Label(singleUserGroup, font, user.getName());
		singleUserGroup.addItem(0.1f, userNameLabel);
		userNameLabel.setSize(30);
		userNameLabel.setAlignment(PApplet.CENTER, PApplet.CENTER);
		userNameLabel.setColor(0xff33adff);

		singleUserGroup.addItem(0.05f, new Label(singleUserGroup, font, "Review Count: " + user.getReviewCount()));
		singleUserGroup.addItem(0.05f, new Label(singleUserGroup, font, "Average Score: " + user.getAverageStars()));

		TabController tabs = new TabController(singleUserGroup, font, true, 0.1f);
		singleUserGroup.addItem(0.8f, tabs);

		WidgetList topList = new WidgetList(tabs, true);
		topList.setEmptyText(font, Constants.LOADING_TEXT);
		topList.setGlobalScroll(true);
		tabs.addListener((oldTabName, oldContent) -> {
			if (tabs.getSelectedContent() == topList && topList.itemCount() == 0) {
				user.findReviewsWritten(Constants.REVIEW_QUERY_LIMIT, new Model.Listener<Review>() {
					@Override
					public void onFind(List<Review> results) {
						List<Widget> cards = new ArrayList<>(results.size());
						for (Review r : results) {
							cards.add(Card.createReviewCard(topList, r, font, stack));
						}

						topList.addItems(Constants.REVIEW_CARD_SIZE, cards);
					}
					@Override
					public void onError(Exception e) {
						e.printStackTrace();
					}
				});
			}
		});

		tabs.addTab(user.getName()+"'s Reviews", topList);

		stack.push(singleUserGroup);
		return stack;
	}

	public static WidgetStack pushSingleReview(WidgetStack stack, PFont font, Review r) {
		WidgetGroup singleReviewGroup = new WidgetGroup(stack, true);

		WidgetGroup header = new WidgetGroup(singleReviewGroup, true);
		singleReviewGroup.addItem(0.3f, header);

		Label reviewTitleLabel = new Label(header, font,
				r.getReviewer().getName() + "'s review of " + r.getBusiness().getName());
		header.addItem(0.3f, reviewTitleLabel);
		reviewTitleLabel.setAlignment(PApplet.CENTER, PApplet.CENTER);

		Label reviewDateLabel = new Label(header, font, r.getDate().toString());
		header.addItem(0.1f, reviewDateLabel);
		reviewDateLabel.setAlignment(PApplet.CENTER, PApplet.CENTER);

		Label starsLabel = new Label(header, font, r.getStarsAsString());
		header.addItem(0.2f, starsLabel);
		starsLabel.setAlignment(PApplet.CENTER, PApplet.CENTER);

		ClickableLabel userLinkLabel = new ClickableLabel(header, font,
				"See more of " + r.getReviewer().getName() + "'s reviews");
		header.addItem(0.2f, userLinkLabel);
		userLinkLabel.addListener((which, e) -> pushSingleUser(stack, font, r.getReviewer()));
		userLinkLabel.setAlignment(PApplet.CENTER, PApplet.CENTER);

		ClickableLabel businessLinkLabel = new ClickableLabel(header, font,
				"See more reviews for " + r.getBusiness().getName());
		header.addItem(0.2f, businessLinkLabel);
		businessLinkLabel.addListener((which, e) -> pushSingleBusiness(stack, font, r.getBusiness()));
		businessLinkLabel.setAlignment(PApplet.CENTER, PApplet.CENTER);

		Label reviewTextLabel = new Label(singleReviewGroup, font, r.getText(), false);
		singleReviewGroup.addItem(0.5f, reviewTextLabel);
		reviewTextLabel.setGlobalScroll(true);

		WidgetGroup graphGroup = new WidgetGroup(singleReviewGroup, true);
		BarGraph usefulFunnyCoolGraph = new BarGraph(graphGroup, font);
		graphGroup.addItem(1.0f, usefulFunnyCoolGraph);
		singleReviewGroup.addItem(0.2f, graphGroup);
		float[] graphdata = { r.getUseful(), r.getFunny(), r.getCool() };
		String[] graphLabels = { "Useful", "Funny", "Cool" };
		usefulFunnyCoolGraph.getChart().setBarGap(100);
		usefulFunnyCoolGraph.setData(graphdata, graphLabels, 5.0f, 0.0f);

		stack.push(singleReviewGroup);
		return stack;
	}

	public static WidgetStack pushBusinesses(WidgetStack stack, PFont font) {
		WidgetGroup businessGroup = new WidgetGroup(stack, true);

		Label businessTitleLabel = new Label(businessGroup, font, "Businesses");
		businessGroup.addItem(0.1f, businessTitleLabel);
		businessTitleLabel.setSize(30);
		businessTitleLabel.setAlignment(PApplet.CENTER, PApplet.CENTER);

		TabController tabs = new TabController(businessGroup, font, true, 0.1f);
		businessGroup.addItem(0.9f, tabs);

		WidgetList topList = new WidgetList(tabs, true);
		WidgetList mostReviewedList = new WidgetList(tabs, true);
		topList.setEmptyText(font, Constants.LOADING_TEXT);
		mostReviewedList.setEmptyText(font, Constants.LOADING_TEXT);
		topList.setGlobalScroll(true);
		mostReviewedList.setGlobalScroll(true);
		tabs.addListener((oldTabName, oldContent) -> {
			if (tabs.getSelectedContent() == topList && topList.itemCount() == 0) {
				Business.findTopRated(Constants.BUSINESS_QUERY_LIMIT, new Model.Listener<Business>() {
					@Override
					public void onFind(List<Business> results) {
						List<Widget> cards = new ArrayList<>(results.size());
						for (Business b : results) {
							cards.add(Card.createBusinessCard(null, b, font, stack));
						}

						topList.addItems(Constants.BUSINESS_CARD_SIZE, cards);
					}
					@Override
					public void onError(Exception e) {
						e.printStackTrace();
					}
				});
			}
			if (tabs.getSelectedContent() == mostReviewedList && mostReviewedList.itemCount() == 0) {
				Business.findMostReviewed(Constants.BUSINESS_QUERY_LIMIT, new Model.Listener<Business>() {
					@Override
					public void onFind(List<Business> results) {
						List<Widget> cards = new ArrayList<>(results.size());
						for (Business b : results) {
							cards.add(Card.createBusinessCard(null, b, font, stack));
						}

						mostReviewedList.addItems(Constants.BUSINESS_CARD_SIZE, cards);
					}
					@Override
					public void onError(Exception e) {
						e.printStackTrace();
					}
				});
			}
		});

		tabs.addTab("Top Rated", topList);
		tabs.addTab("Most Reviewed", mostReviewedList);

		stack.push(businessGroup);
		return stack;
	}

	public static WidgetStack pushReviews(WidgetStack stack, PFont font) {
		WidgetGroup reviewGroup = new WidgetGroup(stack, true);

		Label reviewTitleLabel = new Label(reviewGroup, font, "Reviews");
		reviewGroup.addItem(0.1f, reviewTitleLabel);
		reviewTitleLabel.setSize(30);
		reviewTitleLabel.setAlignment(PApplet.CENTER, PApplet.BOTTOM);

		TabController tabs = new TabController(reviewGroup, font, true, 0.1f);
		reviewGroup.addItem(0.9f, tabs);

		WidgetList recentList = new WidgetList(tabs, true);
		WidgetList activeUsersList = new WidgetList(tabs, true);
		recentList.setEmptyText(font, Constants.LOADING_TEXT);
		activeUsersList.setEmptyText(font, Constants.LOADING_TEXT);
		recentList.setGlobalScroll(true);
		activeUsersList.setGlobalScroll(true);
		tabs.addListener((oldTabName, oldContent) -> {
			if (tabs.getSelectedContent() == recentList && recentList.itemCount() == 0) {
				Review.findMostRecent(Constants.REVIEW_QUERY_LIMIT, new Model.Listener<Review>() {
					@Override
					public void onFind(List<Review> results) {
						List<Widget> cards = new ArrayList<>(results.size());
						for (Review r : results) {
							cards.add(Card.createReviewCard(null, r, font, stack));
						}

						recentList.addItems(Constants.REVIEW_CARD_SIZE, cards);
					}
					@Override
					public void onError(Exception e) {
						e.printStackTrace();
					}
				});
			}
			if (tabs.getSelectedContent() == activeUsersList && activeUsersList.itemCount() == 0) {
				User.findMostReviews(Constants.USER_QUERY_LIMIT, new Model.Listener<User>() {
					@Override
					public void onFind(List<User> results){
						List<Widget> cards = new ArrayList<>(results.size());
						for (User u : results){
							cards.add(Card.createUserCard(null, u, font, stack));
						}

						activeUsersList.addItems(Constants.USER_CARD_SIZE, cards);
					}
					@Override
					public void onError(Exception e){
						e.printStackTrace();
					}
				});
			}
		});

		tabs.addTab("Most Recent", recentList);
		tabs.addTab("Most Active Reviewers", activeUsersList);

		stack.push(reviewGroup);
		return stack;
	}

	private interface QueryProvider<T extends Model> {
		DataManager.AsyncQuery<T> doSuggestionsQuery(String query, Model.Listener<T> proxyListener);
		DataManager.AsyncQuery<T> doSearchQuery(String query, Model.Listener<T> proxyListener);

		Model.Listener<T> getRealSuggestionsListener(SuggestionsTextField suggestions);
		Model.Listener<T> getRealSearchListener(WidgetList resultsList);
	}
	private static <T extends Model> Widget buildSearch(WidgetParent parent, PFont font, QueryProvider<T> queryProvider) {
		WidgetGroup root = new WidgetGroup(parent, true);

		WidgetGroup controls = new WidgetGroup(root, false);
		root.addItem(0.1f, controls);

		SuggestionsTextField searchText = new SuggestionsTextField(controls, font);
		controls.addItem(0.8f, searchText);
		searchText.setAlignment(PApplet.CENTER);

		ClickableLabel searchButton = new ClickableLabel(controls, font, "Search");
		controls.addItem(0.2f, searchButton);
		searchButton.setAlignment(PApplet.CENTER, PApplet.CENTER);
		searchButton.addListener((which, e) -> searchText.submit());

		WidgetList resultsList = new WidgetList(root, true);
		root.addItem(0.9f, resultsList);
		resultsList.setGlobalScroll(true);

		searchText.addListener(new TextField.Listener() {
			private DataManager.AsyncQuery query;
			@Override
			public void onTextChanged(TextField which, String oldText) {
				if (query != null) {
					query.cancel();
				}
				if (searchText.getText().length() < Constants.MIN_SUGGESTION_LENGTH) {
					searchText.clearSuggestions();
					query = null;
					return;
				}

				query = queryProvider.doSuggestionsQuery(searchText.getText(), new Model.Listener<T>() {
					@Override
					public void onFind(List<T> results) {
						searchText.clearSuggestions();
						queryProvider.getRealSuggestionsListener(searchText).onFind(results);

						query = null;
					}
					@Override
					public void onError(Exception ex) {
						queryProvider.getRealSuggestionsListener(searchText).onError(ex);
					}
				});
			}
			@Override
			public void onSubmit(TextField which, String text) {
				resultsList.clearItems();
				queryProvider.doSearchQuery(searchText.getText(), new Model.Listener<T>() {
					@Override
					public void onFind(List<T> results) {
						resultsList.clearItems();
						queryProvider.getRealSearchListener(resultsList).onFind(results);
					}
					@Override
					public void onError(Exception ex) {
						queryProvider.getRealSearchListener(resultsList).onError(ex);
					}
				});
			}
		});

		return root;
	}
	public static WidgetStack pushHome(WidgetStack stack, PFont font) {
		WidgetGroup homeGroup = new WidgetGroup(stack, true);

		Label titleLabel = new Label(homeGroup, font, "Yelp");
		homeGroup.addItem(0.2f, titleLabel);
		titleLabel.setAlignment(PApplet.CENTER, PApplet.CENTER);
		titleLabel.setSize(60);

		TabController tabs = new TabController(homeGroup, font, true, 0.1f);
		homeGroup.addItem(0.8f, tabs);

		tabs.addTab("Search Businesses", buildSearch(tabs, font, new QueryProvider<Business>() {
			@Override
			public DataManager.AsyncQuery<Business> doSuggestionsQuery(String query, Model.Listener<Business> proxyListener) {
				return Business.searchByName(query, Constants.SUGGESTION_QUERY_LIMIT, proxyListener);
			}
			@Override
			public DataManager.AsyncQuery<Business> doSearchQuery(String query, Model.Listener<Business> proxyListener) {
				return Business.searchByName(query, Constants.BUSINESS_QUERY_LIMIT, proxyListener);
			}
			@Override
			public Model.Listener<Business> getRealSuggestionsListener(SuggestionsTextField suggestions) {
				return new Model.Listener<Business>() {
					@Override
					public void onFind(List<Business> results) {
						for (Business suggestion : results) {
							suggestions.addSuggestion(suggestion.getName());
						}
					}
					@Override
					public void onError(Exception ex) {
						ex.printStackTrace();
					}
				};
			}
			@Override
			public Model.Listener<Business> getRealSearchListener(WidgetList resultsList) {
				return new Model.Listener<Business>() {
					@Override
					public void onFind(List<Business> results) {
						List<Widget> cards = new ArrayList<>(results.size());
						for (Business b : results) {
							cards.add(Card.createBusinessCard(null, b, font, stack));
						}

						resultsList.addItems(Constants.BUSINESS_CARD_SIZE, cards);
					}
					@Override
					public void onError(Exception ex) {
						ex.printStackTrace();
					}
				};
			}
		}));
		tabs.addTab("Search Users", buildSearch(tabs, font, new QueryProvider<User>() {
			@Override
			public DataManager.AsyncQuery<User> doSuggestionsQuery(String query, Model.Listener<User> proxyListener) {
				return User.searchByName(query, Constants.SUGGESTION_QUERY_LIMIT, proxyListener);
			}
			@Override
			public DataManager.AsyncQuery<User> doSearchQuery(String query, Model.Listener<User> proxyListener) {
				return User.searchByName(query, Constants.USER_QUERY_LIMIT, proxyListener);
			}
			@Override
			public Model.Listener<User> getRealSuggestionsListener(SuggestionsTextField suggestions) {
				return new Model.Listener<User>() {
					@Override
					public void onFind(List<User> results) {
						Set<String> found = new HashSet<>();
						for (User suggestion : results) {
							if (found.contains(suggestion.getName())) {
								continue;
							}

							suggestions.addSuggestion(suggestion.getName());
							found.add(suggestion.getName());
						}
					}
					@Override
					public void onError(Exception ex) {
						ex.printStackTrace();
					}
				};
			}
			@Override
			public Model.Listener<User> getRealSearchListener(WidgetList resultsList) {
				return new Model.Listener<User>() {
					@Override
					public void onFind(List<User> results) {
						List<Widget> cards = new ArrayList<>(results.size());
						for (User u : results) {
							cards.add(Card.createUserCard(null, u, font, stack));
						}

						resultsList.addItems(Constants.USER_CARD_SIZE, cards);
					}
					@Override
					public void onError(Exception ex) {
						ex.printStackTrace();
					}
				};
			}
		}));

		stack.push(homeGroup);
		return stack;
	}
}
