package edu.cs1013.yelp.ui;

import processing.event.MouseEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Scrollbar widget implementation.
 *
 * @author Jack O'Sullivan
 */
public class Scrollbar extends Widget {
	public static final float DEFAULT_WIDTH = 16;
	public static final float MIN_LENGTH = DEFAULT_WIDTH;
	public static final float DEFAULT_SPEED = 0.002f;
	private static Rectangle ensureBoundsLargeEnough(Rectangle bounds, boolean vertical) {
		if (vertical) {
			if (bounds.getHeight() >= MIN_LENGTH) {
				return bounds;
			} else {
				return new Rectangle(bounds.getLocation(), bounds.getWidth(), MIN_LENGTH);
			}
		} else {
			if (bounds.getWidth() >= MIN_LENGTH) {
				return bounds;
			} else {
				return new Rectangle(bounds.getLocation(), MIN_LENGTH, bounds.getHeight());
			}
		}
	}

	private boolean vertical;
	private float contentSize, viewportSize, viewportPosition;

	private float barWidth, barLength, barRange, barPosition;
	private Rectangle barBounds, blackoutBounds;

	private float scrollSpeed, speed, dragStartPosition;
	private Set<Listener> listeners;
	public Scrollbar(WidgetParent parent, float contentSize, float viewportSize, boolean vertical, float barWidth, float scrollSpeed) {
		super(parent);

		this.contentSize = (contentSize < MIN_LENGTH ? MIN_LENGTH : contentSize);
		this.viewportSize = (viewportSize < MIN_LENGTH ? MIN_LENGTH : viewportSize);
		this.vertical = vertical;
		this.barWidth = barWidth;
		this.scrollSpeed = scrollSpeed;

		listeners = new HashSet<>();
	}
	public Scrollbar(WidgetParent parent, float contentSize, float viewportSize, boolean vertical) {
		this(parent, contentSize, viewportSize, vertical, DEFAULT_WIDTH, DEFAULT_SPEED);
	}

	public boolean isVertical() {
		return vertical;
	}
	public float getContentSize() {
		return contentSize;
	}
	public void setContentSize(float contentSize) {
		float old = this.contentSize;
		this.contentSize = (contentSize < MIN_LENGTH ? MIN_LENGTH : contentSize);

		if (contentSize != old) {
			update(0);
		}
	}
	public float getViewportSize() {
		return viewportSize;
	}
	public void setViewportSize(float viewportSize) {
		float old = this.viewportSize;
		this.viewportSize = (viewportSize < MIN_LENGTH ? MIN_LENGTH : viewportSize);

		if (viewportSize != old) {
			update(0);
		}
	}
	public float getViewportPosition() {
		return viewportPosition;
	}
	public void setViewportPosition(float viewportPosition) {
		float old = this.viewportPosition;
		this.viewportPosition = viewportPosition;

		if (viewportPosition != old) {
			update(0);
		}
	}
	public float getScrollSpeed() {
		return scrollSpeed;
	}
	public void setScrollSpeed(float scrollSpeed) {
		this.scrollSpeed = scrollSpeed;
	}
	public float getBarWidth() {
		return barWidth;
	}
	public void setBarWidth(float barWidth) {
		this.barWidth = barWidth;
		update(0);
	}
	public void addListener(Listener listener) {
		listeners.add(listener);
	}
	public boolean removeListener(Listener listener) {
		return listeners.remove(listener);
	}

	private void update(float scrollAmount) {
		float contentScrollRange = contentSize - viewportSize;
		float viewportFraction = viewportPosition / contentScrollRange;

		Rectangle bounds = getBounds();
		Point location = getLocation();
		float trackLength = (vertical ? bounds.getHeight() : bounds.getWidth());
		barLength = trackLength * (viewportSize / contentSize);
		if (barLength < MIN_LENGTH) {
			barLength = MIN_LENGTH;
		} else if (barLength > trackLength) {
			barLength = trackLength;
		}
		speed = (trackLength / barLength) * scrollSpeed;

		// range of movement the top of the bar can have without the bar flowing over the end
		barRange = trackLength - barLength;
		if (barRange == 0) {
			barPosition = 0;
			viewportPosition = 0;
		} else {
			barPosition = barRange * viewportFraction + scrollAmount;
			if (barPosition < 0) {
				barPosition = 0;
			} else if (barPosition >= barRange) {
				barPosition = barRange;
			}
			viewportPosition = (barPosition / barRange) * contentScrollRange;
		}

		if (vertical) {
			barBounds = new Rectangle(location.getX() + bounds.getWidth() - barWidth, location.getY() + barPosition, barWidth, barLength);
			blackoutBounds = new Rectangle(barBounds.getLocation().getX(), location.getY(), barWidth, bounds.getHeight());
		} else {
			barBounds = new Rectangle(location.getX() + barPosition, location.getY() + bounds.getHeight() - barWidth, barLength, barWidth);
			blackoutBounds = new Rectangle(location.getX(), barBounds.getLocation().getY(), bounds.getWidth(), barWidth);
		}
		if (scrollAmount != 0) {
			for (Listener listener : listeners) {
				listener.onScroll(getViewportPosition());
			}
		}
	}
	private boolean isOverTrack(float x, float y) {
		return (vertical && x >= barBounds.getLocation().getX()) || (!vertical && y >= barBounds.getLocation().getY());
	}

	@Override
	public void onBoundsChange(Rectangle oldBounds) {
		Rectangle verifiedBounds = ensureBoundsLargeEnough(getBounds(), vertical);
		if (!getBounds().equals(verifiedBounds)) {
			setBounds(verifiedBounds);
			return;
		}

		update(0);
	}
	@Override
	public boolean onMouseDown(MouseEvent event, boolean inBounds) {
		if (!inBounds) {
			return false;
		}

		float mousePos = (vertical ? event.getY() : event.getX());
		if (barBounds.contains(event.getX(), event.getY())) {
			dragStartPosition = mousePos - barPosition;
		} else {
			if (isOverTrack(event.getX(), event.getY())) {
				float barStart = (vertical ? barBounds.getLocation().getY() : barBounds.getLocation().getX());
				if (mousePos > barStart + barLength) {
					setViewportPosition(viewportPosition + viewportSize);
				} else if (mousePos < barStart) {
					setViewportPosition(viewportPosition - viewportSize);
				}

				for (Listener listener : listeners) {
					listener.onScroll(getViewportPosition());
				}
			}
		}
		return true;
	}
	@Override
	public boolean onMouseUp(MouseEvent event, boolean inBounds) {
		dragStartPosition = -1;
		return false;
	}
	@Override
	public boolean onMouseDrag(MouseEvent event, boolean inBounds) {
		float dragPosition, mouseDelta;
		if (vertical) {
			dragPosition = event.getY() - barPosition;
			mouseDelta = event.getY() - ctx.pmouseY;
		} else {
			dragPosition = event.getX() - barPosition;
			mouseDelta = event.getX() - ctx.pmouseX;
		}

		if (wasMouseDownInBounds() &&
				!((barPosition == 0 && dragPosition < dragStartPosition) ||
						(barPosition == barRange && dragPosition > dragStartPosition))) {
			update(mouseDelta);
		}
		return wasMouseDownInBounds();
	}
	@Override
	public boolean onMouseScroll(MouseEvent event, int motion, boolean inBounds) {
		if (!inBounds) {
			return false;
		}

		setViewportPosition(getViewportPosition() + motion * (contentSize) * (speed));
		for (Listener listener : listeners) {
			listener.onScroll(getViewportPosition());
		}
		return true;
	}
	@Override
	public void draw() {
		super.draw();

		ctx.noStroke();
		ctx.fill(235);
		blackoutBounds.draw(ctx);

		ctx.fill(200);
		barBounds.draw(ctx);
	}

	public interface Listener {
		void onScroll(float viewportPosition);
	}
}
