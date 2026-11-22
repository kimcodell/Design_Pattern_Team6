package com.holub.life.pattern;

import java.util.*;
import java.util.function.UnaryOperator;
import com.holub.life.model.Coord;

/** An immutable set of live cells, with no position of its own
 *  (a value object). Most methods return a normalized shape, whose
 *  top-most row and left-most column are 0.
 */
public record Shape(Set<Coord> cells)
{
	private static final List<UnaryOperator<Coord>> SYMMETRIES = List.of(
		c -> new Coord( c.row(),  c.col()),     // as given
		c -> new Coord( c.col(), -c.row()),     // rotated 90 degrees
		c -> new Coord(-c.row(), -c.col()),     // rotated 180
		c -> new Coord(-c.col(),  c.row()),     // rotated 270
		c -> new Coord( c.row(), -c.col()),     // mirrored
		c -> new Coord(-c.col(), -c.row()),
		c -> new Coord(-c.row(),  c.col()),
		c -> new Coord( c.col(),  c.row()));

	/** Number of orientations {@link #oriented} accepts. */
	public static final int ORIENTATIONS = SYMMETRIES.size();

	public Shape
	{
		cells = Set.copyOf(cells);
	}

	/** Parse rows of text: 'O' is a live cell, anything else is dead.
	 *  For example {@code parse(".O.", "..O", "OOO")} is a glider.
	 */
	public static Shape parse(String... rows)
	{
		Set<Coord> cells = new HashSet<>();
		for (int row = 0; row < rows.length; ++row)
			for (int col = 0; col < rows[row].length(); ++col)
				if (rows[row].charAt(col) == 'O')
					cells.add(new Coord(row, col));
		return new Shape(cells).normalized();
	}

	public int size()       { return cells.size(); }
	public boolean isEmpty(){ return cells.isEmpty(); }
	public int top()        { return cells.stream().mapToInt(Coord::row).min().orElse(0); }
	public int left()       { return cells.stream().mapToInt(Coord::col).min().orElse(0); }
	public int height()     { return cells.isEmpty() ? 0 : cells.stream().mapToInt(Coord::row).max().getAsInt() - top() + 1; }
	public int width()      { return cells.isEmpty() ? 0 : cells.stream().mapToInt(Coord::col).max().getAsInt() - left() + 1; }

	/** The same cells moved so the top-left corner is (0, 0). */
	public Shape normalized()
	{
		return translate(-top(), -left());
	}

	public Shape translate(int rows, int cols)
	{
		Set<Coord> moved = new HashSet<>();
		for (Coord c : cells)
			moved.add(new Coord(c.row() + rows, c.col() + cols));
		return new Shape(moved);
	}

	/** One of the eight rotations and reflections, normalized.
	 *  @param orientation 0 (as given) to {@link #ORIENTATIONS} - 1.
	 */
	public Shape oriented(int orientation)
	{
		UnaryOperator<Coord> symmetry = SYMMETRIES.get(Math.floorMod(orientation, ORIENTATIONS));
		Set<Coord> turned = new HashSet<>();
		for (Coord c : cells)
			turned.add(symmetry.apply(c));
		return new Shape(turned).normalized();
	}

	/** The distinct rotations and reflections, normalized. A symmetric
	 *  shape has fewer than eight (a block has one).
	 */
	public List<Shape> orientations()
	{
		Set<Shape> distinct = new LinkedHashSet<>();
		for (int i = 0; i < ORIENTATIONS; ++i)
			distinct.add(oriented(i));
		return List.copyOf(distinct);
	}

	/** One cell turned by one of the eight symmetries, around (0, 0). */
	static Coord turn(Coord cell, int orientation)
	{
		return SYMMETRIES.get(Math.floorMod(orientation, ORIENTATIONS)).apply(cell);
	}

	/** Cells sorted top to bottom, then left to right. */
	List<Coord> sortedCells()
	{
		List<Coord> sorted = new ArrayList<>(cells);
		sorted.sort(Comparator.comparingInt(Coord::row).thenComparingInt(Coord::col));
		return sorted;
	}

	/** A text key that is equal for equal normalized shapes. */
	String key()
	{
		StringBuilder key = new StringBuilder();
		for (Coord c : normalized().sortedCells())
			key.append(c.row()).append(',').append(c.col()).append(';');
		return key.toString();
	}
}
