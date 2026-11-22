package com.holub.life.view;

import com.holub.ui.MenuSite;

/** The "Color" menu: switches a panel between the original look and
 *  coloring cells by age. The panel starts with the original look.
 */
public final class ColoringMenu
{
	private ColoringMenu() {}

	/** Adds the menu to the menu bar of the window that shows <code>panel</code>. */
	public static void install(MenuSite menus, UniversePanel panel)
	{
		menus.addLine(panel, "Color", "Classic", e -> panel.setColoring(new ClassicColoring()));
		menus.addLine(panel, "Color", "By age",  e -> panel.setColoring(new AgeColoring()));
	}
}
