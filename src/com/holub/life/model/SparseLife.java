package com.holub.life.model;

import java.util.*;

/** Life on an unbounded plane, as a pure function on sets of live
 *  cells. Rows and columns may be negative.
 *
 *  <p>{@link LifeModel} is the fast, bounded board. This class is for
 *  code that needs to evolve a small pattern on its own without a
 *  board: working out the phases of a known pattern, or running an
 *  unknown cluster for a few generations to see what it does.
 */
public final class SparseLife
{
	private SparseLife() {}

	/** One generation. The input is not changed.
	 *  @return the live cells of the next generation (immutable).
	 */
	public static Set<Coord> step(Set<Coord> live, Rule rule)
	{
		Map<Coord, Integer> neighbors = new HashMap<>();
		for (Coord cell : live)
			for (int dr = -1; dr <= 1; ++dr)
				for (int dc = -1; dc <= 1; ++dc)
					if (dr != 0 || dc != 0)
						neighbors.merge(new Coord(cell.row() + dr, cell.col() + dc), 1, Integer::sum);

		Set<Coord> next = new HashSet<>();
		neighbors.forEach((cell, count) ->
		{	if (rule.nextState(live.contains(cell), count))
				next.add(cell);
		});
		if (rule.nextState(true, 0))            // B/S0: an isolated cell survives
			for (Coord cell : live)
				if (!neighbors.containsKey(cell))
					next.add(cell);
		return Set.copyOf(next);
	}

	/** Several generations. */
	public static Set<Coord> step(Set<Coord> live, Rule rule, int generations)
	{
		for (int i = 0; i < generations; ++i)
			live = step(live, rule);
		return Set.copyOf(live);
	}
}
