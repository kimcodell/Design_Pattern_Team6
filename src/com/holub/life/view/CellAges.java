package com.holub.life.view;

import java.util.*;
import com.holub.life.model.*;

/** How long each live cell has been alive, worked out from the board's
 *  notifications alone, so the model does not have to store it.
 *
 *  <p>It keeps the generation in which each live cell was born rather
 *  than a counter, so a generation costs time only for the cells that
 *  changed, however many cells stay alive. Cells are also indexed by
 *  birth generation, so the cells that reach a given age in this
 *  generation can be found without looking at the others.
 *
 *  <p>A cell turned on by an edit is born in the current generation. A
 *  cleared or restored board starts over: every live cell is newborn,
 *  because a saved board does not record ages.
 */
final class CellAges
{
	/** Birth generation of every live cell. */
	private final Map<Coord, Long> birth = new HashMap<>();
	/** The same cells, grouped by birth generation. */
	private final Map<Long, Set<Coord>> bornIn = new HashMap<>();
	/** The board's current generation. */
	private long generation;

	CellAges(BoardView board)
	{
		reset(board);
	}

	/** Generations the cell has been alive, counting the current one:
	 *  1 for a cell born in this generation, 0 for a dead cell.
	 */
	long age(int row, int col)
	{
		Long born = birth.get(new Coord(row, col));
		return born == null ? 0 : generation - born + 1;
	}

	/** Live cells whose age became exactly one of <code>ages</code> in
	 *  the current generation.
	 */
	Set<Coord> reached(long[] ages)
	{
		Set<Coord> cells = new HashSet<>();
		for (long age : ages)
		{	Set<Coord> born = bornIn.get(generation - age + 1);
			if (born != null)
				cells.addAll(born);
		}
		return cells;
	}

	/** The board advanced: cells that came alive are born now, cells that
	 *  died are forgotten, the rest grow one generation older.
	 *  <code>changed</code> holds the cells that flipped state, so a changed
	 *  cell is alive now exactly when it was not alive before; the board
	 *  itself is not read (the panel's partial redraw relies on reading
	 *  only the cells it repaints).
	 */
	void stepped(long generation, Set<Coord> changed)
	{
		this.generation = generation;
		for (Coord cell : changed)
			set(cell, !birth.containsKey(cell));
	}

	void edited(BoardEdit applied)
	{
		for (Map.Entry<Coord, Boolean> cell : applied.cells().entrySet())
			set(cell.getKey(), cell.getValue());
	}

	/** Start over from the board as it is now: every live cell is newborn. */
	void reset(BoardView board)
	{
		birth.clear();
		bornIn.clear();
		generation = board.generation();
		for (Coord cell : board.snapshot().liveCells())
			set(cell, true);
	}

	private void set(Coord cell, boolean alive)
	{
		Long born = birth.remove(cell);
		if (born != null)
		{	Set<Coord> group = bornIn.get(born);
			group.remove(cell);
			if (group.isEmpty())
				bornIn.remove(born);
		}
		if (alive)
		{	birth.put(cell, generation);
			bornIn.computeIfAbsent(generation, g -> new HashSet<>()).add(cell);
		}
	}
}
