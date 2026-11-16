package com.holub.life.model;

/** Read-only access to a board. Code that only reads the board
 *  (renderers, analyzers) should depend on this rather than on
 *  {@link LifeModel}, so it cannot change the board by accident and
 *  can also be given other boards (a stored snapshot, for example).
 */
public interface BoardView
{
	/** Width (and height) of the board in cells. */
	int size();

	/** Number of generations since the board was created, cleared or restored. */
	long generation();

	boolean isAlive(int row, int col);

	BoardSnapshot snapshot();

	void addListener(BoardListener listener);

	void removeListener(BoardListener listener);
}
