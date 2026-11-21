package com.holub.life;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import com.holub.ui.MenuSite;
import com.holub.life.storage.BoardFileIO;
import com.holub.life.model.LifeModel;
import com.holub.life.storage.format.JavaSerializationFormat;
import com.holub.life.control.BoardFileActions;
import com.holub.life.control.Clock;
import com.holub.life.control.ClockMenu;
import com.holub.life.view.ColoringMenu;
import com.holub.life.view.ReadOnlyBoardView;
import com.holub.life.view.ScreenFit;
import com.holub.life.view.UniversePanel;
import com.holub.life.race.RaceWindow;

/*******************************************************************
 * An implemenation of Conway's Game of Life.
 *
 * <p>This class is the composition root: it creates the model, the
 * view, the clock and the file I/O, connects them, and installs the
 * menus. Nothing else creates these objects or looks them up through
 * a global (there are no Singletons).
 *
 * @include /etc/license.txt
 */

public final class Life extends JFrame
{
	private static final int CELL_SIZE = 8;
	private RaceWindow raceWindow;

	public static void main( String[] arguments )
	{   SwingUtilities.invokeLater( Life::new );
	}

	private void showRace()
	{
		if (raceWindow == null)
			raceWindow = new RaceWindow(this);
		raceWindow.setVisible(true);
		raceWindow.toFront();
	}

	private Life()
	{   super( "The Game of Life. "
					+"(c)2003 Allen I. Holub <http://www.holub.com>");

		JMenuBar bar = new JMenuBar();
		setJMenuBar(bar);
		MenuSite menus = new MenuSite(bar);

		LifeModel     model = LifeModel.builder().build();
		Dimension     room  = ScreenFit.roomOnScreen();
		int           cell  = ScreenFit.cellSize( model.size(), CELL_SIZE,
												  Math.min( room.width, room.height ) );
		UniversePanel panel = new UniversePanel( ReadOnlyBoardView.of( model ), cell );
		panel.setCellClickListener( model::toggle );

		BoardFileIO files = new BoardFileIO(
						List.of( new JavaSerializationFormat( model.size() ) ) );

		Clock clock = new Clock();				// ticks on the event-dispatch thread
		clock.addClockListener( () -> model.step() );

		BoardFileActions fileActions = new BoardFileActions( this, model, files, clock );
		installGridMenu( menus, model, fileActions );
		ClockMenu.install( menus, clock );
		ColoringMenu.install( menus, panel );
		menus.addLine( this, "Race", "Glider Race...", e -> showRace() );

		setDefaultCloseOperation    ( EXIT_ON_CLOSE         );
		getContentPane().setLayout  ( new BorderLayout()    );
		getContentPane().add( ScreenFit.fit( panel, cell, room ),
							  BorderLayout.CENTER ); //{=life.java.install}

		pack();
		setVisible( true );
	}

	private void installGridMenu( MenuSite menus, LifeModel model, BoardFileActions fileActions )
	{
		menus.addLine( this, "Grid", "Clear", e -> model.clear() );
		menus.addLine( this, "Grid", "Load", e -> fileActions.load() );
		menus.addLine( this, "Grid", "Store", e -> fileActions.store() );
		menus.addLine( this, "Grid", "Exit", e -> dispatchEvent(new java.awt.event.WindowEvent(this, java.awt.event.WindowEvent.WINDOW_CLOSING)) );
	}
}
