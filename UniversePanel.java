package com.holub.life.view;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.swing.*;
import com.holub.ui.Colors;
import com.holub.life.model.*;

/** View of a board. Draws whatever {@link BoardView} it is given and
 *  reports clicks as (row, column); it never changes the board itself.
 *
 *  <p>Like the original <code>Neighborhood.redraw</code>, it redraws only
 *  what changed. The board is kept in an image; each notification
 *  repaints the changed cells into it and copies only that part to the
 *  screen. A block (8&times;8 cells) in which something changed in the
 *  last generation, or that was just edited, gets a blue border, as in
 *  the original. A block that goes quiet is redrawn once to remove the
 *  border (the original's <code>oneLastRefreshRequired</code>). The whole
 *  board is drawn only when the panel is first shown, when the cell
 *  size changes, and after {@link #reset()}.
 */
public class UniversePanel extends JPanel implements BoardListener, Scrollable
{
	/** Receives clicks translated to board cells. */
	public interface CellClickListener
	{
		void cellClicked(int row, int col);
	}

	private static final Color BORDER_COLOR = Colors.DARK_YELLOW;
	private static final Color LIVE_COLOR   = Color.RED;
	private static final Color DEAD_COLOR   = Colors.LIGHT_YELLOW;
	private static final Color BLOCK_COLOR  = Colors.LIGHT_ORANGE;
	private static final Color ACTIVE_COLOR = Color.BLUE;
	private static final int   BLOCK_SIZE   = 8;
	private static final int   MAX_VIEWPORT_SIZE = 640;

	private final BoardView board;
	private final int       blocksPerSide;
	private CellClickListener clickListener = (row, col) -> {};

	/** The board as last drawn, or null when it must be drawn in full. */
	private BufferedImage canvas;
	/** Cell size in pixels that {@link #canvas} was drawn with. */
	private int canvasPixels;

	/** Blocks (row * blocksPerSide + col) drawn with a blue border. */
	private Set<Integer> activeBlocks = new HashSet<>();
	/** Whether the whole board has a blue border (something changed). */
	private boolean boardActive = false;

	public UniversePanel(BoardView board, int cellSize)
	{
		this.board = board;
		this.blocksPerSide = (board.size() + BLOCK_SIZE - 1) / BLOCK_SIZE;
		Dimension preferred = new Dimension(board.size() * cellSize, board.size() * cellSize);
		setPreferredSize(preferred);
		setBackground(Color.white);
		setOpaque(true);

		addMouseListener(new MouseAdapter()
		{
			@Override public void mousePressed(MouseEvent e)
			{
				int pixels = cellPixels();
				int row = e.getY() / pixels, col = e.getX() / pixels;
				if (row < board.size() && col < board.size())
					clickListener.cellClicked(row, col);
			}
		});
		board.addListener(this);
	}

	public void setCellClickListener(CellClickListener listener)
	{
		clickListener = listener;
	}

	/** Keep large boards at a useful cell size and let a scroll pane expose
	 *  the rest, instead of shrinking the cells to one pixel or packing a
	 *  window larger than the screen.
	 */
	@Override public Dimension getPreferredScrollableViewportSize()
	{
		Dimension preferred = getPreferredSize();
		return new Dimension(Math.min(preferred.width, MAX_VIEWPORT_SIZE),
							 Math.min(preferred.height, MAX_VIEWPORT_SIZE));
	}

	@Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
	{
		return Math.max(1, canvasPixels == 0 ? 8 : canvasPixels);
	}

	@Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
	{
		int extent = orientation == SwingConstants.HORIZONTAL ? visibleRect.width : visibleRect.height;
		return Math.max(1, extent - getScrollableUnitIncrement(visibleRect, orientation, direction));
	}

	@Override public boolean getScrollableTracksViewportWidth() { return false; }
	@Override public boolean getScrollableTracksViewportHeight() { return false; }

	/** Pixel size of one cell: the largest whole number that fits. */
	private int cellPixels()
	{
		return Math.max(1, Math.min(getWidth(), getHeight()) / board.size());
	}

	@Override protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		if (canvas == null || canvasPixels != cellPixels())
			drawWholeBoard();
		g.drawImage(canvas, 0, 0, null);
	}

	// ---- BoardListener ----

	/** Cells that changed are redrawn. Blocks with a change get a blue
	 *  border; blocks that had one and are now quiet are redrawn without it.
	 */
	@Override public void stepped(long generation, Set<Coord> changed)
	{
		update(changed, blocksOf(changed), !changed.isEmpty());
	}

	/** An edit marks its blocks active, as a click did in the original. */
	@Override public void edited(BoardEdit applied)
	{
		Set<Integer> active = new HashSet<>(activeBlocks);
		active.addAll(blocksOf(applied.cells().keySet()));
		update(applied.cells().keySet(), active, true);
	}

	@Override public void reset()
	{
		activeBlocks = new HashSet<>();
		boardActive  = false;
		canvas       = null;            // draw everything on the next paint
		repaint();
	}

	// ---- drawing ----

	private void update(Collection<Coord> cells, Set<Integer> nowActive, boolean nowBoardActive)
	{
		Set<Integer> quieted = new HashSet<>(activeBlocks);
		quieted.removeAll(nowActive);
		boolean boardTurnedOn  = nowBoardActive && !boardActive;
		boolean boardTurnedOff = boardActive && !nowBoardActive;
		if (boardTurnedOff)
			quieted.addAll(perimeterBlocks());

		activeBlocks = nowActive;
		boardActive  = nowBoardActive;

		if (canvas == null || canvasPixels != cellPixels())
		{	repaint();                  // the next paint draws everything
			return;
		}

		Graphics2D g = canvas.createGraphics();
		try
		{
			Rectangle dirty = null;
			if (!cells.isEmpty())
			{	int top = Integer.MAX_VALUE, left = Integer.MAX_VALUE, bottom = 0, right = 0;
				for (Coord cell : cells)
				{	paintCell(g, cell.row(), cell.col());
					top    = Math.min(top,    cell.row());
					left   = Math.min(left,   cell.col());
					bottom = Math.max(bottom, cell.row());
					right  = Math.max(right,  cell.col());
				}
				dirty = new Rectangle(left * canvasPixels, top * canvasPixels,
									  (right - left + 1) * canvasPixels,
									  (bottom - top + 1) * canvasPixels);
			}
			for (int block : quieted)
			{	paintBlock(g, block);
				dirty = union(dirty, blockBounds(block));
			}
			for (int block : activeBlocks)  // cell fills may have covered the border
			{	paintActiveBorder(g, block);
				dirty = union(dirty, blockBounds(block));
			}
			if (boardTurnedOn)
				dirty = union(dirty, new Rectangle(0, 0, canvas.getWidth(), canvas.getHeight()));
			if (dirty == null)
				return;
			if (boardActive)
			{	g.setClip(dirty);          // repair the board border where we drew
				paintBoardBorder(g);
			}
			repaint(dirty);
		}
		finally
		{	g.dispose();
		}
	}

	private void drawWholeBoard()
	{
		canvasPixels = cellPixels();
		int side = board.size() * canvasPixels + 1;     // +1 for the closing line
		canvas = new BufferedImage(side, side, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = canvas.createGraphics();
		try
		{
			g.setColor(getBackground());
			g.fillRect(0, 0, side, side);
			for (int block = 0; block < blocksPerSide * blocksPerSide; ++block)
				paintBlock(g, block);
			for (int block : activeBlocks)
				paintActiveBorder(g, block);

			int end = board.size() * canvasPixels;      // right and bottom edges
			g.setColor(BLOCK_COLOR);
			g.drawLine(end, 0, end, end);
			g.drawLine(0, end, end, end);

			if (boardActive)
				paintBoardBorder(g);
		}
		finally
		{	g.dispose();
		}
	}

	private void paintCell(Graphics2D g, int row, int col)
	{
		int px = canvasPixels;
		g.setColor(board.isAlive(row, col) ? LIVE_COLOR : DEAD_COLOR);
		g.fillRect(col * px + 1, row * px + 1, px - 1, px - 1);
	}

	/** Cells, grid lines and the block outline, without the blue border. */
	private void paintBlock(Graphics2D g, int block)
	{
		int px = canvasPixels;
		int firstRow = block / blocksPerSide * BLOCK_SIZE;
		int firstCol = block % blocksPerSide * BLOCK_SIZE;
		int lastRow  = Math.min(firstRow + BLOCK_SIZE, board.size());
		int lastCol  = Math.min(firstCol + BLOCK_SIZE, board.size());

		for (int row = firstRow; row < lastRow; ++row)
			for (int col = firstCol; col < lastCol; ++col)
				paintCell(g, row, col);

		int top = firstRow * px, bottom = lastRow * px;
		int left = firstCol * px, right = lastCol * px;
		if (px >= 3)
		{
			g.setColor(BORDER_COLOR);
			for (int row = firstRow + 1; row < lastRow; ++row)
				g.drawLine(left, row * px, right - 1, row * px);
			for (int col = firstCol + 1; col < lastCol; ++col)
				g.drawLine(col * px, top, col * px, bottom - 1);
		}
		g.setColor(BLOCK_COLOR);
		g.drawLine(left, top, right - 1, top);
		g.drawLine(left, top, left, bottom - 1);
	}

	/** The original's blue rectangle just inside an active block. */
	private void paintActiveBorder(Graphics2D g, int block)
	{
		Rectangle r = blockBounds(block);
		g.setColor(ACTIVE_COLOR);
		g.drawRect(r.x + 1, r.y + 1, r.width - 2, r.height - 2);
	}

	/** The original outermost Neighborhood's blue rectangle. */
	private void paintBoardBorder(Graphics2D g)
	{
		int end = board.size() * canvasPixels;
		g.setColor(ACTIVE_COLOR);
		g.drawRect(1, 1, end - 2, end - 2);
	}

	// ---- geometry ----

	private Set<Integer> blocksOf(Collection<Coord> cells)
	{
		Set<Integer> blocks = new HashSet<>();
		for (Coord cell : cells)
			blocks.add(cell.row() / BLOCK_SIZE * blocksPerSide + cell.col() / BLOCK_SIZE);
		return blocks;
	}

	private Set<Integer> perimeterBlocks()
	{
		Set<Integer> blocks = new HashSet<>();
		int last = blocksPerSide - 1;
		for (int i = 0; i < blocksPerSide; ++i)
		{	blocks.add(i);                          // top row
			blocks.add(last * blocksPerSide + i);   // bottom row
			blocks.add(i * blocksPerSide);          // left column
			blocks.add(i * blocksPerSide + last);   // right column
		}
		return blocks;
	}

	private Rectangle blockBounds(int block)
	{
		int firstRow = block / blocksPerSide * BLOCK_SIZE;
		int firstCol = block % blocksPerSide * BLOCK_SIZE;
		int rows = Math.min(BLOCK_SIZE, board.size() - firstRow);
		int cols = Math.min(BLOCK_SIZE, board.size() - firstCol);
		return new Rectangle(firstCol * canvasPixels, firstRow * canvasPixels,
							 cols * canvasPixels, rows * canvasPixels);
	}

	private static Rectangle union(Rectangle sum, Rectangle more)
	{
		return sum == null ? more : sum.union(more);
	}
}
