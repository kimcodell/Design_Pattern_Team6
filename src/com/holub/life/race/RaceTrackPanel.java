package com.holub.life.race;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JPanel;
import com.holub.life.model.BoardSnapshot;
import com.holub.life.model.Coord;
import com.holub.life.model.LifeModel;

final class RaceTrackPanel extends JPanel implements RaceObserver
{
	private static final int LANE_HEIGHT = 48;
	private static final int TRACK_TOP = 35;
	private static final int CELL_SIZE = 6;
	private static final Color TRACK = new Color(0x102A28);
	private static final Color LANE = new Color(0x1D3B36);
	private static final Color ALT_LANE = new Color(0x17332F);
	private static final Color LINE = new Color(0x87A698);
	private static final Color LABEL = new Color(0xF2EAD9);
	private static final Color GOLD = new Color(0xF8D37B);
	private static final Color[] RACER_COLORS = {
		new Color(0x72E4C4), new Color(0xFBB78C), new Color(0xA5BBFF),
		new Color(0xF1E08A), new Color(0xEAABE0), new Color(0xACE58F)
	};

	private static final List<Set<Coord>> GLIDER_FRAMES = createGliderFrames();

	private List<Double> progress = List.of();
	private int pick;
	private int winner;

	void showStartingLine(int racerCount, int pick)
	{
		this.pick = pick;
		winner = 0;
		progress = Collections.nCopies(racerCount, 0.0);
		setPreferredSize(new Dimension(680, TRACK_TOP + racerCount * LANE_HEIGHT + 14));
		repaint();
	}

	@Override public void raceAdvanced(GliderRace.Frame frame)
	{
		progress = frame.progress();
		repaint();
	}

	@Override public void raceFinished(GliderRace.Result result)
	{
		winner = result.winner();
		repaint();
	}

	@Override protected void paintComponent(Graphics graphics)
	{
		super.paintComponent(graphics);
		Graphics2D track = (Graphics2D)graphics.create();
		try
		{
			track.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			track.setColor(TRACK);
			track.fillRect(0, 0, getWidth(), getHeight());

			int startX = 92;
			int finishX = getWidth() - 52;
			int laneBottom = TRACK_TOP + progress.size() * LANE_HEIGHT;
			track.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
			track.setColor(LABEL);
			drawCentered(track, "START", startX, 22);
			drawCentered(track, "FINISH", finishX, 22);

			for (int index = 0; index < progress.size(); index++)
			{
				int laneTop = TRACK_TOP + index * LANE_HEIGHT;
				track.setColor(index % 2 == 0 ? LANE : ALT_LANE);
				track.fillRect(12, laneTop, getWidth() - 24, LANE_HEIGHT - 1);
			}

			track.setColor(LINE);
			track.drawLine(startX, TRACK_TOP, startX, laneBottom);
			drawFinishLine(track, finishX, laneBottom);

			for (int index = 0; index < progress.size(); index++)
				drawRacer(track, index, startX, finishX);
		}
		finally
		{
			track.dispose();
		}
	}

	private void drawRacer(Graphics2D track, int index, int startX, int finishX)
	{
		int racer = index + 1;
		int laneCenter = TRACK_TOP + index * LANE_HEIGHT + LANE_HEIGHT / 2;
		double position = progress.get(index);
		int centerX = startX + (int)Math.round(position / GliderRace.FINISH_GENERATION * (finishX - startX));
		int generation = (int)position;

		track.setColor(RACER_COLORS[index]);
		track.drawString("#" + racer, 22, laneCenter + 4);
		if (racer == pick)
		{
			track.setColor(GOLD);
			track.drawString("BET", 45, laneCenter + 4);
		}
		if (racer == winner)
		{
			track.setColor(GOLD);
			track.drawRect(15, laneCenter - LANE_HEIGHT / 2 + 2, getWidth() - 31, LANE_HEIGHT - 5);
		}

		track.setColor(RACER_COLORS[index]);
		for (Coord cell : GLIDER_FRAMES.get(generation))
		{
			int x = centerX + cell.col() * CELL_SIZE - 3 * CELL_SIZE / 2;
			int y = laneCenter + cell.row() * CELL_SIZE - 3 * CELL_SIZE / 2;
			track.fillRect(x, y, CELL_SIZE - 1, CELL_SIZE - 1);
		}
	}

	private static void drawFinishLine(Graphics2D track, int finishX, int laneBottom)
	{
		int tile = 4;
		for (int y = TRACK_TOP; y < laneBottom; y += tile)
		{
			boolean lightLeft = ((y - TRACK_TOP) / tile) % 2 == 0;
			track.setColor(lightLeft ? LABEL : TRACK);
			track.fillRect(finishX - tile, y, tile, tile);
			track.setColor(lightLeft ? TRACK : LABEL);
			track.fillRect(finishX, y, tile, tile);
		}
	}

	private static void drawCentered(Graphics2D track, String text, int x, int y)
	{
		track.drawString(text, x - track.getFontMetrics().stringWidth(text) / 2, y);
	}

	private static List<Set<Coord>> createGliderFrames()
	{
		LifeModel board = LifeModel.builder().build();
		board.restore(new BoardSnapshot(board.size(), Set.of(
				new Coord(3, 4), new Coord(4, 5), new Coord(5, 3), new Coord(5, 4), new Coord(5, 5))));

		List<Set<Coord>> frames = new ArrayList<>(GliderRace.FINISH_GENERATION + 1);
		for (int generation = 0; generation <= GliderRace.FINISH_GENERATION; generation++)
		{
			// Keep the evolving cell shape, centered in its horizontal race lane.
			Set<Coord> liveCells = board.snapshot().liveCells();
			int top = liveCells.stream().mapToInt(Coord::row).min().orElseThrow();
			int left = liveCells.stream().mapToInt(Coord::col).min().orElseThrow();
			Set<Coord> localCells = new HashSet<>();
			for (Coord cell : liveCells)
				localCells.add(new Coord(cell.row() - top, cell.col() - left));
			frames.add(Set.copyOf(localCells));
			if (generation < GliderRace.FINISH_GENERATION)
				board.step();
		}
		return List.copyOf(frames);
	}
}
