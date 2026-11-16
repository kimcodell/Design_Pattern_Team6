package com.holub.life.storage.format;

import java.awt.Point;
import java.io.*;
import java.util.*;
import com.holub.life.model.BoardSnapshot;
import com.holub.life.model.Coord;

/** The original Holub ".life" format: a Java-serialized collection of
 *  java.awt.Point (x = column, y = row) holding the live cells.
 *
 *  <p>Files written by the original program can be read back:
 *  <ul>
 *  <li>The original wrote every live cell twice; duplicates are ignored.</li>
 *  <li>The format has no board size, so the size given to the
 *      constructor is used (the original board was always 64x64).</li>
 *  <li>Only LinkedList/collection classes and Point may appear in the
 *      stream; anything else is rejected before it is deserialized.</li>
 *  </ul>
 */
public final class JavaSerializationFormat implements BoardSerializer
{
	private static final ObjectInputFilter ALLOWED = ObjectInputFilter.Config.createFilter(
					"java.util.LinkedList;java.util.ArrayList;java.util.HashSet;"
				  + "java.awt.Point;maxdepth=4;!*");

	private final int boardSize;

	/** @param boardSize size to assume for files, which do not record it. */
	public JavaSerializationFormat(int boardSize)
	{
		if (boardSize <= 0)
			throw new IllegalArgumentException("board size must be positive: " + boardSize);
		this.boardSize = boardSize;
	}

	@Override public void write(BoardSnapshot snapshot, OutputStream out) throws IOException
	{
		LinkedList<Point> liveCells = new LinkedList<>();
		for (Coord coord : snapshot.liveCells())
			liveCells.add(new Point(coord.col(), coord.row()));

		ObjectOutputStream sink = new ObjectOutputStream(out);
		sink.writeObject(liveCells);
		sink.flush();
	}

	@Override public BoardSnapshot read(InputStream in) throws IOException
	{
		ObjectInputStream source = new ObjectInputStream(in);
		source.setObjectInputFilter(ALLOWED);

		Object contents;
		try
		{
			contents = source.readObject();
		}
		catch (ClassNotFoundException | InvalidClassException e)
		{
			throw new IOException("Not a Life file: " + e.getMessage(), e);
		}

		if (!(contents instanceof Collection<?> cells))
			throw new IOException("Not a Life file: expected a list of cells");

		Set<Coord> live = new HashSet<>();
		for (Object cell : cells)
		{
			if (!(cell instanceof Point point))
				throw new IOException("Not a Life file: unexpected entry " + cell);
			live.add(new Coord(point.y, point.x));
		}
		return new BoardSnapshot(boardSize, live);
	}

	@Override public String extension()
	{
		return ".life";
	}
}
