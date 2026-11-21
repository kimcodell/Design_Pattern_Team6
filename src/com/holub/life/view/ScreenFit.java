package com.holub.life.view;

import java.awt.*;
import javax.swing.*;

/** Fits a board view onto the screen. The original failed "miserably"
 *  when the board did not fit (Universe's constructor comment): the
 *  window grew past the screen and the cells beyond its edge could be
 *  neither seen nor clicked.
 *
 *  <p>Two steps fix that. First the cells shrink until the board fits
 *  ({@link #cellSize}), down to one pixel. A board too big even at one
 *  pixel per cell is then shown in a scroll pane no larger than the
 *  screen ({@link #fit}). The default 64&times;64 board with 8-pixel
 *  cells fits any ordinary screen, so it looks exactly as before.
 */
public final class ScreenFit
{
	/** Room left on the screen for the window's title bar, menu bar and
	 *  frame, which are not part of the board.
	 */
	static final int WINDOW_MARGIN_WIDTH  = 40;
	static final int WINDOW_MARGIN_HEIGHT = 100;

	private ScreenFit() {}

	/** The space the board can use: the screen without the task bar or
	 *  dock, minus the window's own decorations.
	 */
	public static Dimension roomOnScreen()
	{
		Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		return new Dimension(Math.max(1, screen.width  - WINDOW_MARGIN_WIDTH),
							 Math.max(1, screen.height - WINDOW_MARGIN_HEIGHT));
	}

	/** The requested cell size, shrunk if needed so that a square board of
	 *  <code>boardSize</code> cells fits into <code>available</code>
	 *  pixels, but never below one pixel.
	 */
	public static int cellSize(int boardSize, int requestedCellSize, int available)
	{
		if (boardSize <= 0 || requestedCellSize <= 0)
			throw new IllegalArgumentException("board size and cell size must be positive: "
											   + boardSize + ", " + requestedCellSize);
		return Math.max(1, Math.min(requestedCellSize, available / boardSize));
	}

	/** The component to put in the window. A board that fits into
	 *  <code>room</code> is the panel itself, so it behaves exactly as
	 *  before (the cells grow and shrink with the window). A bigger board
	 *  goes into a scroll pane the size of <code>room</code>, scrolling one
	 *  cell per arrow click.
	 */
	public static JComponent fit(UniversePanel panel, int cellSize, Dimension room)
	{
		Dimension board = panel.getPreferredSize();
		if (board.width <= room.width && board.height <= room.height)
			return panel;

		JScrollPane scroll = new JScrollPane(panel);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		scroll.getHorizontalScrollBar().setUnitIncrement(cellSize);
		scroll.getVerticalScrollBar().setUnitIncrement(cellSize);
		scroll.setPreferredSize(new Dimension(Math.min(board.width,  room.width),
											  Math.min(board.height, room.height)));
		return scroll;
	}
}
