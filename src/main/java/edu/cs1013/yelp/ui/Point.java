package edu.cs1013.yelp.ui;

import java.util.Objects;

/**
 * Representation of a point in 2D space
 *
 * @author Jack O'Sullivan
 */
public class Point {
	private final float x, y;
	public Point(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public float getX() {
		return x;
	}
	public float getY() {
		return y;
	}

	@Override
	public int hashCode() {
		return Objects.hash(x, y);
	}
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Point)) {
			return false;
		}

		Point other = (Point)obj;
		return other.x == x && other.y == y;
	}
}
