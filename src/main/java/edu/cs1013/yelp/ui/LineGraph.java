package edu.cs1013.yelp.ui;

import org.gicentre.utils.stat.XYChart;
import processing.core.PFont;

/**
 * Widget representation of a giCentre Utils XYChart.
 *
 * @author Jack O'Sullivan
 */
public class LineGraph extends AbstractGraph<XYChart> {
	public LineGraph(WidgetParent parent, PFont font, int textSize, int textColor) {
		super(parent, new XYChart(parent.getProcessingContext()), font, textSize, textColor);
	}
	public LineGraph(WidgetParent parent, PFont font) {
		super(parent, new XYChart(parent.getProcessingContext()), font);
	}

	public void setData(float[] xValues, float[] yValues, float maxX, float maxY) {
		chart.showXAxis(true);
		chart.showYAxis(true);

		chart.setMinX(0);
		chart.setMinY(0);
		chart.setMaxX(maxX);
		chart.setMaxY(maxY);

		chart.setPointSize(5);
		chart.setLineWidth(1);

		chart.setData(xValues, yValues);
	}
	@Override
	public void draw() {
		super.draw();

		chart.draw(getLocation().getX(), getLocation().getY(), getBounds().getWidth(), getBounds().getHeight());
	}
}
