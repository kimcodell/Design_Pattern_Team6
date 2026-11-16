package com.holub.life.model;

import java.util.*;

/** An immutable set of cell changes: each cell maps to the state it
 *  should have. {@link LifeModel#apply} returns the inverse edit, so
 *  undo can be built outside the model.
 */
public record BoardEdit(Map<Coord, Boolean> cells)
{
	public static final BoardEdit EMPTY = new BoardEdit(Map.of());

	public BoardEdit
	{
		Objects.requireNonNull(cells, "cells");
		Map<Coord, Boolean> copy = new LinkedHashMap<>();
		for (Map.Entry<Coord, Boolean> entry : cells.entrySet())
			copy.put(Objects.requireNonNull(entry.getKey(), "cell"),
					 Objects.requireNonNull(entry.getValue(), "state"));
		cells = Collections.unmodifiableMap(copy);
	}

	/** Set every listed cell to the same state. */
	public static BoardEdit set(Collection<Coord> coords, boolean alive)
	{
		Map<Coord, Boolean> cells = new LinkedHashMap<>();
		for (Coord coord : coords)
			cells.put(coord, alive);
		return new BoardEdit(cells);
	}

	public static BoardEdit of(Coord coord, boolean alive)
	{
		return new BoardEdit(Map.of(coord, alive));
	}

	public boolean isEmpty()
	{
		return cells.isEmpty();
	}
}
