package com.holub.life.view;

import java.awt.Color;

/** How {@link UniversePanel} colors a cell (Strategy). The panel keeps
 *  track of how long each live cell has been alive and asks the coloring
 *  for the color; switching colorings changes only the look, never the
 *  board.
 *
 *  <p>The panel redraws only cells that changed. A coloring whose live
 *  color depends on age would make a cell that did not change need a new
 *  color, so it lists the ages at which its color changes
 *  ({@link #colorChangesAt()}) and the panel also redraws the cells that
 *  reach one of them. Keeping the list short (a few steps rather than a
 *  new shade every generation) keeps the partial redraw cheap.
 */
public interface CellColoring
{
	/** Color of a dead cell. */
	Color dead();

	/** Color of a live cell.
	 *  @param age generations the cell has been alive, counting the
	 *             current one: 1 for a cell that was just born or clicked.
	 */
	Color live(long age);

	/** The ages, in increasing order, at which {@link #live} returns a new
	 *  color. Empty (the default) when the color does not depend on age.
	 *  Must match {@link #live} exactly, or cells keep a stale color.
	 */
	default long[] colorChangesAt()
	{
		return new long[0];
	}
}
