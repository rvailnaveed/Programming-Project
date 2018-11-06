package edu.cs1013.yelp.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Group of <tt>Widget</tt> where each item's size is a fraction of the total space available to this group.
 *
 * @author Jack O'Sullivan
 */
public class WidgetGroup extends Widget {
	private boolean vertical;
	private List<ItemAttributes> attributes;
	public WidgetGroup(WidgetParent parent, boolean vertical) {
		super(parent);

		this.vertical = vertical;
		attributes = new ArrayList<>();
	}

	public void addItem(int index, float size, Widget item) {
		if (item != null) {
			attributes.add(index, new ItemAttributes(item, size));
			addChild(item);
		} else {
			attributes.add(index, new ItemAttributes(size));
		}

		layout();
	}
	public void addItem(float size, Widget item) {
		addItem(attributes.size(), size, item);
	}
	// add empty space
	public void addItem(int index, float size) {
		addItem(index, size, null);
	}
	public void addItem(float size) {
		addItem(size, null);
	}
	public void setItemSize(int index, float size) {
		attributes.get(index).setSize(size);
		layout();
	}
	public void setItemSize(Widget item, float size) {
		setItemSize(getWidgetIndex(item), size);
	}
	public void setItem(int index, float size, Widget item) {
		ItemAttributes attr = attributes.get(index);

		attr.setSize(size);
		if (attr.widget != null) {
			removeChild(attr.widget);
		}
		attr.widget = item;

		addChild(item);
		layout();
	}
	public void setItem(int index, Widget item) {
		ItemAttributes attr = attributes.get(index);

		setItem(index, attr.size, item);
	}
	public float getRealSize(int index) {
		return attributes.get(index).size;
	}
	public float getRealSize(Widget item) {
		return getRealSize(getWidgetIndex(item));
	}
	public Widget getItem(int index) {
		return attributes.get(index).widget;
	}
	public int itemCount() {
		return attributes.size();
	}
	public boolean isVertical() {
		return vertical;
	}

	private int getWidgetIndex(Widget item) {
		for (int i = 0; i < attributes.size(); i++) {
			if (attributes.get(i).widget == item) {
				return i;
			}
		}
		return -1;
	}
	private void layout() {
		Rectangle bounds = getBounds();
		Point location = getLocation();

		float curPos, baseSize;
		if (vertical) {
			curPos = location.getY();
			baseSize = bounds.getHeight();
		} else {
			curPos = location.getX();
			baseSize = bounds.getWidth();
		}

		float totalRelSize = 0;
		for (ItemAttributes attr : attributes) {
			totalRelSize += attr.size;
			float realSize = attr.size * baseSize;

			if (attr.widget != null) {
				Rectangle newBounds;
				if (vertical) {
					newBounds = new Rectangle(location.getX(), curPos, bounds.getWidth(), realSize);
				} else {
					newBounds = new Rectangle(curPos, location.getY(), realSize, bounds.getHeight());
				}

				attr.widget.setBounds(newBounds);
			}
			curPos += realSize;
		}

		if (totalRelSize > 1) {
			System.err.printf("Warning: WidgetGroup total relative size > 1 (%f)\n", totalRelSize);
		}
	}

	@Override
	protected void onBoundsChange(Rectangle oldBounds) {
		layout();
	}

	private static class ItemAttributes {
		private Widget widget;
		private float size;
		public ItemAttributes(Widget widget, float size) {
			this.widget = widget;

			setSize(size);
		}
		// empty space constructor
		public ItemAttributes(float size) {
			this(null, size);
		}

		public void setSize(float size) {
			if (size < 0 || size > 1) {
				throw new IllegalArgumentException("size must be between 0 and 1");
			}

			this.size = size;
		}
	}
}
