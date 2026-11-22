package com.holub.life.pattern;

import java.util.*;
import com.holub.life.model.*;

/** Finds catalog patterns by exact shape. Templates are tried largest
 *  first; for each, every unclaimed live cell is tried as the
 *  template's anchor (its first cell, top to bottom, left to right).
 *  A match needs every template cell live and unclaimed, and every
 *  other cell of the checked area dead.
 */
final class CatalogMatcher implements DetectionStage
{
	private final PatternCatalog catalog;

	CatalogMatcher(PatternCatalog catalog)
	{
		this.catalog = catalog;
	}

	@Override public boolean appliesTo(BoardView board)
	{
		return board.rule().equals(catalog.rule());
	}

	@Override public List<Detection> detect(CellClaims claims)
	{
		List<Detection> found = new ArrayList<>();
		for (Template t : catalog.templates())
			for (Coord start : claims.liveCells())
			{
				if (!claims.isFree(start))
					continue;
				int top  = start.row() - t.anchor().row();
				int left = start.col() - t.anchor().col();
				Set<Coord> cells = match(claims, t, top, left);
				if (cells == null)
					continue;
				cells.forEach(claims::claim);
				PatternDefinition d = t.definition;
				found.add(new Detection(d.name(), d.category(), Detection.Source.CATALOG,
										d.period(), top, left, t.height, t.width, cells));
			}
		return found;
	}

	/** @return the board cells of the match, or null if t is not at (top, left). */
	private static Set<Coord> match(CellClaims claims, Template t, int top, int left)
	{
		Set<Coord> cells = new HashSet<>();
		for (Coord c : t.cells)
		{
			Coord at = claims.locate(top + c.row(), left + c.col());
			if (at == null || !claims.isFree(at) || !cells.add(at))
				return null;        // off the board, dead, taken, or wraps onto itself
		}
		int ring = t.ringChecked ? 1 : 0;
		for (int r = -ring; r < t.height + ring; ++r)
			for (int c = -ring; c < t.width + ring; ++c)
				if (!t.contains(r, c) && claims.isAlive(top + r, left + c))
					return null;
		return cells;
	}
}
