package com.holub.life.view;

import java.awt.Color;

/** Live cells darken the longer they stay alive: a newborn cell is pale
 *  pink, it turns red at age 4 and is almost black from age 15 on. Still
 *  lifes and long-lived parts of a pattern turn dark, while moving
 *  patterns stay light: no cell of a glider lives longer than 4
 *  generations, because it moves by cells dying at its back and being
 *  born at its front.
 *
 *  <p>The color changes in eight steps, closer together at young ages so
 *  that cells 1 to 4 generations old can be told apart. Changing in steps
 *  rather than every generation means only the cells that reach a step
 *  are redrawn, which keeps the panel's partial redraw cheap.
 */
public final class AgeColoring implements CellColoring
{
	/** Shade for ages 1, 2, 3, 4&ndash;5, 6&ndash;7, 8&ndash;10,
	 *  11&ndash;14 and 15 or more.
	 */
	static final Color[] SHADES =
	{	new Color(0xFFB0B0),        // pale pink
		new Color(0xFF8C8C),
		new Color(0xFF6060),
		new Color(0xFF3030),        // red
		new Color(0xE00000),
		new Color(0xB00000),
		new Color(0x780000),
		new Color(0x300000),        // almost black
	};

	/** Ages at which the next shade starts (one fewer than the shades). */
	private static final long[] STEPS = { 2, 3, 4, 6, 8, 11, 15 };

	@Override public Color dead()
	{
		return ClassicColoring.DEAD;
	}

	@Override public Color live(long age)
	{
		int shade = 0;
		while (shade < STEPS.length && age >= STEPS[shade])
			++shade;
		return SHADES[shade];
	}

	@Override public long[] colorChangesAt()
	{
		return STEPS.clone();
	}
}
