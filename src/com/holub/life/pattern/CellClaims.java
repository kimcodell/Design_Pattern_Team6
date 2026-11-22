package com.holub.life.pattern;

import java.util.*;
import com.holub.life.model.*;

/** The live cells of one board snapshot, and which of them a
 *  {@link DetectionStage} has already explained. Each stage looks only
 *  at unclaimed cells, so a cell belongs to at most one detection.
 *  Positions are resolved through the board's {@link Boundary}, so a
 *  TORUS board is seen as joined at the edges.
 */
final class CellClaims
{
	private final int size;
	private final Boundary boundary;
	private final boolean[] live;
	private final boolean[] claimed;
	private final List<Coord> liveCells;    // row-major order

	CellClaims(BoardView board)
	{
		size     = board.size();
		boundary = board.boundary();
		live     = new boolean[size * size];
		claimed  = new boolean[size * size];
		for (Coord c : board.snapshot().liveCells())
			live[c.row() * size + c.col()] = true;
		List<Coord> cells = new ArrayList<>();
		for (int i = 0; i < live.length; ++i)
			if (live[i]) cells.add(new Coord(i / size, i % size));
		liveCells = List.copyOf(cells);
	}

	int size() { return size; }

	/** The board cell at a position that may be off the board, or null
	 *  if the boundary says the position is always dead.
	 */
	Coord locate(int row, int col)
	{
		return boundary.locate(row, col, size).orElse(null);
	}

	/** Live, whether claimed or not. Off-board positions follow the boundary. */
	boolean isAlive(int row, int col)
	{
		Coord c = locate(row, col);
		return c != null && live[index(c)];
	}

	boolean isFree(Coord cell)          { return live[index(cell)] && !claimed[index(cell)]; }
	void claim(Coord cell)              { claimed[index(cell)] = true; }
	List<Coord> liveCells()             { return liveCells; }

	/** A group of unclaimed live cells that lie near each other. */
	record Cluster(Set<Coord> cells, Set<Coord> unwrapped) {}

	/** Unclaimed live cells grouped so that cells at most two apart
	 *  (in rows and columns) are in the same cluster: far enough to keep
	 *  a pulsar's quarters together, near enough to separate patterns
	 *  that cannot affect each other in the next generation.
	 *  {@code unwrapped} has the same cells as positions that are next
	 *  to each other even across a TORUS edge, so a cluster can be run
	 *  on its own.
	 */
	List<Cluster> unclaimedClusters()
	{
		boolean[] seen = new boolean[live.length];
		List<Cluster> clusters = new ArrayList<>();
		for (Coord start : liveCells)
		{
			if (claimed[index(start)] || seen[index(start)])
				continue;
			Set<Coord> cells = new HashSet<>(), unwrapped = new HashSet<>();
			Deque<Coord> todo = new ArrayDeque<>();
			todo.push(start);
			seen[index(start)] = true;
			Map<Coord, Coord> unwrappedOf = new HashMap<>();
			unwrappedOf.put(start, start);
			while (!todo.isEmpty())
			{
				Coord cell = todo.pop();
				Coord at = unwrappedOf.get(cell);
				cells.add(cell);
				unwrapped.add(at);
				for (int dr = -2; dr <= 2; ++dr)
					for (int dc = -2; dc <= 2; ++dc)
					{
						Coord near = locate(at.row() + dr, at.col() + dc);
						if (near == null || seen[index(near)] || !isFree(near))
							continue;
						seen[index(near)] = true;
						unwrappedOf.put(near, new Coord(at.row() + dr, at.col() + dc));
						todo.push(near);
					}
			}
			clusters.add(new Cluster(cells, unwrapped));
		}
		return clusters;
	}

	private int index(Coord c) { return c.row() * size + c.col(); }
}
