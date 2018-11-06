package edu.cs1013.yelp.ui;

import edu.cs1013.yelp.Constants;
import processing.core.PApplet;
import processing.core.PFont;
import processing.event.MouseEvent;

/**
 * A <tt>TextField</tt> with a scrollable suggestions list (via <tt>WidgetList</tt>).
 *
 * @author Jack O'Sullivan
 * @see TextField
 * @see WidgetList
 */
public class SuggestionsTextField extends TextField implements ClickableLabel.Listener {
	public static final int LIST_HEIGHT = 120;
	public static final int SUGGESTION_HEIGHT = 30;

	private WidgetList suggestions;
	public SuggestionsTextField(WidgetParent parent, PFont font, int textSize, int textColor, String startText, float padding) {
		super(parent, font, textSize, textColor, startText, padding);

		suggestions = new SuggestionsList(this, true);
		suggestions.setGlobalScroll(true);
	}
	public SuggestionsTextField(WidgetParent parent, PFont font) {
		this(parent, font, Label.DEFAULT_SIZE, Label.DEFAULT_COLOR, null, DEFAULT_PADDING);
	}

	public void addSuggestion(String text) {
		ClickableLabel suggestion = new ClickableLabel(suggestions, getFont(), text);
		suggestion.setAlignment(PApplet.LEFT, PApplet.CENTER);
		suggestion.addListener(this);
		suggestions.addItem(SUGGESTION_HEIGHT, suggestion);

		if (!hasChild(suggestions)) {
			addChild(suggestions);
		}
		suggestions.setBounds(new Rectangle(suggestions.getLocation(), getBounds().getWidth(),
				Math.min(suggestions.getContentSize(), LIST_HEIGHT)));
	}
	public void clearSuggestions() {
		suggestions.clearItems();
		removeChild(suggestions);
	}

	@Override
	public void onMove(Point oldLocation) {
		float fieldHeight = getTextSize() + 2*getPadding();
		suggestions.setLocation(new Point(getLocation().getX(), getRealY() + fieldHeight - 1));
	}
	@Override
	public void onResize(Rectangle oldBounds) {
		suggestions.setBounds(new Rectangle(suggestions.getLocation(), getBounds().getWidth(),
				suggestions.getBounds().getHeight()));
	}
	@Override
	public void onClick(ClickableLabel which, MouseEvent e) {
		setText(which.getText());
		removeChild(suggestions);
	}

	private class SuggestionsList extends WidgetList {
		public SuggestionsList(WidgetParent parent, boolean vertical) {
			super(parent, vertical);
		}

		@Override
		public int getZIndex() {
			if (getParent() == null) {
				return super.getZIndex();
			}

			return getParent().getZIndex() + 1000;
		}
		@Override
		public Rectangle getVisibleBounds() {
			return getBounds();
		}
		@Override
		public boolean onMouseUp(MouseEvent event, boolean inBounds) {
			if (!inBounds) {
				clearSuggestions();
			}

			return false;
		}
		@Override
		public void draw() {
			Point location = getLocation();
			Rectangle bounds = getBounds();

			ctx.fill(Constants.BACKGROUND_COLOR);
			ctx.stroke(0xff000000);
			ctx.rect(location.getX(), location.getY(), bounds.getWidth() - 1, bounds.getHeight());

			super.draw();
		}
	}
}
