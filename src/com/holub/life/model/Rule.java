package com.holub.life.model;

import java.util.regex.*;

/** Decides whether a cell lives in the next generation (Strategy).
 *  The original hard-coded Conway's rule in Resident (P5).
 *
 *  <p>Rules are written in the usual B/S notation: "B3/S23" means a dead
 *  cell is born with exactly 3 live neighbors and a live cell survives
 *  with 2 or 3. Rules are immutable and shared by every cell of a board.
 */
public interface Rule
{
	/** @return true if the cell is alive in the next generation. */
	boolean nextState(boolean alive, int liveNeighbors);

	/** Conway's Game of Life. */
	Rule CONWAY = parse("B3/S23");

	/** Parse B/S notation, for example "B3/S23" or "B36/S23" (HighLife).
	 *  @throws IllegalArgumentException if the notation is malformed.
	 */
	static Rule parse(String notation)
	{
		Matcher m = Pattern.compile("B([0-8]*)/S([0-8]*)")
						   .matcher(notation.trim().toUpperCase());
		if (!m.matches())
			throw new IllegalArgumentException("not a B/S rule: \"" + notation + "\"");
		return new LifeLikeRule(m.group(1), m.group(2));
	}
}

/** A rule given by two sets of neighbor counts. */
final class LifeLikeRule implements Rule
{
	private final boolean[] birth   = new boolean[9];
	private final boolean[] survive = new boolean[9];

	LifeLikeRule(String birthDigits, String surviveDigits)
	{
		for (char c : birthDigits.toCharArray())   birth[c - '0']   = true;
		for (char c : surviveDigits.toCharArray()) survive[c - '0'] = true;
	}

	@Override public boolean nextState(boolean alive, int liveNeighbors)
	{
		return alive ? survive[liveNeighbors] : birth[liveNeighbors];
	}

	@Override public String toString()
	{
		StringBuilder b = new StringBuilder("B");
		for (int n = 0; n <= 8; ++n) if (birth[n]) b.append(n);
		b.append("/S");
		for (int n = 0; n <= 8; ++n) if (survive[n]) b.append(n);
		return b.toString();
	}

	@Override public boolean equals(Object other)
	{
		return other instanceof LifeLikeRule r
			   && java.util.Arrays.equals(birth, r.birth)
			   && java.util.Arrays.equals(survive, r.survive);
	}

	@Override public int hashCode()
	{
		return toString().hashCode();
	}
}
