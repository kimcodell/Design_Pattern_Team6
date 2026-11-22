package com.holub.life.pattern;

import java.util.*;
import com.holub.life.model.*;

/** Helpers for the pattern tests. */
final class Fixtures
{
	private Fixtures() {}

	static final PatternCatalog CONWAY = PatternCatalog.conway();

	static PatternDefinition def(String name)
	{
		return CONWAY.definitions().stream().filter(d -> d.name().equals(name)).findFirst()
					 .orElseThrow(() -> new IllegalArgumentException(name));
	}

	/** Put a shape on the board with its top-left corner at (row, col). */
	static void put(LifeModel model, Shape shape, int row, int col)
	{
		Set<Coord> cells = new HashSet<>();
		for (Coord c : shape.translate(row, col).cells())
			model.boundary().locate(c.row(), c.col(), model.size()).ifPresent(cells::add);
		model.apply(BoardEdit.set(cells, true));
	}

	static void put(LifeModel model, String name, int row, int col)
	{
		put(model, def(name).seed(), row, col);
	}

	static Map<String, Integer> names(PatternReport report)
	{
		Map<String, Integer> names = new TreeMap<>();
		for (Detection d : report.detections()) names.merge(d.name(), 1, Integer::sum);
		return names;
	}
}
