package edu.cs1013.yelp.ui;

import processing.event.MouseEvent;

import java.util.NavigableSet;
import java.util.TreeSet;

/**
 * Class which holds a flattened version of the widget hierarchy and performs tasks dependant on z-order.
 *
 * <p>The z-index of a <tt>Widget</tt> determines its draw order as well as whether or not a mouse event counts
 * as being in the bounds of that widget. It is normally represented as its parent's z-index + 1.</p>
 *
 * @author Jack O'Sullivan
 * @see TreeSet
 */
public class OrderedWidgetHierarchy {
	private Widget root;
	private NavigableSet<Widget> flattenedHierarchy;
	public OrderedWidgetHierarchy(Widget root) {
		this.root = root;

		flattenedHierarchy = new TreeSet<>();
		rebuild();
	}

	/**
	 * Flatten the widget hierarchy to build a z-index sorted set.
	 */
	public void rebuild() {
		flattenedHierarchy = new TreeSet<>(root.flattenHierarchy());
		flattenedHierarchy.add(root);
	}
	/**
	 * Dispatches a mouse event to all widgets in the hierarchy, starting from the top down.
	 *
	 * <p>The <tt>inBounds</tt> value is determined by whether or not the event location intersects with the
	 * <tt>Widget</tt>'s visible bounds <em>and</em> if another widget has not already decided to handle (or 'consume')
	 * the event.</p>
	 *
	 * @param type the type of mouse event
	 * @param event the Processing mouse event
	 */
	public void dispatchMouseEvent(MouseEventType type, MouseEvent event) {
		boolean handled = false;
		for (Widget widget : flattenedHierarchy.descendingSet()) {
			boolean inBounds = !handled && widget.getVisibleBounds().contains(event.getX(), event.getY());

			// so that we don't accidentally revert to not having handled the event...
			boolean justHandled = false;
			switch (type) {
			case DOWN:
				widget.setWasMouseDownInBounds(inBounds);
				justHandled = widget.onMouseDown(event, inBounds);
				break;
			case UP:
				justHandled = widget.onMouseUp(event, inBounds);
				widget.setWasMouseDownInBounds(false);
				break;
			case DRAG:
				justHandled = widget.onMouseDrag(event, inBounds);
				break;
			case SCROLL:
				// event.getCount() gives the direction of the scroll for some reason (1 is down, -1 is up)
				// (see https://processing.org/reference/mouseWheel_.html)
				justHandled = widget.onMouseScroll(event, event.getCount(), inBounds);
				break;
			}

			handled = justHandled || handled;
		}
	}
	/**
	 * Draws all of the widgets in the hierarchy from the bottom up.
	 */
	public void draw() {
		for (Widget widget : flattenedHierarchy) {
			Rectangle visibleBounds = widget.getVisibleBounds();
			if (visibleBounds.getWidth() == 0 || visibleBounds.getHeight() == 0) {
				continue;
			}

			widget.getVisibleBounds().clip(widget.getProcessingContext());
			widget.draw();
		}
	}

	public enum MouseEventType {
		DOWN, UP, DRAG, SCROLL;
	}
}
