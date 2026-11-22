package com.holub.life.model;

import java.util.Optional;

/** What lies beyond the edge of the board (Strategy). The original
 *  always used dead cells (P6).
 *
 *  <p>This is an enum rather than a public interface because choosing
 *  the neighbor means handing out a {@link Cell}, which is hidden inside
 *  this package. Callers outside the package can pick a boundary but
 *  cannot implement one.
 *
 *  <p>Code outside the package asks about the boundary in board
 *  coordinates through {@link #locate}; the Cell-level method stays
 *  package-private, so a Cell never appears in a public signature.
 */
public enum Boundary
{
	/** Everything outside the board is a permanently dead cell
	 *  (the {@link Cell#DUMMY} Null Object). The original behavior.
	 */
	DEAD
	{
		@Override Cell neighborOf(Cell outermost)
		{
			return Cell.DUMMY;
		}

		@Override public Optional<Coord> locate(int row, int col, int size)
		{
			return row < 0 || col < 0 || row >= size || col >= size
				   ? Optional.empty()
				   : Optional.of(new Coord(row, col));
		}
	},

	/** Opposite edges are joined (a doughnut). The outermost block is
	 *  its own neighbor in all eight directions, so a cell on the top
	 *  edge sees the bottom row, and a corner sees the opposite corner.
	 */
	TORUS
	{
		@Override Cell neighborOf(Cell outermost)
		{
			return outermost;
		}

		@Override public Optional<Coord> locate(int row, int col, int size)
		{
			return Optional.of(new Coord(Math.floorMod(row, size), Math.floorMod(col, size)));
		}
	};

	/** The board cell that position (row, col) refers to on a board of
	 *  the given size. The position may be off the board.
	 *  @return the cell, or empty if the position is outside the board
	 *          and so always dead.
	 */
	public abstract Optional<Coord> locate(int row, int col, int size);

	abstract Cell neighborOf(Cell outermost);
}
