package com.holub.life.model;

import java.util.Set;

/** Observer of a board. Each notification says why the board changed,
 *  so a listener can react precisely (for example, repaint only the
 *  cells that changed). Implement only the methods you need.
 */
public interface BoardListener
{
	/** The board advanced one generation.
	 *  @param generation the new generation number.
	 *  @param changed    the cells that changed state (read-only).
	 */
	default void stepped(long generation, Set<Coord> changed) {}

	/** Cells were edited directly (click, stamp, ...).
	 *  @param applied only the cells that actually changed.
	 */
	default void edited(BoardEdit applied) {}

	/** The whole board was replaced (clear or restore). */
	default void reset() {}
}
