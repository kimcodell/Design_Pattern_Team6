package com.holub.life.model;

import java.util.Set;

/** Immutable copy of a board's state: its size and its live cells.
 *  The rule and boundary are configuration, not state, so they are
 *  not part of a snapshot.
 */
public record BoardSnapshot(int size, Set<Coord> liveCells)
{
	public BoardSnapshot
	{
		if (size <= 0)
			throw new IllegalArgumentException("size must be positive: " + size);
		liveCells = Set.copyOf(liveCells);
	}
}
