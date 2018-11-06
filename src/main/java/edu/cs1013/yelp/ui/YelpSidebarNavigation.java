package edu.cs1013.yelp.ui;

import processing.core.PFont;

/**
 * <tt>SidebarNavigation</tt> representing the whole Yelp UI.
 *
 * @author Jack O'Sullivan
 * @see SidebarNavigation
 */
public class YelpSidebarNavigation extends SidebarNavigation {
	public YelpSidebarNavigation(WidgetParent parent, PFont font) {
		super(parent, font);

		addTab("HOME", ScreenBuilder.pushHome(new WidgetStack(this), font));
		addTab("BUSINESSES", ScreenBuilder.pushBusinesses(new WidgetStack(this), font));
		addTab("REVIEWS", ScreenBuilder.pushReviews(new WidgetStack(this), font));
	}
}
