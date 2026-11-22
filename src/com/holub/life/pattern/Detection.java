package com.holub.life.pattern;

import java.util.*;
import com.holub.life.model.Coord;

/** One pattern found on a board.
 *
 *  @param name     the catalog name, or a description for a pattern
 *                  that is not in the catalog.
 *  @param category its group; null only when {@code source} is
 *                  {@link Source#UNCLASSIFIED}.
 *  @param period   generations until it repeats; 0 if unknown or never.
 *  @param top      row of the top edge of its box. With a TORUS
 *                  boundary the box may start off the board (negative)
 *                  or end past it, because it is not wrapped.
 *  @param left     column of the left edge of its box.
 *  @param cells    the board cells it is made of (always on the board).
 */
public record Detection(String name, PatternCategory category, Source source, int period,
						int top, int left, int height, int width, Set<Coord> cells)
{
	/** How the pattern was recognized. */
	public enum Source
	{
		/** Its shape matched a {@link PatternCatalog} entry. */
		CATALOG,
		/** Not in the catalog; its group was worked out by running it. */
		BEHAVIOR,
		/** Not recognized: debris that is still changing, or dying out. */
		UNCLASSIFIED
	}

	public Detection
	{
		Objects.requireNonNull(name, "name");
		Objects.requireNonNull(source, "source");
		if ((category == null) != (source == Source.UNCLASSIFIED))
			throw new IllegalArgumentException("only an unclassified detection has no category");
		cells = Set.copyOf(cells);
	}

	public boolean isClassified() { return source != Source.UNCLASSIFIED; }
}
