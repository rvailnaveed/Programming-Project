package edu.cs1013.yelp.ui;

import processing.core.PApplet;

import java.util.Objects;

/**
 * Representation of a rectangle (location, width and height) in 2D space
 *
 * @author Jack O'Sullivan
 */
public class Rectangle {
	private final Point location;
	private final float width, height;

	public Rectangle(Point location, float width, float height) {
		this.location = location;
		this.width = width;
		this.height = height;
	}
	public Rectangle(float x, float y, float width, float height) {
		this(new Point(x, y), width, height);
	}
	public Rectangle(float width, float height) {
		this(0, 0, width, height);
	}

	// change location constructor
	public Rectangle(Rectangle existing, Point newLocation) {
		this(newLocation, existing.width, existing.height);
	}

	public Point getLocation() {
		return location;
	}
	public float getWidth() {
		return width;
	}
	public float getHeight() {
		return height;
	}

	public boolean contains(Point p) {
		return p.getX() >= location.getX() && p.getX() < location.getX() + width &&
				p.getY() >= location.getY() && p.getY() < location.getY() + height;
	}
	public boolean contains(float x, float y) {
		return contains(new Point(x, y));
	}
	public void draw(PApplet ctx) {
		ctx.rect(location.getX(), location.getY(), width, height);
	}
	public void clip(PApplet ctx) {
		ctx.imageMode(PApplet.CORNER);
		ctx.clip(location.getX(), location.getY(), width, height);
	}
	public Rectangle intersection(Rectangle other) {
		if (other == null) {
			return this;
		}

		float iX = Math.max(location.getX(), other.location.getX());
		float iY = Math.max(location.getY(), other.location.getY());
		float iWidth = Math.min(location.getX() + width, other.location.getX() + other.width) - iX;
		float iHeight = Math.min(location.getY() + height, other.location.getY() + other.height) - iY;

		if (iWidth < 0) {
			iWidth = 0;
		}
		if (iHeight < 0) {
			iHeight = 0;
		}

		return new Rectangle(iX, iY, iWidth, iHeight);
	}

	@Override
	public int hashCode() {
		return Objects.hash(location, width, height);
	}
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Rectangle)) {
			return false;
		}

		Rectangle other = (Rectangle)obj;
		return other.location.equals(location) && other.width == width && other.height == height;
	}
	public boolean equalsIgnoreLocation(Rectangle other) {
		return other.width == width && other.height == height;
	}
	@Override
	public String toString() {
		return String.format("Rectangle[x=%f, y=%f, width=%f, height=%f]",
				location.getX(), location.getY(), width, height);
	}
}
