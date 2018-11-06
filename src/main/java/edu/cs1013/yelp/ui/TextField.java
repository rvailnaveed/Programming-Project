package edu.cs1013.yelp.ui;

import edu.cs1013.yelp.Constants;
import processing.core.PApplet;
import processing.core.PFont;
import processing.event.KeyEvent;
import processing.event.MouseEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Text field implementation.
 *
 * <p>Features a blinking cursor, wrapping text and ellipsis (when out of focus).</p>
 *
 * <p>Moving the cursor is not supported.</p>
 *
 * @author Jack O'Sullivan
 */
public class TextField extends Widget {
	private static final Set<Character> KEY_BLACKLIST;
	static {
		KEY_BLACKLIST = new HashSet<>();
		KEY_BLACKLIST.add(PApplet.ENTER);
		KEY_BLACKLIST.add(PApplet.DELETE);
		KEY_BLACKLIST.add(PApplet.TAB);
	}

	public static final float DEFAULT_PADDING = 6;
	public static final float CURSOR_WIDTH = 1;
	public static final float CURSOR_PADDING = 1;
	public static final float BLINK_RATE = 2;

	private PFont font;
	private int textSize, textColor;
	private StringBuilder text;
	private String visibleText;
	private float visibleWidth, padding;
	private int alignment;
	private int blinkStartFrame;
	protected Set<Listener> listeners;
	public TextField(WidgetParent parent, PFont font, int textSize, int textColor, String startText, float padding) {
		super(parent);

		this.font = font;
		this.textSize = textSize;
		this.textColor = textColor;
		setAlignment(PApplet.TOP);

		text = new StringBuilder(startText == null ? "" : startText);
		visibleText = "";
		this.padding = padding;
		listeners = new HashSet<>();
	}
	public TextField(WidgetParent parent, PFont font) {
		this(parent, font, Label.DEFAULT_SIZE, Label.DEFAULT_COLOR, null, DEFAULT_PADDING);
	}

	public String getText() {
		return text.toString();
	}
	public void setText(String text) {
		this.text = new StringBuilder(text);
		calculateVisible();
	}
	public float getPadding() {
		return padding;
	}
	public void setPadding(float padding) {
		this.padding = padding;
		calculateVisible();
	}
	public float getTextSize() {
		return textSize;
	}
	public void setTextSize(int size) {
		textSize = size;
		calculateVisible();
	}
	public int getAlignment() {
		return alignment;
	}
	public void setAlignment(int alignment) {
		this.alignment = alignment;
	}
	public void addListener(Listener listener) {
		listeners.add(listener);
	}
	public boolean removeListener(Listener listener) {
		return listeners.remove(listener);
	}
	public PFont getFont() {
		return font;
	}
	public void submit() {
		for (Listener listener : listeners) {
			listener.onSubmit(this, text.toString());
		}
	}

	private void calculateVisible() {
		float availableWidth = getBounds().getWidth() - 2*padding;
		if (availableWidth <= 0) {
			return;
		}
		if (hasFocus()) {
			availableWidth -= CURSOR_PADDING + CURSOR_WIDTH;
		}

		ctx.textFont(font);
		ctx.textSize(textSize);
		if (ctx.textWidth(text.toString()) < availableWidth) {
			visibleText = text.toString();
		} else {
			StringBuilder visible = new StringBuilder();
			if (!hasFocus()) {
				float ellipsisWidth = ctx.textWidth(Constants.ELLIPSIS);
				for (int i = 0; i < text.length() && ctx.textWidth(visible.toString()) + ellipsisWidth < availableWidth; i++) {
					visible.append(text.charAt(i));
				}
				if (visible.length() > 0) {
					visible.setLength(visible.length() - 1);
				}

				visible.append(Constants.ELLIPSIS);
			} else {
				for (int i = text.length() - 1; i >= 0 && ctx.textWidth(visible.toString()) < availableWidth; i--) {
					visible.insert(0, text.charAt(i));
				}
				if (visible.length() > 0) {
					visible.deleteCharAt(0);
				}
			}

			visibleText = visible.toString();
		}
		visibleWidth = ctx.textWidth(visibleText);
	}
	protected float getRealY() {
		Rectangle bounds = getBounds();
		Point location = getLocation();

		float height = textSize + 2*padding;
		float y = location.getY();
		switch (alignment) {
		case PApplet.TOP:
			y += padding;
			break;
		case PApplet.CENTER:
			y += bounds.getHeight() / 2 - height / 2;
			break;
		case PApplet.BOTTOM:
			y += bounds.getHeight() - height;
			break;
		}

		return y;
	}

	@Override
	public boolean onMouseUp(MouseEvent event, boolean inBounds) {
		if (inBounds && wasMouseDownInBounds() && !hasFocus() && event.getButton() == PApplet.LEFT) {
			requestFocus();
			return true;
		}

		return false;
	}
	@Override
	public void onKeyUp(KeyEvent event) {
		if (event.getKey() == PApplet.ENTER) {
			submit();
		}
	}
	@Override
	public void onKeyTyped(KeyEvent event) {
		if (KEY_BLACKLIST.contains(event.getKey()) || event.isControlDown()) {
			return;
		}
		String oldText = text.toString();
		if (event.getKey() == PApplet.BACKSPACE) {
			if (text.length() > 0) {
				text.deleteCharAt(text.length() - 1);
			}
		} else {
			text.append(event.getKey());
		}
		for (Listener listener : listeners) {
			listener.onTextChanged(this, oldText);
		}

		calculateVisible();
	}
	@Override
	public void onResize(Rectangle oldBounds) {
		calculateVisible();
	}
	@Override
	public void onGainFocus() {
		calculateVisible();
	}
	@Override
	public void onLoseFocus(Widget newFocus) {
		calculateVisible();
	}
	@Override
	public void draw() {
		Rectangle bounds = getBounds();
		Point location = getLocation();
		float x = location.getX();
		float y = getRealY();
		float height = textSize + 2*padding;

		ctx.noFill();
		ctx.stroke(0xff000000);
		ctx.rect(x, y, bounds.getWidth() - 1, height - 1);

		ctx.textFont(font);
		ctx.textSize(textSize);
		ctx.fill(textColor);
		ctx.textAlign(PApplet.LEFT, PApplet.TOP);
		ctx.text(visibleText, x + padding, y + padding);

		if (hasFocus()) {
			float diff = ctx.frameCount - blinkStartFrame;
			float interval = ctx.frameRate / BLINK_RATE;
			if (diff > interval) {
				if (diff > 2*interval) {
					blinkStartFrame = ctx.frameCount;
				} else {
					ctx.noStroke();
					ctx.rect(x + padding + visibleWidth + CURSOR_PADDING, y + padding, CURSOR_WIDTH, textSize);
				}
			}
		} else {
			blinkStartFrame = 0;
		}

		super.draw();
	}

	public interface Listener {
		void onTextChanged(TextField which, String oldText);
		void onSubmit(TextField which, String text);
	}
}
