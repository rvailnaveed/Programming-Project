package edu.cs1013.yelp.ui;

import processing.core.PApplet;
import processing.event.KeyEvent;
import processing.event.MouseEvent;

import java.util.Collection;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Base class representing all UI Widgets.
 *
 * <p>Creates a parent-child hierarchy.</p>
 *
 * @author Jack O'Sullivan
 */
public abstract class Widget implements WidgetParent, Comparable<Widget> {
	private static final boolean DEBUG_BOUNDS = false;

	protected PApplet ctx;
	private Rectangle bounds, visibleBounds;
	private WidgetParent parent;
	private final Set<Widget> children;
	private boolean focus;
	private boolean wasMouseDownInBounds;
	public Widget(WidgetParent parent) {
		this.parent = parent;
		this.ctx = (parent == null ? null : parent.getProcessingContext());

		// use a CopyOnWriteArraySet as children may be modified in event listeners
		children = new CopyOnWriteArraySet<>();
		bounds = new Rectangle(0, 0);
	}

	@Override
	public PApplet getProcessingContext() {
		return ctx;
	}
	@Override
	public Rectangle getBounds() {
		return bounds;
	}
	@Override
	public WidgetParent getParent() {
		return parent;
	}
	public Point getLocation() {
		return getBounds().getLocation();
	}
	public void setParent(WidgetParent parent) {
		WidgetParent old = this.parent;
		this.parent = parent;
		this.ctx = (parent == null ? null : parent.getProcessingContext());
		updateVisibleBounds();

		onParentChange(old);
		for (Widget child : children) {
			child.setParent(this);
		}
	}
	public void setBounds(Rectangle bounds) {
		Rectangle old = this.bounds;
		this.bounds = bounds;
		updateVisibleBounds();

		if (!bounds.equals(old)) {
			onBoundsChange(old);
		}
		if (!getLocation().equals(old.getLocation())) {
			onMove(old.getLocation());
		}
		if (!bounds.equalsIgnoreLocation(old)) {
			onResize(old);
		}
	}
	public void setLocation(Point location) {
		setBounds(new Rectangle(getBounds(), location));
	}
	public boolean wasMouseDownInBounds() {
		return wasMouseDownInBounds;
	}
	public void setWasMouseDownInBounds(boolean downInBounds) {
		this.wasMouseDownInBounds = downInBounds;
	}
	public Rectangle getVisibleBounds() {
		return visibleBounds;
	}

	protected void onBoundsChange(Rectangle oldBounds) {}
	protected void onMove(Point oldLocation) {}
	protected void onResize(Rectangle oldBounds) {}
	protected void onGainFocus() {}
	protected void onLoseFocus(Widget newFocus) {}
	protected void onParentChange(WidgetParent oldParent) {}
	public boolean onMouseDown(MouseEvent event, boolean inBounds) {
		return false;
	}
	public boolean onMouseUp(MouseEvent event, boolean inBounds) {
		return false;
	}
	public boolean onMouseDrag(MouseEvent event, boolean inBounds) {
		return false;
	}
	public boolean onMouseScroll(MouseEvent event, int motion, boolean inBounds) {
		return false;
	}
	public void onKeyDown(KeyEvent event) {}
	public void onKeyUp(KeyEvent event) {}
	public void onKeyTyped(KeyEvent event) {}

	protected void addChild(Widget child) {
		children.add(child);
		if (parent != null) {
			parent.onHierarchyChange();
		}
	}
	protected boolean removeChild(Widget child) {
		boolean result = children.remove(child);
		if (result && parent != null) {
			parent.onHierarchyChange();
		}

		return result;
	}
	protected boolean addChildren(Collection<Widget> children) {
		boolean result = this.children.addAll(children);
		if (result && parent != null) {
			parent.onHierarchyChange();
		}

		return result;
	}
	protected boolean removeChildren(Collection<Widget> children) {
		boolean result = this.children.removeAll(children);
		if (result && parent != null) {
			parent.onHierarchyChange();
		}

		return result;
	}
	protected boolean hasChild(Widget child) {
		return children.contains(child);
	}
	protected int childCount() {
		return children.size();
	}
	protected Iterable<Widget> childrenIter() {
		return children;
	}
	private void updateVisibleBounds() {
		visibleBounds = getBounds().intersection(getParent().getVisibleBounds());
	}

	@Override
	public int getZIndex() {
		if (parent == null) {
			return 0;
		}

		return parent.getZIndex() + 1;
	}
	public NavigableSet<Widget> flattenHierarchy() {
		NavigableSet<Widget> flattened = new TreeSet<>(children);
		for (Widget child : children) {
			flattened.addAll(child.flattenHierarchy());
		}

		return flattened;
	}
	@Override
	public void onHierarchyChange() {
		if (parent != null) {
			parent.onHierarchyChange();
		}
	}

	@Override
	public void onFocusChange(Widget newFocus, boolean notifyParent) {
		if (notifyParent && parent != null) {
			parent.onFocusChange(newFocus, true);
		}

		for (Widget child : children) {
			if (child.equals(newFocus)) {
				continue;
			}

			child.onFocusChange(newFocus, false);
		}
		if (!focus && newFocus == this) {
			focus = true;
			onGainFocus();
		} else if (focus && !newFocus.equals(this)) {
			focus = false;
			onLoseFocus(newFocus);
		}
	}
	@Override
	public Widget findFocus() {
		if (hasFocus()) {
			return this;
		}
		for (Widget child : children) {
			if (child.hasFocus()) {
				return child;
			}
		}
		if (parent != null) {
			return parent.findFocus();
		}

		return null;
	}
	protected void requestFocus() {
		if (parent == null) {
			return;
		}

		onFocusChange(this, true);
	}
	public boolean hasFocus() {
		return focus;
	}

	public void draw() {
		if (DEBUG_BOUNDS) {
			ctx.noClip();
			ctx.stroke(0xffffffff);
			ctx.fill(0x7fff0000);
			bounds.draw(ctx);
		}
	}
	public void notifyKeyDown(KeyEvent event) {
		for (Widget child : children) {
			if (child.hasFocus()) {
				child.onKeyDown(event);
			}

			child.notifyKeyDown(event);
		}
	}
	public void notifyKeyUp(KeyEvent event) {
		for (Widget child : children) {
			if (child.hasFocus()) {
				child.onKeyUp(event);
			}

			child.notifyKeyUp(event);
		}
	}
	public void notifyKeyTyped(KeyEvent event) {
		for (Widget child : children) {
			if (child.hasFocus()) {
				child.onKeyTyped(event);
			}

			child.notifyKeyTyped(event);
		}
	}

	@Override
	public int compareTo(Widget other) {
		if (other.getZIndex() == getZIndex()) {
			return 1;
		}

		return Integer.compare(getZIndex(), other.getZIndex());
	}
}
