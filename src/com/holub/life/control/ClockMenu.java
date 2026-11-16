package com.holub.life.control;

import java.awt.event.*;
import javax.swing.*;
import com.holub.ui.MenuSite;

/** Installs the "Go" menu that controls a {@link Clock}. This code used
 *  to run inside the Clock constructor; it moved here so that a clock
 *  can be created (and tested) without a menu bar.
 *
 *  <p>The speed is still chosen by the first letter of the menu item's
 *  name, exactly as in the original. Replacing that is left to the
 *  clock owner (defect C1).
 */
public final class ClockMenu
{
	private ClockMenu() {}

	public static void install( final Clock clock )
	{
		// First set up a single listener that will handle all the
		// menu-selection events except "Exit"

		ActionListener modifier =                                   //{=startSetup}
			new ActionListener()
			{   public void actionPerformed(ActionEvent e)
				{
					String name = ((JMenuItem)e.getSource()).getName();
					char toDo = name.charAt(0);

					if( toDo=='T' )
						clock.tick();             // single tick
					else
						clock.startTicking(   toDo=='A' ? 500:    // agonizing
										toDo=='S' ? 150:      // slow
										toDo=='M' ? 70 :      // medium
										toDo=='F' ? 30 : 0 ); // fast
				}
			};
																	// {=midSetup}
		MenuSite.addLine(clock,"Go","Halt",             modifier);
		MenuSite.addLine(clock,"Go","Tick (Single Step)",modifier);
		MenuSite.addLine(clock,"Go","Agonizing",        modifier);
		MenuSite.addLine(clock,"Go","Slow",             modifier);
		MenuSite.addLine(clock,"Go","Medium",           modifier);
		MenuSite.addLine(clock,"Go","Fast",             modifier); // {=endSetup}
	}
}
