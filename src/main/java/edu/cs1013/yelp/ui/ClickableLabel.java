package edu.cs1013.yelp.ui;

import processing.core.PApplet;
import processing.core.PFont;
import java.util.Set;
import java.util.HashSet;
import processing.event.MouseEvent;

/**
 * A clickable version of a <tt>Label</tt> (e.g. for use as a button)
 *
 * @author Jack O'Sullivan
 * @see Label
 */
public class ClickableLabel extends Label {
	private Set<Listener> listeners;
	public ClickableLabel(WidgetParent parent, PFont font, String text) {
		super(parent, font, text);
		listeners = new HashSet<>();
	}

	public void addListener(Listener listener){
		listeners.add(listener);
	}
	public boolean removeListener(Listener listener){
		return listeners.remove(listener);
	}
	@Override
	public boolean onMouseUp(MouseEvent event, boolean inBounds) {
		if (inBounds && wasMouseDownInBounds() && event.getButton() == PApplet.LEFT) {
			if (!hasFocus()) {
				requestFocus();
			}

			for (Listener l : listeners) {
				l.onClick(this, event);
			}
			return true;
		}

		return false;
	}
	public interface Listener {
		void onClick(ClickableLabel which, MouseEvent e);
	}
}

