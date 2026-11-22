package com.holub.life.pattern;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import com.holub.life.model.*;

/** Keeps a {@link PatternReport} up to date as the board changes
 *  (Observer of the board, and subject for whoever shows the report).
 *
 *  <p>Analysis runs on the thread that changed the board (in the
 *  application, the event-dispatch thread). While inactive, for
 *  example when nothing shows the report, changes are only noted and
 *  the board is analyzed once when it becomes active again.
 */
public final class PatternMonitor implements BoardListener
{
	private final BoardView board;
	private final PatternAnalyzer analyzer;
	private final List<Consumer<PatternReport>> listeners = new CopyOnWriteArrayList<>();
	private PatternReport latest = PatternReport.EMPTY;
	private boolean active = true;
	private boolean stale  = true;

	public PatternMonitor(BoardView board, PatternAnalyzer analyzer)
	{
		this.board    = Objects.requireNonNull(board, "board");
		this.analyzer = Objects.requireNonNull(analyzer, "analyzer");
		board.addListener(this);
		refresh();
	}

	public PatternReport latest()        { return latest; }
	public PatternAnalyzer analyzer()    { return analyzer; }
	public BoardView board()             { return board; }
	public boolean isActive()            { return active; }

	public void setActive(boolean active)
	{
		this.active = active;
		if (active && stale)
			refresh();
	}

	/** The listener is called with every new report. */
	public void addReportListener(Consumer<PatternReport> listener)
	{
		listeners.add(Objects.requireNonNull(listener));
	}

	public void removeReportListener(Consumer<PatternReport> listener)
	{
		listeners.remove(listener);
	}

	@Override public void stepped(long generation, Set<Coord> changed) { refresh(); }
	@Override public void edited(BoardEdit applied)                    { refresh(); }
	@Override public void reset()                                      { refresh(); }

	private void refresh()
	{
		stale = true;
		if (!active)
			return;
		latest = analyzer.analyze(board);
		stale  = false;
		for (Consumer<PatternReport> listener : listeners)
			listener.accept(latest);
	}
}
