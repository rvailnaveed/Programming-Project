package edu.cs1013.yelp.ui;

import processing.core.PApplet;
import processing.core.PFont;

/**
 * A <tt>TabController</tt> where each tab's content is represented by a <tt>WidgetStack</tt>.
 *
 * <p>A back button pops the top Widget off the selected tab's stack (as long as the stack is not empty)</p>.
 *
 * @author Jack O'Sullivan
 * @see TabController
 * @see WidgetStack
 */
public class SidebarNavigation extends TabController {
	public static final float SIDEBAR_SIZE = 0.28f;
	public static final float BACK_SIZE = 0.05f;

	protected PFont font;

	public SidebarNavigation(WidgetParent parent, PFont font) {
		super(parent, font, false, SIDEBAR_SIZE);

		ClickableLabel backButton = new ClickableLabel(getTabGroup(), font, "BACK");
		getTabGroup().setItemSize(0, 1 - BACK_SIZE);
		getTabGroup().addItem(0, BACK_SIZE, backButton);
		backButton.setAlignment(PApplet.LEFT, PApplet.CENTER);
		backButton.setSize(12);
		backButton.addListener((which, e) -> {
			if (getSelectedContent() != null && getSelectedContent().size() > 1) {
				getSelectedContent().pop();
			}
		});
	}

	@Override
	public void addTab(String name, Widget content) {
		if (!(content instanceof WidgetStack)) {
			throw new IllegalArgumentException("Each tab must be a WidgetStack");
		}
		WidgetStack contentStack = (WidgetStack)content;
		if (contentStack.size() == 0) {
			throw new IllegalArgumentException("Each navigation item must have at least one item in its content stack");
		}

		super.addTab(name, contentStack);
	}

	@Override
	public WidgetStack getSelectedContent() {
		return (WidgetStack)super.getSelectedContent();
	}
}
