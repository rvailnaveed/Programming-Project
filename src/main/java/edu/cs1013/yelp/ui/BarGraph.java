package edu.cs1013.yelp.ui;

import org.gicentre.utils.stat.*;
import processing.core.PFont;

// originally by naveedm, updated by osullj19
/**
 * Widget representation of a giCentre Utils BarChart.
 *
 * @author Rvail Naveed
 * @author Jack O'Sullivan
 */
public class BarGraph extends AbstractGraph<BarChart> {
	public BarGraph(WidgetParent parent, PFont font, int textSize, int textColor) {
		super(parent, new BarChart(parent.getProcessingContext()), font, textSize, textColor);
	}
	public BarGraph(WidgetParent parent, PFont font) {
		super(parent, new BarChart(parent.getProcessingContext()), font);
	}

	public void setData(float[] data, String[] labels, float max, float min) {
		chart.setMaxValue(max);
		chart.setMinValue(min);
		chart.showValueAxis(true);
		chart.showCategoryAxis(true);
		chart.setData(data);
		chart.setBarLabels(labels);
	}
	@Override
	public void draw() {
		super.draw();

		chart.draw(getLocation().getX(), getLocation().getY(), getBounds().getWidth(), getBounds().getHeight());
	}
}

