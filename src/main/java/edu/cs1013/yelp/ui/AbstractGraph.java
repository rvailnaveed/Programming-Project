package edu.cs1013.yelp.ui;

import org.gicentre.utils.stat.AbstractChart;
import processing.core.PFont;

/**
 * Widget to represent some kind of giCentre Utils chart
 *
 * @author Jack O'Sullivan
 * @param <T> type of chart represented by this graph
 */
public abstract class AbstractGraph<T extends AbstractChart> extends Widget {
	private PFont font;
	private int textSize;
	protected T chart;
	protected AbstractGraph(WidgetParent parent, T chart, PFont font, int textSize, int textColor) {
		super(parent);

		this.font = font;
		this.textSize = textSize;
		this.chart = chart;
		setTextColor(textColor);
	}
	protected AbstractGraph(WidgetParent parent, T chart, PFont font) {
		this(parent, chart, font, Label.DEFAULT_SIZE, Label.DEFAULT_COLOR);
	}

	public void setTextColor(int color) {
		chart.setAxisColour(color);
		chart.setAxisLabelColour(color);
		chart.setAxisValuesColour(color);
	}
	public T getChart() {
		return chart;
	}
	@Override
	public void draw() {
		super.draw();

		ctx.textFont(font);
		ctx.textSize(textSize);
	}
}
