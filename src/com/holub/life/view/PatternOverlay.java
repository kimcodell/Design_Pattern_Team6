package com.holub.life.view;

import java.awt.*;
import com.holub.life.pattern.*;

/** Outlines every recognized pattern on the board. A pattern found in
 *  the catalog gets a solid outline, one grouped by its behavior a
 *  dashed one; debris gets none. When a pattern is selected, only its
 *  outlines are drawn, thicker; selecting debris outlines the debris
 *  (dashed), so a selection never leaves the board without outlines.
 *
 *  <p>One color for every group: group colors are left to the view's
 *  color scheme.
 */
public final class PatternOverlay implements BoardOverlay
{
	private static final Color  OUTLINE  = new Color(40, 40, 40);
	private static final Stroke THIN     = new BasicStroke(1f);
	private static final Stroke THICK    = new BasicStroke(2f);
	private static final Stroke DASHED   = dashed(1f);
	private static final Stroke DASHED_THICK = dashed(2f);

	private final PatternMonitor monitor;
	private Runnable repaint = () -> {};
	private boolean visible = true;
	private String selected;            // pattern name, or null for all

	public PatternOverlay(PatternMonitor monitor)
	{
		this.monitor = monitor;
	}

	@Override public void attach(Runnable repaint)
	{
		this.repaint = repaint;
		monitor.addReportListener(report -> repaint.run());
	}

	public boolean isVisible() { return visible; }
	public String selected()   { return selected; }

	public void setVisible(boolean visible)
	{
		this.visible = visible;
		repaint.run();
	}

	/** Emphasize one pattern by name, or pass null to show all. */
	public void select(String name)
	{
		selected = name;
		repaint.run();
	}

	@Override public void paint(Graphics2D g, int px)
	{
		if (!visible)
			return;
		g.setColor(OUTLINE);
		for (Detection d : monitor.latest().detections())
		{
			boolean emphasized = d.name().equals(selected);
			if (selected != null ? !emphasized : !d.isClassified())
				continue;
			boolean catalog = d.source() == Detection.Source.CATALOG;
			g.setStroke(emphasized ? (catalog ? THICK : DASHED_THICK)
								   : (catalog ? THIN  : DASHED));
			g.drawRect(d.left() * px - 2, d.top() * px - 2,
					   d.width() * px + 3, d.height() * px + 3);
		}
	}

	private static Stroke dashed(float width)
	{
		return new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
							   10f, new float[] {4f, 3f}, 0f);
	}
}
