package com.holub.life.view;

import java.awt.Graphics2D;

/** Something drawn over the board by {@link UniversePanel}, such as
 *  outlines around patterns. Overlays are drawn on the screen after the
 *  board image, never into it, so they do not disturb the panel's
 *  changed-cells-only drawing.
 */
public interface BoardOverlay
{
	/** Draw over the board.
	 *  @param g          clipped to the area being repainted; the
	 *                    overlay may change its settings freely.
	 *  @param cellPixels size of one cell in pixels.
	 */
	void paint(Graphics2D g, int cellPixels);

	/** Called once when the overlay is added.
	 *  @param repaint asks the panel to repaint everything; call it
	 *                 when what the overlay draws has changed.
	 */
	default void attach(Runnable repaint) {}
}
