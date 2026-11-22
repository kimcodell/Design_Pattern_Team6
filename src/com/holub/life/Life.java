package com.holub.life;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import com.holub.ui.MenuSite;
import com.holub.life.storage.BoardFileIO;
import com.holub.life.model.LifeModel;
import com.holub.life.storage.format.JavaSerializationFormat;
import com.holub.life.control.BoardFileActions;
import com.holub.life.control.StampToolbar;
import com.holub.life.control.Clock;
import com.holub.life.control.ClockMenu;
import com.holub.life.view.ColoringMenu;
import com.holub.life.view.PatternOverlay;
import com.holub.life.view.PatternPanel;
import com.holub.life.view.ReadOnlyBoardView;
import com.holub.life.view.ScreenFit;
import com.holub.life.view.UniversePanel;
import com.holub.life.race.RaceWindow;
import com.holub.life.pattern.PatternAnalyzer;
import com.holub.life.pattern.PatternCatalog;
import com.holub.life.pattern.PatternMonitor;
import com.holub.life.branch.BranchWindow;

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
	private static final int CELL_SIZE = 10;
	private RaceWindow raceWindow;
	private BranchWindow branchWindow;

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

		// Pattern analysis: outlines on the board and a list beside it.
		PatternMonitor  patterns = new PatternMonitor( model,
								new PatternAnalyzer( PatternCatalog.conway() ) );
		PatternOverlay  outlines = new PatternOverlay( patterns );
		PatternPanel    list     = new PatternPanel( patterns, outlines );

		// The board gets the screen room left beside the pattern list.
		Dimension     room  = ScreenFit.roomOnScreen();
		room = new Dimension( Math.max( 1, room.width - list.getPreferredSize().width ),
							  room.height );
		int           cell  = ScreenFit.cellSize( model.size(), CELL_SIZE,
												  Math.min( room.width, room.height ) );
		UniversePanel panel = new UniversePanel( ReadOnlyBoardView.of( model ), cell );
		StampToolbar tools = new StampToolbar(model, panel, patterns.analyzer().catalog());
		panel.addOverlay( outlines );

		BoardFileIO files = new BoardFileIO(
						List.of( new JavaSerializationFormat( model.size() ) ) );

		Clock clock = new Clock();				// ticks on the event-dispatch thread
		clock.addClockListener( () -> model.step() );

		BoardFileActions fileActions = new BoardFileActions( this, model, files, clock );
		installGridMenu( menus, model, fileActions );
		ClockMenu.install( menus, clock );
		ColoringMenu.install( menus, panel );
		menus.addLine( this, "Race", "Glider Race...", e -> showRace() );
		menus.addLine( this, "Bookmark", "북마크·분기 비교...", e -> showBranches(model) );

		setDefaultCloseOperation    ( EXIT_ON_CLOSE         );
		getContentPane().setLayout  ( new BorderLayout()    );
		getContentPane().add(tools, BorderLayout.NORTH);
		getContentPane().add( ScreenFit.fit( panel, cell, room ),
							  BorderLayout.CENTER ); //{=life.java.install}
		getContentPane().add( list, BorderLayout.EAST );

		pack();
		setVisible( true );
	}

	private void showBranches(LifeModel model)
	{
		if (branchWindow == null || !branchWindow.isDisplayable())
			branchWindow = new BranchWindow(this, model);
		branchWindow.setVisible(true);
		branchWindow.toFront();
	}

	private void installGridMenu( MenuSite menus, LifeModel model, BoardFileActions fileActions )
	{
		menus.addLine( this, "Grid", "Clear", e -> model.clear() );
		menus.addLine( this, "Grid", "Load", e -> fileActions.load() );
		menus.addLine( this, "Grid", "Store", e -> fileActions.store() );
		menus.addLine( this, "Grid", "Exit", e -> dispatchEvent(new java.awt.event.WindowEvent(this, java.awt.event.WindowEvent.WINDOW_CLOSING)) );
	}
}
