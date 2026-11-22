package com.holub.life.branch;

import java.util.Objects;
import com.holub.life.model.BoardView;
import com.holub.life.model.LifeModel;

/** Coordinates two branches. Use on the EDT in the application. */
public final class BranchSession
{
	public enum Side { A, B }

	private LifeModel left;
	private LifeModel right;
	private String origin;

	public BranchSession(Bookmark bookmark)
	{
		compare(bookmark);
	}

	/** Replace both branches, preserving the bookmark's generation and settings. */
	public void compare(Bookmark bookmark)
	{
		Objects.requireNonNull(bookmark, "bookmark");
		LifeModel newLeft = bookmark.createBranch();
		LifeModel newRight = bookmark.createBranch();
		left = newLeft;
		right = newRight;
		origin = bookmark.name();
	}

	public String origin()
	{
		return origin;
	}

	public BoardView board(Side side)
	{
		return model(side);
	}

	public void toggle(Side side, int row, int col)
	{
		model(side).toggle(row, col);
	}

	public Bookmark bookmark(String name, Side side)
	{
		return new Bookmark(name, model(side));
	}

	/** Advance each board exactly once, including stable and empty boards. */
	public void stepBoth()
	{
		left.step();
		right.step();
	}

	private LifeModel model(Side side)
	{
		return Objects.requireNonNull(side, "side") == Side.A ? left : right;
	}
}
