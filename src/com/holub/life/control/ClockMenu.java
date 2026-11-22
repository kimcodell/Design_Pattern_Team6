package com.holub.life.control;

import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
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

	public static void install( MenuSite menus, final Clock clock )
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
						clock.stepOnce();
					else
						clock.startTicking(   toDo=='A' ? 500:    // agonizing
										toDo=='S' ? 150:      // slow
										toDo=='M' ? 70 :      // medium
										toDo=='F' ? 30 : 0 ); // fast
				}
			};
																	// {=midSetup}
		menus.addLine(clock,"Go","Status",           e -> {});
		menus.addLine(clock,"Go","Halt",             modifier);
		menus.addLine(clock,"Go","Tick (Single Step)",modifier);
		menus.addLine(clock,"Go","Agonizing",        modifier);
		menus.addLine(clock,"Go","Slow",             modifier);
		menus.addLine(clock,"Go","Medium",           modifier);
		menus.addLine(clock,"Go","Fast",             modifier); // {=endSetup}

		JMenu goMenu = (JMenu)menus.getMyMenuItem(clock, "Go", null);
		JMenuItem statusItem = menus.getMyMenuItem(clock, "Go", "Status");
		showStatusOnOpen(goMenu, statusItem, clock);
	}

	static void showStatusOnOpen(JMenu goMenu, JMenuItem statusItem, Clock clock)
	{
		statusItem.setEnabled(false);
		goMenu.addMenuListener(new MenuListener()
		{
			@Override public void menuSelected(MenuEvent e)
			{
				if (clock.isRunning())
					statusItem.setText("Status: Running (" + clock.getTickDelay() + " ms/generation)");
				else
					statusItem.setText("Status: Stopped");
			}

			@Override public void menuDeselected(MenuEvent e) {}
			@Override public void menuCanceled(MenuEvent e) {}
		});
	}
}
