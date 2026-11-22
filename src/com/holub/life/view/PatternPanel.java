package com.holub.life.view;

import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.util.*;
import java.util.List;
import javax.swing.*;
import com.holub.life.model.Coord;
import com.holub.life.pattern.*;
import com.holub.life.pattern.Shape;

/** Side panel listing the patterns now on the board, grouped into the
 *  five categories, with counts. Selecting a pattern emphasizes its
 *  outlines on the board; clicking it again goes back to all outlines.
 *
 *  <p>Reads only the {@link PatternReport}s that the monitor sends.
 *  Analysis is switched off while the panel is not showing.
 */
public final class PatternPanel extends JPanel
{
	/** A list line: a category heading, or a pattern under it.
	 *  @param detail second line in small type, or null.
	 *  @param count  right-aligned number.
	 */
	private record Row(String text, String detail, String count, Shape shape,
					   String patternName, boolean heading)
	{
		static Row heading(String text, String count)
		{
			return new Row(text, null, count, null, null, true);
		}
	}

	/** Longest rows that can appear, used to size the panel so that
	 *  nothing is cut off and no horizontal scrolling is needed.
	 */
	private static final List<Row> WIDEST_ROWS = List.of(
		Row.heading("Gun · Puffer · Breeder", "999"),
		new Row("Unlisted oscillator (p60)", "Not in catalog · judged by running it", "×999",
				Shape.parse("O"), "", false),
		new Row("Gosper glider gun", "1 glider every 30 gens", "×999",
				Shape.parse("O"), "", false));

	private final PatternMonitor monitor;
	private final PatternOverlay overlay;

	private final Map<PatternCategory, JLabel> counts = new EnumMap<>(PatternCategory.class);
	private final JPanel summary = new JPanel(new GridLayout(1, 0, 8, 0));
	private final JLabel notice  = new JLabel(" ");
	private final DefaultListModel<Row> found = new DefaultListModel<>();
	private final JList<Row> foundList = new JList<>(found);
	private boolean updating;

	public PatternPanel(PatternMonitor monitor, PatternOverlay overlay)
	{
		super(new BorderLayout());
		this.monitor = monitor;
		this.overlay = overlay;

		JLabel title = new JLabel("Patterns");
		title.setFont(title.getFont().deriveFont(Font.BOLD));
		JCheckBox outlines = new JCheckBox("Show outlines", overlay.isVisible());
		outlines.addActionListener(e -> overlay.setVisible(outlines.isSelected()));
		JPanel heading = new JPanel(new BorderLayout());
		heading.add(title, BorderLayout.WEST);
		heading.add(outlines, BorderLayout.EAST);

		for (PatternCategory category : PatternCategory.values())
		{	JLabel count = new JLabel(category.shortName() + " 999", SwingConstants.CENTER);
			count.setToolTipText(category.title() + ": " + category.description());
			counts.put(category, count);
			summary.add(count);
		}
		JPanel top = new JPanel(new BorderLayout(0, 2));
		top.add(heading, BorderLayout.NORTH);
		top.add(summary, BorderLayout.CENTER);
		top.add(notice, BorderLayout.SOUTH);
		top.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

		foundList.setCellRenderer(new RowRenderer());
		foundList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		foundList.addListSelectionListener(e ->
		{	if (updating || e.getValueIsAdjusting())
				return;
			Row row = foundList.getSelectedValue();
			if (row != null && row.heading())
			{	foundList.clearSelection();         // headings are not selectable
				return;
			}
			overlay.select(row == null ? null : row.patternName());
		});
		// Clicking the selected pattern again, or the empty space below the
		// rows, goes back to showing every outline. The list itself keeps a
		// row selected when it is pressed again, and picks the last row for
		// a press below the rows; both are undone here, after the list's own
		// handling. The pattern the press started from is the overlay's:
		// the list's selection is already changing ("adjusting") by now and
		// settles only when the button is released.
		foundList.addMouseListener(new java.awt.event.MouseAdapter()
		{
			@Override public void mousePressed(java.awt.event.MouseEvent e)
			{
				int index = foundList.locationToIndex(e.getPoint());
				boolean onRow = index >= 0 && foundList.getCellBounds(index, index).contains(e.getPoint());
				Row row = onRow ? found.get(index) : null;
				boolean again = row != null && !row.heading()
								&& row.patternName().equals(overlay.selected());
				if (!onRow || again)
					foundList.clearSelection();
			}
		});

		add(top, BorderLayout.NORTH);
		add(verticalScroll(foundList), BorderLayout.CENTER);
		setPreferredSize(new Dimension(fittingWidth(), 0));

		monitor.addReportListener(this::show);
		addHierarchyListener(e ->
		{	if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0)
				monitor.setActive(isShowing());
		});
		show(monitor.latest());
	}

	private void show(PatternReport report)
	{
		for (PatternCategory category : PatternCategory.values())
			counts.get(category).setText(category.shortName() + " " + report.count(category));
		notice.setText(monitor.analyzer().usesCatalog(monitor.board())
					   ? "Generation " + report.generation()
					   : "<html>Generation " + report.generation() + " · The rule is not "
						 + monitor.analyzer().catalog().rule() + ", so the catalog is off "
						 + "and patterns are judged by running them only.</html>");

		Map<String, String> notes = new HashMap<>();
		for (PatternDefinition d : monitor.analyzer().catalog().definitions())
			notes.put(d.name(), d.note());

		List<Row> rows = new ArrayList<>();
		Map<PatternCategory, List<PatternReport.Group>> grouped = report.grouped();
		for (PatternCategory category : PatternCategory.values())
		{
			List<PatternReport.Group> groups = grouped.get(category);
			rows.add(Row.heading(category.title(), String.valueOf(total(groups))));
			for (PatternReport.Group g : groups)
				rows.add(patternRow(g, notes));
		}
		List<PatternReport.Group> debris = report.unclassified();
		rows.add(Row.heading("Unclassified", String.valueOf(total(debris))));
		for (PatternReport.Group g : debris)
			rows.add(patternRow(g, notes));

		String selected = overlay.selected();
		updating = true;
		try
		{	found.clear();
			found.addAll(rows);
			int keep = -1;
			for (int i = 0; i < rows.size(); ++i)
				if (!rows.get(i).heading() && rows.get(i).patternName().equals(selected))
					keep = i;
			if (keep >= 0)
				foundList.setSelectedIndex(keep);
		}
		finally
		{	updating = false;
		}
	}

	private static Row patternRow(PatternReport.Group g, Map<String, String> notes)
	{
		String detail = switch (g.source())
		{
			case CATALOG      -> notes.getOrDefault(g.name(), "");
			case BEHAVIOR     -> "Not in catalog · judged by running it";
			case UNCLASSIFIED -> "Still changing, or dying out";
		};
		Detection first = g.detections().get(0);
		return new Row(g.name(), detail, "×" + g.count(), new Shape(first.cells()).normalized(),
					   g.name(), false);
	}

	private static int total(List<PatternReport.Group> groups)
	{
		return groups.stream().mapToInt(PatternReport.Group::count).sum();
	}

	// ---- sizing ----

	/** Scrolls up and down only. The scroll bar is always there so the
	 *  width does not jump when the list grows.
	 */
	private static JScrollPane verticalScroll(JList<Row> list)
	{
		JScrollPane scroll = new JScrollPane(list,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		return scroll;
	}

	/** Panel width that shows the widest row that can occur and the
	 *  counts line, plus the scroll bar and borders.
	 */
	private int fittingWidth()
	{
		JList<Row> widest = new JList<>(WIDEST_ROWS.toArray(new Row[0]));
		widest.setCellRenderer(new RowRenderer());
		widest.setFont(foundList.getFont());
		int scrollBar = Math.max(UIManager.getInt("ScrollBar.width"), 15);
		int borders = 20;                       // panel padding and scroll pane border
		int rows = widest.getPreferredSize().width + scrollBar;
		return Math.max(rows, summary.getPreferredSize().width) + borders;
	}

	// ---- drawing a row ----

	private static final class RowRenderer extends JPanel implements ListCellRenderer<Row>
	{
		private final ShapeIcon icon   = new ShapeIcon();
		private final JLabel picture   = new JLabel(icon);
		private final JLabel name      = new JLabel();
		private final JLabel detail    = new JLabel();
		private final JLabel count     = new JLabel();

		RowRenderer()
		{
			super(new BorderLayout(8, 0));
			JPanel lines = new JPanel();
			lines.setLayout(new BoxLayout(lines, BoxLayout.Y_AXIS));   // skips a hidden detail line
			lines.setOpaque(false);
			name.setAlignmentX(LEFT_ALIGNMENT);
			detail.setAlignmentX(LEFT_ALIGNMENT);
			lines.add(name);
			lines.add(detail);
			add(picture, BorderLayout.WEST);
			add(lines,   BorderLayout.CENTER);
			add(count,   BorderLayout.EAST);
		}

		@Override public Component getListCellRendererComponent(JList<? extends Row> list, Row row,
																int index, boolean selected, boolean focused)
		{
			Font base = list.getFont();
			icon.shape = row.shape();
			picture.setVisible(row.shape() != null);
			name.setText(row.text());
			name.setFont(row.heading() ? base.deriveFont(Font.BOLD) : base);
			detail.setText(row.detail());
			detail.setVisible(row.detail() != null);
			detail.setFont(base.deriveFont(base.getSize2D() - 2f));
			count.setText(row.count());
			count.setFont(row.heading() ? base.deriveFont(base.getSize2D() - 1f) : base);
			setBorder(BorderFactory.createEmptyBorder(row.heading() && index > 0 ? 10 : 3, 6, 3, 8));

			Color foreground = selected ? list.getSelectionForeground() : list.getForeground();
			setBackground(selected ? list.getSelectionBackground() : list.getBackground());
			name.setForeground(foreground);
			count.setForeground(row.heading() && !selected ? Color.GRAY : foreground);
			detail.setForeground(selected ? foreground : Color.GRAY);
			picture.setForeground(foreground);
			return this;
		}
	}

	/** A small picture of a shape. */
	private static final class ShapeIcon implements Icon
	{
		private static final int SIDE = 24;
		Shape shape;

		@Override public int getIconWidth()  { return SIDE; }
		@Override public int getIconHeight() { return SIDE; }

		@Override public void paintIcon(Component c, Graphics g, int x, int y)
		{
			if (shape == null || shape.isEmpty())
				return;
			int cell = Math.max(1, Math.min(SIDE / shape.height(), SIDE / shape.width()));
			int ox = x + (SIDE - cell * shape.width()) / 2;
			int oy = y + (SIDE - cell * shape.height()) / 2;
			int side = Math.max(1, cell - (cell > 3 ? 1 : 0));
			g.setColor(c.getForeground());
			for (Coord p : shape.cells())
				g.fillRect(ox + p.col() * cell, oy + p.row() * cell, side, side);
		}
	}
}
