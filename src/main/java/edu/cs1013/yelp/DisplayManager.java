package edu.cs1013.yelp;

import com.jogamp.newt.opengl.GLWindow;
import edu.cs1013.yelp.ui.Rectangle;
import processing.awt.PSurfaceAWT;
import processing.core.PApplet;
import processing.core.PSurface;
import processing.opengl.PSurfaceJOGL;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Detects window resizing and provides full screen control functionality
 */
public class DisplayManager {
	public static final int DEFAULT_WIDTH = 1280;
	public static final int DEFAULT_HEIGHT = 720;

	private PApplet ctx;
	private Rectangle size, preFullscreenSize;
	private boolean fullscreen;
	private Set<Listener> listeners;
	public DisplayManager() {
		listeners = new HashSet<>();
	}

	/**
	 * Shows/hides the title bar at the top of the Processing window.
	 *
	 * <p>As this functionality  is not exposed by the Processing API, is necessary to implement it manually for each
	 * type of PSurface which has a title bar.</p>
	 *
	 * @param visible whether or not the window title bar should be shown
	 * @see PSurface
	 * @see PSurfaceAWT
	 * @see PSurfaceJOGL
	 */
	public void setTitlebarVisible(boolean visible) {
		PSurface surface = ctx.getSurface();
		if (surface instanceof PSurfaceAWT) {
			Frame frame = ((PSurfaceAWT.SmoothCanvas)surface.getNative()).getFrame();

			frame.removeNotify();
			frame.setUndecorated(!visible);
			frame.addNotify();
		}
		if (surface instanceof PSurfaceJOGL) {
			GLWindow window = (GLWindow)surface.getNative();

			window.setUndecorated(!visible);
		}
	}
	/**
	 * 'Attach' this <tt>DisplayManager</tt> to a <tt>PApplet</tt>.
	 *
	 * <p>Registers a listener to be called just before draw to detect window resizing and makes the window
	 * resizable.</p>
	 *
	 * @param ctx the Processing applet to attach to
	 */
	public void attach(PApplet ctx) {
		if (this.ctx != null) {
			this.ctx.unregisterMethod("pre", this);
		}

		this.ctx = ctx;
		ctx.getSurface().setResizable(true);
		ctx.registerMethod("pre", this);
	}
	/**
	 * Convenience method - get the current size of the window as a <tt>Rectangle</tt>
	 *
	 * @return the current size as a Rectangle
	 * @see Rectangle
	 */
	public Rectangle getSize() {
		return size;
	}
	public boolean isFullscreen() {
		return fullscreen;
	}
	/**
	 * Set the fullscreen state of the Processing window
	 *
	 * @param fullscreen whether the window should be put into or taken out of fullscreen mode
	 */
	public void setFullscreen(boolean fullscreen) {
		if (fullscreen == this.fullscreen) {
			return;
		}

		PSurface surface = ctx.getSurface();
		if (!this.fullscreen) {
			preFullscreenSize = size;

			setTitlebarVisible(false);
			surface.setResizable(false);
			surface.setSize(ctx.displayWidth, ctx.displayHeight);
			surface.setAlwaysOnTop(true);
			surface.setLocation(0, 0);
		} else {
			setTitlebarVisible(true);
			surface.setResizable(true);
			surface.setSize((int)preFullscreenSize.getWidth(), (int)preFullscreenSize.getHeight());
			surface.setAlwaysOnTop(false);

			preFullscreenSize = null;
		}
		this.fullscreen = fullscreen;
	}
	/**
	 * Toggle the fullscreen state of the Processing window
	 * @see #setFullscreen(boolean)
	 */
	public void toggleFullscreen() {
		setFullscreen(!fullscreen);
	}
	public void addListener(Listener listener) {
		listeners.add(listener);
	}
	public boolean removeListener(Listener listener) {
		return listeners.remove(listener);
	}

	/**
	 * Called before <tt>PApplet.draw()</tt>
	 */
	public void pre() {
		if (size == null || size.getWidth() != ctx.width || size.getHeight() != ctx.height) {
			// we have been resized!
			Rectangle oldSize = size;
			size = new Rectangle(ctx.width, ctx.height);

			for (Listener listener : listeners) {
				listener.onResize(oldSize);
			}
		}
	}

	/**
	 * Listener to detect when the window has been resized
	 */
	public interface Listener {
		/**
		 * Called when the window size changes
		 *
		 * @param oldSize - the size of the window before the resize occurred
		 */
		void onResize(Rectangle oldSize);
	}
}
