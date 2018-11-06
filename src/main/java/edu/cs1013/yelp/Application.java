package edu.cs1013.yelp;

import edu.cs1013.yelp.data.DataManager;
import edu.cs1013.yelp.ui.*;
import processing.core.PApplet;
import processing.core.PFont;
import processing.event.KeyEvent;
import processing.event.MouseEvent;

import java.sql.SQLException;

/**
 * Main application class and entry point.
 *
 * <p>Holds the root Widget and dispatches input events via an <tt>OrderedWidgetHierarchy</tt>.</p>
 *
 * <p>Also dispatches SQL query results on draw thread.</p>
 *
 * @author	Jack O'Sullivan
 * @see OrderedWidgetHierarchy
 * @see DataManager
 * @see DisplayManager
 * @see YelpSidebarNavigation
 */
public class Application extends PApplet implements WidgetParent, DisplayManager.Listener {
	private static String[] args;

	private DisplayManager dpyManager;
	private PFont font;
	private Widget root;
	private OrderedWidgetHierarchy hierarchy;

	@Override
	public void settings() {
		size(DisplayManager.DEFAULT_WIDTH, DisplayManager.DEFAULT_HEIGHT);
	}
	@Override
	public void setup() {
		try {
			DataManager.init(args.length > 0 ? args[0] : null);

			dpyManager = new DisplayManager();
			dpyManager.addListener(this);
			dpyManager.attach(this);
			surface.setTitle(Constants.WINDOW_TITLE);
			font = createFont(Constants.FONT_FILE, Label.DEFAULT_SIZE);

			root = new YelpSidebarNavigation(this, font);
			hierarchy = new OrderedWidgetHierarchy(root);
		} catch (Exception e) {
			e.printStackTrace();
			exit();
		}
	}
	@Override
	public void dispose() {
		super.dispose();

		try {
			DataManager.shutdown();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void draw() {
		DataManager.dispatchResults();

		background(Constants.BACKGROUND_COLOR);
		hierarchy.draw();
	}
	@Override
	public void onResize(Rectangle oldSize) {
		root.setBounds(dpyManager.getSize());
	}
	@Override
	public void mousePressed(MouseEvent event) {
		hierarchy.dispatchMouseEvent(OrderedWidgetHierarchy.MouseEventType.DOWN, event);
	}
	@Override
	public void mouseReleased(MouseEvent event) {
		hierarchy.dispatchMouseEvent(OrderedWidgetHierarchy.MouseEventType.UP, event);
	}
	@Override
	public void mouseDragged(MouseEvent event) {
		hierarchy.dispatchMouseEvent(OrderedWidgetHierarchy.MouseEventType.DRAG, event);
	}
	@Override
	public void mouseWheel(MouseEvent event) {
		hierarchy.dispatchMouseEvent(OrderedWidgetHierarchy.MouseEventType.SCROLL, event);
	}
	@Override
	public void keyPressed(KeyEvent event) {
		if (root.hasFocus()) root.onKeyDown(event);
		root.notifyKeyDown(event);
	}
	@Override
	public void keyReleased(KeyEvent event) {
		if (event.getKeyCode() == java.awt.event.KeyEvent.VK_F11) {
			dpyManager.toggleFullscreen();
			return;
		}

		if (root.hasFocus()) root.onKeyUp(event);
		root.notifyKeyUp(event);
	}
	@Override
	public void keyTyped(KeyEvent event) {
		if (root.hasFocus()) root.onKeyTyped(event);
		root.notifyKeyTyped(event);
	}
	@Override
	public void onHierarchyChange() {
		if (hierarchy == null) {
			return;
		}

		hierarchy.rebuild();
	}


	@Override
	public PApplet getProcessingContext() {
		return this;
	}
	@Override
	public Rectangle getBounds() {
		return dpyManager.getSize();
	}
	@Override
	public Rectangle getVisibleBounds() {
		return dpyManager.getSize();
	}
	@Override
	public WidgetParent getParent() {
		return null;
	}
	@Override
	public int getZIndex() {
		return 0;
	}
	@Override
	public Widget findFocus() {
		if (root.hasFocus()) {
			return root;
		}

		return null;
	}
	@Override
	public void onFocusChange(Widget newFocus, boolean notifyParent) {}

	public static void main(String[] args) {
		Application.args = args;
		PApplet.main(Application.class);
	}
}
