package edu.cs1013.yelp.ui;

import edu.cs1013.yelp.Constants;
import processing.core.PApplet;
import processing.core.PFont;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Widget to show some text.
 *
 * <p>Includes custom multi-line rendering (Processing's built-in implementation is missing some features),
 * ellipsis and scrolling.</p>
 *
 * @author Jack O'Sullivan
 * @see ScrollableWidget
 */
public class Label extends ScrollableWidget {
	public static final int DEFAULT_SIZE = 16;
	public static final float DEFAULT_PADDING = 4;
	public static final int DEFAULT_COLOR = 0xff000000;

	private PFont font;
	private String text;
	private boolean ellipsize;
	private int size, textColor;
	private int alignX, alignY;
	private float padding;

	private float lineHeight;
	private float totalTextHeight;
	private List<String> lines;
	public Label(WidgetParent parent, PFont font, String text, boolean ellipsize, int size, int textColor, float padding) {
		super(parent, true, false);

		this.font = font;
		this.text = (text == null ? "null" : text);
		this.ellipsize = ellipsize;
		this.size = size;
		this.textColor = textColor;
		this.padding = padding;

		lines = new ArrayList<>();
		setAlignment(PApplet.LEFT, PApplet.TOP);
		update();
	}
	public Label(WidgetParent parent, PFont font, String text, boolean ellipsize) {
		this(parent, font, text, ellipsize, DEFAULT_SIZE, DEFAULT_COLOR, DEFAULT_PADDING);
	}
	public Label(WidgetParent parent, PFont font, String text) {
		this(parent, font, text, true);
	}

	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = (text == null ? "null" : text);
		update();
	}
	public int getColor() {
		return textColor;
	}
	public void setColor(int textColor) {
		this.textColor = textColor;
	}
	public float getPadding() {
		return padding;
	}
	public void setPadding(float padding) {
		this.padding = padding;
		update();
	}
	public int getSize() {
		return size;
	}
	public void setSize(int size) {
		this.size = size;
		update();
	}
	public int getAlignX() {
		return alignX;
	}
	public int getAlignY() {
		return alignY;
	}
	public void setAlignment(int alignX, int alignY) {
		this.alignX = alignX;
		this.alignY = alignY;
	}
	@Override
	public float getContentSize() {
		float requiredHeight = totalTextHeight + 2*padding;
		return (getViewportSize() > requiredHeight ? getViewportSize() : requiredHeight);
	}
	@Override
	public void onParentChange(WidgetParent old) {
		update();
	}

	private boolean addLine(String line) {
		float maxHeight = (ellipsize ? getViewportSize() - 2*padding : Float.MAX_VALUE);
		float totalHeight = (lines.size() + 1) * lineHeight;
		if (totalHeight > maxHeight) {
			// ellipsize the previous line since it was the last one
			// maxHeight must be at least big enough for one line or this
			// will potentially be out of bounds...
			// it should also be at least as wide as the width of the ellipsis
			if (lines.size() > 0) {
				String lastLine = lines.get(lines.size() - 1);
				if (lastLine != null) {
					StringBuilder newLastLine = new StringBuilder(lastLine);
					if (newLastLine.length() >= Constants.ELLIPSIS.length()) {
						newLastLine.setLength(newLastLine.length() - Constants.ELLIPSIS.length());
					}
					newLastLine.append(Constants.ELLIPSIS);
					lines.set(lines.size() - 1, newLastLine.toString());
				}
			}

			return false;
		}

		totalTextHeight += lineHeight;
		lines.add(line);
		return true;
	}
	private void update() {
		if (ctx == null || font == null) {
			return;
		}

		ctx.textFont(font);
		ctx.textSize(size);
		lineHeight = ctx.textAscent() + ctx.textDescent();
		totalTextHeight = 0;

		float spaceWidth = ctx.textWidth(' ');
		Rectangle bounds = getContentBounds();
		float availableWidth = bounds.getWidth() - 2*padding;
		float minWidth = 2*ctx.textWidth(Constants.ELLIPSIS) + 2*padding;
		if (availableWidth < minWidth) {
			// too small!
			availableWidth = minWidth;
		}

		lines.clear();
		String[] initialLines = text.split("\n");

		for (String line : initialLines) {
			line = line.trim();
			if (line.length() == 0) {
				if (!addLine(null)) {
					return;
				}
				continue;
			}

			if (ctx.textWidth(line) <= availableWidth) {
				if (!addLine(line)) {
					return;
				}
			} else {
				List<String> words = Arrays.asList(line.split(Constants.SPACE));

				float lineWidth;
				do {
					int lastWord = 0;
					lineWidth = 0;
					while (lastWord < words.size() && lineWidth <= availableWidth) {
						lineWidth += ctx.textWidth(words.get(lastWord));
						if (lastWord != words.size() - 1) {
							lineWidth += spaceWidth;
						}

						lastWord++;
					}
					lastWord--;

					// even the first word is too big - split it and hyphenate
					if (lastWord == 0 && lineWidth > availableWidth) {
						String bigWord = words.get(lastWord);
						float wordWidth;
						do {
							int lastChar = 0;
							wordWidth = 0;
							while (lastChar < bigWord.length() && wordWidth <= availableWidth) {
								wordWidth += ctx.textWidth(bigWord.charAt(lastChar));
								lastChar++;
							}
							if (lastWord != words.size() - 1) {
								wordWidth += spaceWidth;
							}
							lastChar--;

							if (wordWidth > availableWidth) {
								if (!addLine(bigWord.substring(0, lastChar) + '-')) {
									return;
								}
								bigWord = bigWord.substring(lastChar + 1, bigWord.length());
							}
						} while (wordWidth > availableWidth);

						// try again now that we have hyphenated the parts that are too long
						words.set(lastWord, bigWord);
						continue;
					}

					if (lineWidth > availableWidth) {
						// we have one too many words to fit
						List<String> fitWords = words.subList(0, lastWord);
						if (!addLine(String.join(Constants.SPACE, fitWords))) {
							return;
						}
						words = words.subList(lastWord, words.size());
					} else {
						if (!addLine(String.join(Constants.SPACE, words))) {
							return;
						}
					}
				} while (lineWidth > availableWidth);
			}
		}
	}

	@Override
	public void onViewportResize(Rectangle oldContentBounds) {
		update();
	}
	@Override
	public void draw() {
		Rectangle contentBounds = getContentBounds();
		Point location = getContentLocation();

		ctx.textFont(font);
		ctx.textSize(size); // must be called AFTER textFont() to apply
		ctx.fill(textColor);
		ctx.textAlign(alignX, PApplet.TOP);

		float x = location.getX();
		switch (alignX) {
		case PApplet.LEFT:
			x += padding;
			break;
		case PApplet.CENTER:
			x += contentBounds.getWidth() / 2;
			break;
		case PApplet.RIGHT:
			x += contentBounds.getWidth() - padding;
			break;
		}

		float textHeight = lineHeight*lines.size();
		float y = location.getY();
		switch (alignY) {
		case PApplet.TOP:
			y += padding;
			break;
		case PApplet.CENTER:
			y += contentBounds.getHeight() / 2 - textHeight / 2;
			break;
		case PApplet.BOTTOM:
			y += contentBounds.getHeight() - textHeight - padding;
			break;
		}

		for (String line : lines) {
			if (line != null) {
				ctx.text(line, x, y);
			}
			y += lineHeight;
		}

		super.draw();
	}
}
