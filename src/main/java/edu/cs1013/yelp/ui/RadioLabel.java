package edu.cs1013.yelp.ui;

import java.util.ArrayList;
import processing.core.PFont;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import processing.event.MouseEvent;

/**
 * Radio button implementation.
 *
 * @author Jack O'Sullivan
 * @see Model
 */
public class RadioLabel extends ClickableLabel implements ClickableLabel.Listener {
	private Model model;
	public RadioLabel(WidgetParent parent, Model model, PFont font, String text) {
		super(parent, font, text);
		addListener(this);

		this.model = model;

		model.add(this);
	}

	private void onSelect(RadioLabel old) {
		setColor(0xff0000ff);
	}
	private void onUnselect(RadioLabel newLabel) {
		setColor(0xff000000);
	}
	private void onRemove() {
		model = null;
	}

	@Override
	public void onClick(ClickableLabel which, MouseEvent e) {
		if (model != null) {
			model.select(this);
		}
	}

	/**
	 * Controls a group of <tt>RadioLabel</tt>.
	 *
	 * @see RadioLabel
	 */
	public static class Model {
		private List<RadioLabel> radioLabels;
		private RadioLabel selected;
		private Set<Listener> listeners;
		public Model() {
			radioLabels = new ArrayList<>();
			listeners = new HashSet<>();
		}

		private void add(RadioLabel label) {
			radioLabels.add(label);
		}

		public void select(RadioLabel label) {
			RadioLabel oldSelected = selected;
			selected = label;
			label.onSelect(oldSelected);

			for (RadioLabel other : radioLabels) {
				if (other == label) {
					continue;
				}

				other.onUnselect(label);
			}
			for (Listener listener : listeners) {
				listener.onSelect(label, oldSelected == label);
			}
		}
		public boolean remove(RadioLabel label) {
			return radioLabels.remove(label);
		}
		public RadioLabel getSelected() {
			return selected;
		}
		public void addListener(Listener listener) {
			listeners.add(listener);
		}
		public boolean removeListener(Listener listener) {
			return listeners.remove(listener);
		}
	}
	public interface Listener {
		void onSelect(RadioLabel which, boolean alreadySelected);
	}
}
