package com.holub.life.model;

import java.util.*;

/** Conversions between LifeModel and the brute-force encoding. */
final class Boards
{
	private Boards() {}

	static void load(LifeModel model, Set<Integer> live)
	{
		int n = model.size();
		Set<Coord> cells = new HashSet<>();
		for (int i : live) cells.add(new Coord(i / n, i % n));
		model.restore(new BoardSnapshot(n, cells));
	}

	static Set<Integer> read(BoardView board)
	{
		int n = board.size();
		Set<Integer> live = new HashSet<>();
		for (Coord c : board.snapshot().liveCells()) live.add(c.row() * n + c.col());
		return live;
	}
}
