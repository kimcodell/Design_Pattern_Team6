package com.holub.life.view;

import java.awt.Color;
import com.holub.ui.Colors;

/** The original look: every live cell red, dead cells light yellow. */
public final class ClassicColoring implements CellColoring
{
	static final Color LIVE = Color.RED;
	static final Color DEAD = Colors.LIGHT_YELLOW;

	@Override public Color dead()
	{
		return DEAD;
	}

	@Override public Color live(long age)
	{
		return LIVE;
	}
}
