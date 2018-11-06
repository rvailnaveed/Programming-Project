package edu.cs1013.yelp.ui;

import processing.core.PApplet;
import processing.core.PFont;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Content switcher based on tabs implemented via a <tt>WidgetList</tt> of <tt>RadioLabel</tt>s.
 *
 * @author Jack O'Sullivan
 * @see WidgetList
 * @see RadioLabel
 */
public class TabController extends WidgetGroup implements RadioLabel.Listener {
	public static final int H_NAVBUTTON_SIZE = 60;
	public static final int V_NAVBUTTON_SIZE = 150;

	private PFont font;
	private RadioLabel.Model tabModel;
	private Map<RadioLabel, Widget> tabs;
	private Set<Listener> listeners;

	private WidgetGroup tabGroup;
	private WidgetList tabButtonList;
	public TabController(WidgetParent parent, PFont font, boolean vertical, float tabSize) {
		super(parent, vertical);
		this.font = font;
		listeners = new HashSet<>();

		tabModel = new RadioLabel.Model();
		tabModel.addListener(this);
		tabs = new HashMap<>();

		tabGroup = new WidgetGroup(this, !vertical);
		addItem(tabSize, tabGroup);

		tabButtonList = new WidgetList(tabGroup, !vertical);
		tabGroup.addItem(1, tabButtonList);

		// empty space for content
		addItem(1 - tabSize);
	}

	public void addTab(String name, Widget content) {
		RadioLabel button = new RadioLabel(tabButtonList, tabModel, font, name);
		tabButtonList.addItem(isVertical() ? V_NAVBUTTON_SIZE : H_NAVBUTTON_SIZE, button);
		button.setAlignment(PApplet.CENTER, PApplet.CENTER);

		tabs.put(button, content);
		if (tabs.size() == 1) {
			tabModel.select(button);
		}
	}
	public String getSelectedName() {
		if (tabModel.getSelected() == null) {
			return null;
		}

		return tabModel.getSelected().getText();
	}
	public Widget getSelectedContent() {
		return tabs.get(tabModel.getSelected());
	}
	public void setSelected(Widget content) {
		for (Map.Entry<RadioLabel, Widget> tab : tabs.entrySet()) {
			if (tab.getValue() == content) {
				tabModel.select(tab.getKey());
				return;
			}
		}
	}
	public void addListener(Listener listener) {
		listeners.add(listener);
	}
	public boolean removeListener(Listener listener) {
		return listeners.remove(listener);
	}

	protected WidgetGroup getTabGroup() {
		return tabGroup;
	}

	@Override
	public void onSelect(RadioLabel which, boolean alreadySelected) {
		if (alreadySelected) {
			return;
		}

		String oldName = getSelectedName();
		Widget oldContent = getSelectedContent();
		setItem(1, tabs.get(which));
		for (Listener listener : listeners) {
			listener.onSelect(oldName, oldContent);
		}
	}

	public interface Listener {
		void onSelect(String oldTabName, Widget oldContent);
	}
}
