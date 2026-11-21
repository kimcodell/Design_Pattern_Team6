package com.holub.life.view;

import java.util.Objects;
import com.holub.life.model.*;

/** A read-only stand-in for a board (Protection Proxy). It forwards every
 *  {@link BoardView} call to the real board, but it is not the real board:
 *  code that is handed this object cannot cast it back to
 *  {@link LifeModel} and change the board.
 *
 *  <p>{@link LifeModel} implements {@link BoardView}, so passing the model
 *  itself to a view only promises read-only use; a cast breaks the
 *  promise. Wrapping it makes the promise hold. Everything the proxy
 *  returns is already immutable ({@link BoardSnapshot}, the cell sets and
 *  {@link BoardEdit}s in notifications), so nothing leaks a way to write.
 *
 *  <p>Listeners are forwarded unchanged, so a listener added through the
 *  proxy can be removed through the proxy or the real board.
 */
public final class ReadOnlyBoardView implements BoardView
{
	private final BoardView board;

	private ReadOnlyBoardView(BoardView board)
	{
		this.board = board;
	}

	/** Wrap a board. A board that is already wrapped is returned as is. */
	public static BoardView of(BoardView board)
	{
		Objects.requireNonNull(board, "board");
		return board instanceof ReadOnlyBoardView ? board : new ReadOnlyBoardView(board);
	}

	@Override public int size()
	{
		return board.size();
	}

	@Override public long generation()
	{
		return board.generation();
	}

	@Override public boolean isAlive(int row, int col)
	{
		return board.isAlive(row, col);
	}

	@Override public BoardSnapshot snapshot()
	{
		return board.snapshot();
	}

	@Override public void addListener(BoardListener listener)
	{
		board.addListener(listener);
	}

	@Override public void removeListener(BoardListener listener)
	{
		board.removeListener(listener);
	}
}
