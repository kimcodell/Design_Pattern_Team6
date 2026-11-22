package com.holub.life.model;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** The single entry point to the Life model (Facade). Everything
 *  outside this package uses the board only through this class or
 *  {@link BoardView}; the Composite of {@link Neighborhood} and
 *  {@link Resident} objects behind it is an implementation detail.
 *
 *  <p>The model knows nothing about Swing, menus, clocks or files.
 *  It is not thread-safe: use it from one thread only (in the
 *  application, the Swing event-dispatch thread).
 */
public final class LifeModel implements BoardView
{
	/** Width of the innermost block, in cells. */
	static final int BLOCK_SIZE = 8;

	private final int size;
	private final Rule rule;
	private final Boundary boundary;
	private final EvaluationCounter evaluations = new EvaluationCounter();
	private final Cell outermost;
	private long generation = 0;
	private final List<BoardListener> listeners = new CopyOnWriteArrayList<>();

	private LifeModel(Builder builder)
	{
		size = builder.size;
		rule = builder.rule;
		boundary = builder.boundary;
		outermost = new Neighborhood(size / BLOCK_SIZE,
						new Neighborhood(BLOCK_SIZE, new Resident(rule, evaluations)));
	}

	public static Builder builder()
	{
		return new Builder();
	}

	/** Creates a {@link LifeModel}. Settings that are not given use
	 *  their defaults: 64x64, Conway's rule, dead boundary.
	 */
	public static final class Builder
	{
		private int size = 64;
		private Rule rule = Rule.CONWAY;
		private Boundary boundary = Boundary.DEAD;

		private Builder() {}

		/** @param size cells per side; a multiple of {@value LifeModel#BLOCK_SIZE}. */
		public Builder size(int size)
		{
			if (size < BLOCK_SIZE || size % BLOCK_SIZE != 0)
				throw new IllegalArgumentException("board size must be a positive multiple of "
												   + BLOCK_SIZE + ": " + size);
			this.size = size;
			return this;
		}

		public Builder boundary(Boundary boundary)
		{
			this.boundary = Objects.requireNonNull(boundary, "boundary");
			return this;
		}

		public Builder rule(Rule rule)
		{
			this.rule = Objects.requireNonNull(rule, "rule");
			return this;
		}

		public LifeModel build()
		{
			return new LifeModel(this);
		}
	}

	/** An independent board with the same size, rule, boundary, cells
	 *  and generation (Prototype). Listeners are not copied. Use it to
	 *  branch a board, or to look ahead without disturbing this one.
	 */
	public LifeModel copy()
	{
		LifeModel twin = builder().size(size).rule(rule).boundary(boundary).build();
		twin.restore(snapshot());
		twin.generation = generation;
		return twin;
	}

	// ---- reading ----

	@Override public int size()
	{
		return size;
	}

	@Override public long generation()
	{
		return generation;
	}

	@Override public Rule rule()
	{
		return rule;
	}

	@Override public Boundary boundary()
	{
		return boundary;
	}

	@Override public boolean isAlive(int row, int col)
	{
		checkBounds(row, col);
		return outermost.isAlive(row, col);
	}

	@Override public BoardSnapshot snapshot()
	{
		Set<Coord> live = new HashSet<>();
		outermost.forEachLive(0, 0, live::add);
		return new BoardSnapshot(size, live);
	}

	// ---- changing ----

	/** Advance one generation. The two-phase update (figure the next
	 *  state, then transition to it) is done here, so callers cannot
	 *  get the order wrong.
	 *  @return true if any cell changed state.
	 */
	public boolean step()
	{
		Cell n = boundary.neighborOf(outermost);
		outermost.figureNextState(n, n, n, n, n, n, n, n);

		// Collect the changed cells only when someone will read them:
		// building the set is most of the cost of notification.
		boolean notify = !listeners.isEmpty();
		Set<Coord> changed = notify ? new HashSet<>() : Set.of();
		Consumer<Coord> sink = notify ? changed::add : coord -> {};
		boolean anyChanged = false;
		// Transition whenever there is something to do, including a
		// generation in which nothing changed but old edge markers
		// must be cleared (the original skipped it: N1).
		if (outermost.needsTransition())
			anyChanged = outermost.transition(0, 0, sink);
		++generation;

		if (notify)
		{
			Set<Coord> view = Collections.unmodifiableSet(changed);
			for (BoardListener listener : listeners)
				listener.stepped(generation, view);
		}
		return anyChanged;
	}

	/** Set several cells at once. Listeners get one notification with
	 *  the cells that actually changed.
	 *  @return the edit that undoes this one.
	 *  @throws IllegalArgumentException if a cell is off the board
	 *          (nothing is changed in that case).
	 */
	public BoardEdit apply(BoardEdit edit)
	{
		for (Coord coord : edit.cells().keySet())
			checkBounds(coord.row(), coord.col());

		Map<Coord, Boolean> applied = new LinkedHashMap<>();
		Map<Coord, Boolean> inverse = new LinkedHashMap<>();
		for (Map.Entry<Coord, Boolean> entry : edit.cells().entrySet())
		{
			Coord coord = entry.getKey();
			boolean alive = entry.getValue();
			if (outermost.set(coord.row(), coord.col(), alive))
			{
				applied.put(coord, alive);
				inverse.put(coord, !alive);
			}
		}

		if (!applied.isEmpty())
		{
			BoardEdit done = new BoardEdit(applied);
			for (BoardListener listener : listeners)
				listener.edited(done);
		}
		return new BoardEdit(inverse);
	}

	/** Flip one cell. A convenience for {@link #apply}. */
	public void toggle(int row, int col)
	{
		apply(BoardEdit.of(new Coord(row, col), !isAlive(row, col)));
	}

	/** Kill every cell and reset the generation to 0. */
	public void clear()
	{
		outermost.clear();
		generation = 0;
		for (BoardListener listener : listeners)
			listener.reset();
	}

	/** Replace the board with the given snapshot and reset the
	 *  generation to 0.
	 *  @throws IllegalArgumentException if the snapshot has a different
	 *          size or a cell off the board (the board is unchanged).
	 */
	public void restore(BoardSnapshot snapshot)
	{
		if (snapshot.size() != size)
			throw new IllegalArgumentException("snapshot size " + snapshot.size()
											   + " does not match board size " + size);
		for (Coord coord : snapshot.liveCells())
			checkBounds(coord.row(), coord.col());

		outermost.clear();
		for (Coord coord : snapshot.liveCells())
			outermost.set(coord.row(), coord.col(), true);
		generation = 0;
		for (BoardListener listener : listeners)
			listener.reset();
	}

	// ---- observing ----

	@Override public void addListener(BoardListener listener)
	{
		listeners.add(Objects.requireNonNull(listener));
	}

	@Override public void removeListener(BoardListener listener)
	{
		listeners.remove(listener);
	}

	/** Total number of cell evaluations so far (for tests). */
	long cellEvaluations()
	{
		return evaluations.count();
	}

	private void checkBounds(int row, int col)
	{
		if (row < 0 || col < 0 || row >= size || col >= size)
			throw new IllegalArgumentException("cell (" + row + ", " + col
											   + ") is off the " + size + "x" + size + " board");
	}
}
