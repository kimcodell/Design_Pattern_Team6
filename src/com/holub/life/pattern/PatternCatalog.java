package com.holub.life.pattern;

import java.util.*;
import com.holub.life.model.*;

/** The known patterns for one rule, and the templates used to find
 *  them on a board. Building a catalog works out every phase of every
 *  pattern in every orientation once (with {@link SparseLife}); after
 *  that the catalog is immutable and can be shared.
 */
public final class PatternCatalog
{
	private final Rule rule;
	private final List<PatternDefinition> definitions;
	private final List<Template> templates;

	public PatternCatalog(Rule rule, List<PatternDefinition> definitions)
	{
		this.rule        = Objects.requireNonNull(rule, "rule");
		this.definitions = List.copyOf(definitions);
		this.templates   = buildTemplates();
	}

	/** The well-known patterns of Conway's B3/S23. */
	public static PatternCatalog conway()
	{
		return new PatternCatalog(Rule.CONWAY, List.of(
			def("Block",          PatternCategory.STILL_LIFE, 1, "period 1", "OO", "OO"),
			def("Beehive",        PatternCategory.STILL_LIFE, 1, "period 1", ".OO.", "O..O", ".OO."),
			def("Loaf",           PatternCategory.STILL_LIFE, 1, "period 1", ".OO.", "O..O", ".O.O", "..O."),
			def("Boat",           PatternCategory.STILL_LIFE, 1, "period 1", "OO.", "O.O", ".O."),
			def("Tub",            PatternCategory.STILL_LIFE, 1, "period 1", ".O.", "O.O", ".O."),
			def("Blinker",        PatternCategory.OSCILLATOR, 2, "period 2", "OOO"),
			def("Toad",           PatternCategory.OSCILLATOR, 2, "period 2", ".OOO", "OOO."),
			def("Beacon",         PatternCategory.OSCILLATOR, 2, "period 2", "OO..", "OO..", "..OO", "..OO"),
			def("Pulsar",         PatternCategory.OSCILLATOR, 3, "period 3",
				"..OOO...OOO..", ".............", "O....O.O....O", "O....O.O....O",
				"O....O.O....O", "..OOO...OOO..", ".............", "..OOO...OOO..",
				"O....O.O....O", "O....O.O....O", "O....O.O....O", ".............",
				"..OOO...OOO.."),
			def("Pentadecathlon", PatternCategory.OSCILLATOR, 15, "period 15", "..O....O..", "OO.OOOO.OO", "..O....O.."),
			def("Glider",         PatternCategory.SPACESHIP, 4, "period 4 · c/4 diagonal", ".O.", "..O", "OOO"),
			def("LWSS",           PatternCategory.SPACESHIP, 4, "period 4 · c/2 straight", ".O..O", "O....", "O...O", "OOOO."),
			def("MWSS",           PatternCategory.SPACESHIP, 4, "period 4 · c/2 straight", "...O..", ".O...O", "O.....", "O....O", "OOOOO."),
			def("HWSS",           PatternCategory.SPACESHIP, 4, "period 4 · c/2 straight", "...OO..", ".O....O", "O......", "O.....O", "OOOOOO."),
			def("R-pentomino",    PatternCategory.METHUSELAH, 0, "settles after 1,103 gens", ".OO", "OO.", ".O."),
			def("Acorn",          PatternCategory.METHUSELAH, 0, "settles after 5,206 gens", ".O.....", "...O...", "OO..OOO"),
			def("Diehard",        PatternCategory.METHUSELAH, 0, "dies after 130 gens", "......O.", "OO......", ".O...OOO"),
			def("Gosper glider gun", PatternCategory.GENERATOR, 30, "1 glider every 30 gens",
				"........................O...........",
				"......................O.O...........",
				"............OO......OO............OO",
				"...........O...O....OO............OO",
				"OO........O.....O...OO..............",
				"OO........O...O.OO....O.O...........",
				"..........O.....O.......O...........",
				"...........O...O....................",
				"............OO......................")));
	}

	private static PatternDefinition def(String name, PatternCategory category, int period,
										 String note, String... rows)
	{
		return new PatternDefinition(name, category, Shape.parse(rows), period, note);
	}

	public Rule rule()                           { return rule; }
	public List<PatternDefinition> definitions() { return definitions; }

	/** Definitions grouped by category, in category order. Every
	 *  category is present, possibly with an empty list.
	 */
	public Map<PatternCategory, List<PatternDefinition>> byCategory()
	{
		Map<PatternCategory, List<PatternDefinition>> groups = new EnumMap<>(PatternCategory.class);
		for (PatternCategory category : PatternCategory.values())
			groups.put(category, new ArrayList<>());
		for (PatternDefinition d : definitions)
			groups.get(d.category()).add(d);
		groups.replaceAll((category, list) -> List.copyOf(list));
		return Collections.unmodifiableMap(groups);
	}

	/** Every template, largest first, so that a big pattern is found
	 *  before a small one inside it could be.
	 */
	List<Template> templates()                   { return templates; }

	// ---- building ----

	private List<Template> buildTemplates()
	{
		List<Template> all = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		for (PatternDefinition d : definitions)
		{
			if (d.category() == PatternCategory.GENERATOR)
				addWindowTemplates(d, all, seen);
			else
				addTemplates(d, all, seen);
		}
		all.sort(Comparator.comparingInt((Template t) -> t.cells.size()).reversed());
		return List.copyOf(all);
	}

	/** Each phase in each orientation, with a dead ring around it. A
	 *  methuselah has only its seed: once it starts changing it is no
	 *  longer recognizable.
	 */
	private void addTemplates(PatternDefinition d, List<Template> out, Set<String> seen)
	{
		Set<Coord> phase = d.seed().cells();
		int phases = Math.max(1, d.period());
		for (int p = 0; p < phases; ++p)
		{
			for (int o = 0; o < Shape.ORIENTATIONS; ++o)
			{
				Shape look = new Shape(phase).oriented(o);
				if (seen.add(look.key()))
					out.add(new Template(d, p, look, look.height(), look.width(), true));
			}
			phase = SparseLife.step(phase, rule);
		}
	}

	/** A generator grows without limit, so its templates are what can
	 *  be seen through a window the size of its seed box, over one
	 *  period once it is running steadily. Gliders it already sent away
	 *  are outside the window and are found as spaceships on their own.
	 */
	private void addWindowTemplates(PatternDefinition d, List<Template> out, Set<String> seen)
	{
		int h = d.seed().height(), w = d.seed().width();
		Set<Coord> state = d.seed().cells();
		for (int g = 0; g < 2 * d.period(); ++g)
		{
			if (g >= d.period())
			{
				Set<Coord> window = new HashSet<>();
				for (Coord c : state)
					if (c.row() >= 0 && c.col() >= 0 && c.row() < h && c.col() < w)
						window.add(c);
				for (int o = 0; o < Shape.ORIENTATIONS; ++o)
				{
					Shape turned = orientInBox(window, h, w, o);
					int th = (o % 2 == 0) ? h : w, tw = (o % 2 == 0) ? w : h;
					if (seen.add("window" + o + ":" + turned.key() + "@" + turned.top() + "," + turned.left()))
						out.add(new Template(d, g - d.period(), turned, th, tw, false));
				}
			}
			state = SparseLife.step(state, rule);
		}
	}

	/** Turn cells that lie in an h-by-w box so the turned box's top-left
	 *  corner is (0, 0). Unlike {@link Shape#oriented}, the cells keep
	 *  their place inside the box (a window may have empty edges).
	 */
	private static Shape orientInBox(Set<Coord> cells, int h, int w, int orientation)
	{
		Coord a = Shape.turn(new Coord(0, 0), orientation);
		Coord b = Shape.turn(new Coord(h - 1, w - 1), orientation);
		int top = Math.min(a.row(), b.row()), left = Math.min(a.col(), b.col());
		Set<Coord> turned = new HashSet<>();
		for (Coord c : cells)
		{	Coord t = Shape.turn(c, orientation);
			turned.add(new Coord(t.row() - top, t.col() - left));
		}
		return new Shape(turned);
	}
}
