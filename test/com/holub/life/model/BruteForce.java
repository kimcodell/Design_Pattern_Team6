package com.holub.life.model;

import java.util.*;

/** Reference implementation of Life used to check the optimized board.
 *  Cells are encoded as row * size + column.
 */
public final class BruteForce
{
	private BruteForce() {}

	public static Set<Integer> step(Set<Integer> live, int size, boolean torus,
									Set<Integer> birth, Set<Integer> survive)
	{
		Set<Integer> next = new HashSet<>();
		for (int r = 0; r < size; ++r)
			for (int c = 0; c < size; ++c)
			{
				int n = 0;
				for (int dr = -1; dr <= 1; ++dr)
					for (int dc = -1; dc <= 1; ++dc)
					{
						if (dr == 0 && dc == 0) continue;
						int rr = r + dr, cc = c + dc;
						if (torus)
						{
							rr = (rr + size) % size;
							cc = (cc + size) % size;
						}
						else if (rr < 0 || cc < 0 || rr >= size || cc >= size)
							continue;
						if (live.contains(rr * size + cc)) ++n;
					}
				boolean alive = live.contains(r * size + c);
				if (alive ? survive.contains(n) : birth.contains(n))
					next.add(r * size + c);
			}
		return next;
	}

	public static Set<Integer> conway(Set<Integer> live, int size, boolean torus)
	{
		return step(live, size, torus, Set.of(3), Set.of(2, 3));
	}

	public static Set<Integer> random(int size, double density, long seed)
	{
		Random random = new Random(seed);
		Set<Integer> live = new HashSet<>();
		for (int i = 0; i < size * size; ++i)
			if (random.nextDouble() < density) live.add(i);
		return live;
	}

	/** Encode a pattern given as (row, column) pairs, shifted by an offset. */
	public static Set<Integer> pattern(int size, int rowOffset, int colOffset, int[][] cells)
	{
		Set<Integer> live = new HashSet<>();
		for (int[] cell : cells)
			live.add((cell[0] + rowOffset) * size + (cell[1] + colOffset));
		return live;
	}

	public static final int[][] GLIDER  = { {0, 1}, {1, 2}, {2, 0}, {2, 1}, {2, 2} };
	public static final int[][] BLINKER = { {0, 0}, {0, 1}, {0, 2} };
}
