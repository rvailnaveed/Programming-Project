package edu.cs1013.yelp.ui;

import processing.core.PApplet;
import processing.core.PFont;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Scrollable list of many <tt>Widget</tt>s based on a <tt>ScrollableWidget</tt>.
 *
 * <p>Includes optional text to display when the list is empty.</p>
 *
 * @author Jack O'Sullivan
 * @see ScrollableWidget
 * @see WidgetGroup
 */
public class WidgetList extends ScrollableWidget {
	private List<Widget> items;
	private Label emptyText;
	private float totalSize;
	public WidgetList(WidgetParent parent, boolean vertical) {
		super(parent, vertical, false);

		items = new ArrayList<>();
	}

	public void addItem(Widget item) {
		items.add(item);
		item.setParent(this);
		addChild(item);

		onBoundsChange(getBounds());
		onMove(getLocation());
		onResize(getBounds());
	}
	public void addItem(float size, Widget item) {
		applySizedBounds(size, item);
		addItem(item);
	}
	public void addItems(float size, Collection<Widget> items) {
		for (Widget item : items) {
			item.setParent(this);
			applySizedBounds(size, item);
		}
		this.items.addAll(items);
		addChildren(items);

		onBoundsChange(getBounds());
		onMove(getLocation());
		onResize(getBounds());
	}
	public void clearItems() {
		removeChildren(items);
		items.clear();

		layout();
	}
	public int itemCount() {
		return items.size();
	}
	public void setItemSize(Widget item, float size) {
		applySizedBounds(size, item);

		onBoundsChange(getBounds());
		onMove(getLocation());
		onResize(getBounds());
	}
	public void setItemSize(int index, float size) {
		setItemSize(items.get(index), size);
	}
	public void setEmptyText(PFont font, String text) {
		if (emptyText == null) {
			emptyText = new Label(this, font, text);
			emptyText.setAlignment(PApplet.CENTER, PApplet.CENTER);
		} else {
			emptyText.setText(text);
		}
	}

	private Rectangle getSizedBounds(float size, Widget item) {
		Rectangle existingBounds = item.getBounds();
		return (isVertical() ?
				new Rectangle(existingBounds.getLocation(), existingBounds.getWidth(), size) :
				new Rectangle(existingBounds.getLocation(), size, existingBounds.getHeight()));
	}
	private void applySizedBounds(float size, Widget item) {
		item.setBounds(getSizedBounds(size, item));
	}
	private void layout() {
		if (items.size() == 0) {
			if (emptyText != null && !hasChild(emptyText)) {
				emptyText.setBounds(getViewportBounds());
				addChild(emptyText);
			}
			return;
		}

		if (emptyText != null && hasChild(emptyText)) {
			removeChild(emptyText);
		}

		Rectangle bounds = getContentBounds();
		Point location = getContentLocation();

		float curPos = (isVertical() ? location.getY() : location.getX());
		totalSize = 0;
		for (int i = 0; i < items.size(); i++) {
			Widget item = items.get(i);
			Rectangle newBounds;
			float size;
			if (isVertical()) {
				size = item.getBounds().getHeight();
				newBounds = new Rectangle(location.getX(), curPos, bounds.getWidth(), size);
			} else {
				size = item.getBounds().getWidth();
				newBounds = new Rectangle(curPos, location.getY(), size, bounds.getHeight());
			}

			item.setBounds(newBounds);
			curPos += size;
			totalSize += size;
		}
	}

	@Override
	public void onViewportResize(Rectangle oldContentBounds) {
		layout();
	}
	@Override
	public void onMove(Point oldLocation) {
		super.onMove(oldLocation);
		layout();
	}
	@Override
	public void onScroll(float viewportPosition) {
		super.onScroll(viewportPosition);
		layout();
	}
	@Override
	public float getContentSize() {
		return totalSize;
	}
}
