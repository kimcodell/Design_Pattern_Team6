package com.holub.life.model;

/** What lies beyond the edge of the board (Strategy). The original
 *  always used dead cells (P6).
 *
 *  <p>This is an enum rather than a public interface because choosing
 *  the neighbor means handing out a {@link Cell}, which is hidden inside
 *  this package. Callers outside the package can pick a boundary but
 *  cannot implement one.
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
	};

	abstract Cell neighborOf(Cell outermost);
}
