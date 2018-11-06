package edu.cs1013.yelp.ui;

import processing.event.MouseEvent;

/**
 * Widget with a scrollbar which appears when its content cannot fit in the available space (viewport)
 *
 * @author Jack O'Sullivan
 * @see Scrollbar
 */
public abstract class ScrollableWidget extends Widget implements Scrollbar.Listener {
	private boolean globalScroll;
	private Scrollbar scrollbar;
	private Rectangle contentBounds;

	public ScrollableWidget(WidgetParent parent, boolean vertical, boolean globalScroll,
							float barWidth, float scrollSpeed) {
		super(parent);

		this.globalScroll = globalScroll;

		scrollbar = new Scrollbar(this, getContentSize(),
				vertical ? getBounds().getHeight() : getBounds().getWidth(), vertical, barWidth, scrollSpeed);
		scrollbar.addListener(this);
		contentBounds = new Rectangle(0, 0);
	}
	public ScrollableWidget(WidgetParent parent, boolean vertical, boolean globalScroll) {
		this(parent, vertical, globalScroll, Scrollbar.DEFAULT_WIDTH, Scrollbar.DEFAULT_SPEED);
	}

	private float getBarWidth() {
		if (getContentSize() <= scrollbar.getViewportSize()) {
			return 0;
		}

		return scrollbar.getBarWidth();
	}
	private Rectangle getScrollbarBounds() {
		Point vpLoc = getLocation();
		Rectangle vpBounds = getBounds();

		Rectangle sbBounds;
		if (isVertical()) {
			sbBounds = new Rectangle(vpLoc.getX() + vpBounds.getWidth() - scrollbar.getBarWidth(), vpLoc.getY(),
					scrollbar.getBarWidth(), vpBounds.getHeight());
		} else {
			sbBounds = new Rectangle(vpLoc.getX(), vpLoc.getY() + vpBounds.getHeight() - scrollbar.getBarWidth(),
					vpBounds.getWidth(), scrollbar.getBarWidth());
		}
		return sbBounds;
	}
	private void updateContentLocation() {
		Point vpLoc = getLocation();

		if (isVertical()) {
			contentBounds = new Rectangle(contentBounds, new Point(vpLoc.getX(), vpLoc.getY() - scrollbar.getViewportPosition()));
		} else {
			contentBounds = new Rectangle(contentBounds, new Point(vpLoc.getX() - scrollbar.getViewportPosition(), vpLoc.getY()));
		}
	}
	private void updateContentBounds() {
		Rectangle vpBounds = getBounds();

		if (isVertical()) {
			contentBounds = new Rectangle(getContentLocation(),
					vpBounds.getWidth() - getBarWidth(), getContentSize());
		} else {
			contentBounds = new Rectangle(getContentLocation(),
					getContentSize(), vpBounds.getHeight() - getBarWidth());
		}
	}

	@Override
	public void onScroll(float viewportPosition) {
		updateContentLocation();
	}
	@Override
	protected void onBoundsChange(Rectangle oldBounds) {
		scrollbar.setBounds(getScrollbarBounds());
	}
	@Override
	protected void onMove(Point oldLocation) {
		updateContentLocation();
	}
	@Override
	protected void onResize(Rectangle oldBounds) {
		scrollbar.setViewportSize(isVertical() ? getBounds().getHeight() : getBounds().getWidth());
		// once to notify change in viewport
		Rectangle oldContentBounds = contentBounds;
		updateContentBounds();
		onViewportResize(oldContentBounds);

		// twice to use new content size in draw and scrollbar
		updateContentBounds();
		scrollbar.setContentSize(getContentSize());
		float barWidth = getBarWidth();
		if (barWidth == 0) {
			removeChild(scrollbar);
		} else if (!hasChild(scrollbar)) {
			addChild(scrollbar);
		}
	}
	@Override
	public boolean onMouseScroll(MouseEvent event, int motion, boolean inBounds) {
		// scroll as long as the mouse is over this frame
		if (inBounds && globalScroll && scrollbar.getBarWidth() > 0) {
			scrollbar.onMouseScroll(event, motion, true);
			return true;
		}
		return false;
	}
	@Override
	public void onParentChange(WidgetParent oldParent) {
		scrollbar.setParent(this);
	}

	abstract float getContentSize();
	protected void onViewportResize(Rectangle oldContentBounds) {}

	public boolean isVertical() {
		return scrollbar.isVertical();
	}
	public float getViewportSize() {
		if (scrollbar == null) {
			return 0;
		}

		return scrollbar.getViewportSize();
	}
	public Rectangle getContentBounds() {
		return contentBounds;
	}
	public Point getContentLocation() {
		return contentBounds.getLocation();
	}
	public Rectangle getViewportBounds() {
		Rectangle vpBounds = getBounds();
		return (isVertical() ?
				new Rectangle(getLocation(), vpBounds.getWidth() - getBarWidth(), vpBounds.getHeight()) :
				new Rectangle(getLocation(), vpBounds.getWidth(), vpBounds.getHeight() - getBarWidth()));
	}
	public Scrollbar getScrollbar() {
		return hasChild(scrollbar) ? scrollbar : null;
	}
	public void setGlobalScroll(boolean globalScroll) {
		this.globalScroll = globalScroll;
	}
}
