package com.holub.life.pattern;

import java.util.*;
import com.holub.life.model.Coord;

/** One concrete look of a catalog pattern: a phase in an orientation
 *  (Flyweight). Templates hold no position; every detection of the
 *  same look on any board shares one template, and the position lives
 *  in the {@link Detection}.
 *
 *  <p>A template matches where all its cells are live and every other
 *  cell of its box, plus a one-cell ring around the box, is dead. A
 *  gun's template checks only inside its box (see {@link #ringChecked}),
 *  because the gliders it sends out leave through the ring.
 */
final class Template
{
	final PatternDefinition definition;
	final int phase;
	final int height, width;
	final List<Coord> cells;            // sorted; cells.get(0) is the anchor
	final boolean ringChecked;
	private final Set<Coord> cellSet;

	Template(PatternDefinition definition, int phase, Shape shape,
			 int height, int width, boolean ringChecked)
	{
		this.definition  = definition;
		this.phase       = phase;
		this.cells       = shape.sortedCells();
		this.cellSet     = shape.cells();
		this.height      = height;
		this.width       = width;
		this.ringChecked = ringChecked;
	}

	Coord anchor()                      { return cells.get(0); }
	boolean contains(int row, int col)  { return cellSet.contains(new Coord(row, col)); }
}
