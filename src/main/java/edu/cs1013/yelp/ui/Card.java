package edu.cs1013.yelp.ui;

import edu.cs1013.yelp.Util;
import edu.cs1013.yelp.data.Business;
import edu.cs1013.yelp.data.Review;
import edu.cs1013.yelp.data.Tip;
import edu.cs1013.yelp.data.User;
import processing.core.PFont;
import java.lang.NullPointerException;

public class Card {
	public static Widget createBusinessCard(Widget parent,Business business,PFont font, WidgetStack stack) {
		WidgetGroup root = new WidgetGroup(parent,true);
		// sometimes the values are null
		try{
			ClickableLabel businessNameLabel = new ClickableLabel(root, font, business.getName());
			businessNameLabel.addListener((which, e) -> ScreenBuilder.pushSingleBusiness(stack, font, business));
			root.addItem(0.2f, businessNameLabel);
			Label businessAddressLabel = new Label(root, font, business.getFullAddress(", "));
			root.addItem(0.2f, businessAddressLabel);
			String stars = "";
			double rating = business.getRating();
			while(rating > 0) {
				stars += "*";
				rating--;
			}
			Label businessRating = new Label(root,font,stars);
			root.addItem(0.2f, businessRating);
			root.addItem(0.2f, new Label(root, font, "Reviewed "+business.getReviewCount()+ " times"));
		} catch(NullPointerException e){
			e.printStackTrace();
		}
		return root;
	}
	public static Widget createReviewCard(Widget parent, Review review,PFont font, WidgetStack stack) {
		WidgetGroup root = new WidgetGroup(parent,true);
		try {
			ClickableLabel businessName = new ClickableLabel(root,font,review.getBusiness().getName());
            Label reviewRating = new Label(root,font,review.getStarsAsString());
			Label reviewText = new Label(root,font,review.getText());
			businessName.addListener((which, e) -> ScreenBuilder.pushSingleReview(stack, font, review));
			root.addItem(0.2f,businessName);
			root.addItem(0.2f,reviewRating);
			root.addItem(0.2f,reviewText);
			root.addItem(0.2f, new Label(root, font, Util.dateToString(review.getDate())));
		}catch(NullPointerException e){
			e.printStackTrace();
		}
		return root;
	}
	public static Widget createUserCard(Widget parent, User user, PFont font, WidgetStack stack) {
		WidgetGroup root = new WidgetGroup(parent,true);
		try {
			ClickableLabel userName = new ClickableLabel(root,font,user.getName());
			userName.addListener((which, e) -> ScreenBuilder.pushSingleUser(stack, font, user));
			String uFans = Integer.toString(user.getFans());
			uFans = "User Fans: " + uFans;
			Label userFans = new Label(root,font,uFans);
			String uReviewCount = Integer.toString(user.getReviewCount());
			uReviewCount = "Review Count: " + uReviewCount;
			Label userReviewCount = new Label(root,font,uReviewCount);
			root.addItem(0.25f,userName);
			root.addItem(0.25f,userFans);
			root.addItem(0.25f,userReviewCount);
			root.addItem(0.25f);
		}catch(NullPointerException e){
			e.printStackTrace();
		}
		return root;
	}
	public static Widget createTipCard(Widget parent, Tip tip,PFont font,WidgetStack stack) {
		WidgetGroup root = new WidgetGroup(parent,true);
		Label businessName = new Label(root,font,tip.getBusiness().getName());
		Label tipText = new Label(root,font,tip.getText());
		String tLikes = Integer.toString(tip.getLikes());
		tLikes = "Likes: " + tLikes;
		Label tipLikes = new Label(root,font,tLikes);
		root.addItem(0.1f);
		root.addItem(0.2f,businessName);
		root.addItem(0.1f);
		root.addItem(0.2f,tipText);
		root.addItem(0.1f);
		root.addItem(0.2f,tipLikes);
		root.addItem(0.1f);
		return root;
	}
}
