package edu.cs1013.yelp.ui;

import java.util.Stack;

/**
 * A Widget which draws the item from the top of a <tt>Stack</tt> in its bounds.
 *
 * @author Jack O'Sullivan
 * @see Stack
 */
public class WidgetStack extends Widget {
	private Stack<Widget> stack;
	public WidgetStack(WidgetParent parent) {
		super(parent);

		stack = new Stack<>();
	}
	public void push(Widget item) {
		if (stack.size() > 0) {
			removeChild(stack.peek());
		}

		item.setBounds(getBounds());
		stack.push(item);
		addChild(item);
	}
	public Widget pop() {
		if (stack.size() == 0) {
			return null;
		}

		Widget popped = stack.pop();
		removeChild(popped);

		if (stack.size() > 0) {
			Widget item = stack.peek();
			item.setBounds(getBounds());
			addChild(item);
		}
		return popped;
	}
	public int size() {
		return stack.size();
	}

	@Override
	protected void onBoundsChange(Rectangle oldBounds) {
		if (stack.size() == 0) {
			return;
		}

		stack.peek().setBounds(getBounds());
	}
	@Override
	protected void onParentChange(WidgetParent oldParent) {
		for (Widget item : stack) {
			item.setParent(this);
		}
	}
}
