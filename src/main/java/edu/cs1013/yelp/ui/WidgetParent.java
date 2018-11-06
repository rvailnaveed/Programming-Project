package edu.cs1013.yelp.ui;

import processing.core.PApplet;

/**
 * Interface to represent the parent of a Widget
 *
 * <p>This is necessary as the parent of a Widget may not be a Widget itself, e.g. the parent of the root
 * <tt>Widget</tt> is <tt>Application</tt>.
 *
 * @author Jack O'Sullivan
 * @see Widget
 * @see edu.cs1013.yelp.Application
 */
public interface WidgetParent {
	PApplet getProcessingContext();
	Rectangle getBounds();
	Rectangle getVisibleBounds();
	WidgetParent getParent();
	int getZIndex();
	Widget findFocus();

	void onHierarchyChange();
	void onFocusChange(Widget newFocus, boolean notifyParent);
}
