package com.holub.life.bench;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.*;
import com.holub.life.model.*;
import com.holub.life.view.UniversePanel;

/** Measures, for several board sizes, one generation (step), one full
 *  repaint, and the per-generation cost of redrawing only what changed.
 *  Prints numbers instead of asserting them, because timings depend on
 *  the machine. Run with scripts/bench.sh.
 *
 *  <ul>
 *  <li>step: model.step() with nobody listening (a copy of the board).
 *  <li>full paint: drawing the whole board, as after Clear or Load.
 *  <li>partial: extra time step() takes when the panel listens, i.e.
 *      redrawing the changed cells and blue block borders into the image.
 *  <li>copy: copying the whole image to the screen (an upper bound: Swing
 *      copies only the changed rectangle).
 *  </ul>
 */
public final class Benchmark
{
	private static final int WARM_UP = 30, GENERATIONS = 50;

	public static void main(String[] args)
	{
		double density = args.length > 0 ? Double.parseDouble(args[0]) : 0.3;
		System.out.printf("density %.0f%%, %d generations after %d warm-up generations (ms)%n",
						  density * 100, GENERATIONS, WARM_UP);
		System.out.println("   size     step  full paint  partial    copy   step+partial+copy");
		for (int size : new int[] { 64, 128, 256, 512, 1024 })
		{
			LifeModel model = LifeModel.builder().size(size).build();
			Random random = new Random(42);
			Set<Coord> live = new HashSet<>();
			for (int r = 0; r < size; ++r)
				for (int c = 0; c < size; ++c)
					if (random.nextDouble() < density) live.add(new Coord(r, c));
			model.restore(new BoardSnapshot(size, live));
			LifeModel plain = model.copy();         // same board, no listeners

			int cell = Math.max(2, 512 / size);
			int side = size * cell;
			BufferedImage screen = new BufferedImage(side, side, BufferedImage.TYPE_INT_RGB);
			UniversePanel panel = new UniversePanel(model, cell);
			panel.setSize(side, side);
			paint(panel, screen);

			for (int i = 0; i < WARM_UP; ++i)
			{	plain.step();
				model.step();
				paint(panel, screen);
			}
			long stepTotal = 0, withPanelTotal = 0, copyTotal = 0;
			for (int i = 0; i < GENERATIONS; ++i)
			{
				long t0 = System.nanoTime();
				plain.step();
				long t1 = System.nanoTime();
				model.step();
				long t2 = System.nanoTime();
				paint(panel, screen);
				long t3 = System.nanoTime();
				stepTotal      += t1 - t0;
				withPanelTotal += t2 - t1;
				copyTotal      += t3 - t2;
			}

			long bestFull = Long.MAX_VALUE;
			for (int i = 0; i < 5; ++i)
			{
				UniversePanel fresh = new UniversePanel(model, cell);
				model.removeListener(fresh);
				fresh.setSize(side, side);
				long t0 = System.nanoTime();
				paint(fresh, screen);
				bestFull = Math.min(bestFull, System.nanoTime() - t0);
			}

			double step    = stepTotal / 1e6 / GENERATIONS;
			double partial = Math.max(0, (withPanelTotal - stepTotal) / 1e6 / GENERATIONS);
			double copy    = copyTotal / 1e6 / GENERATIONS;
			System.out.printf("%7d  %7.2f  %10.2f  %7.2f  %6.2f  %18.2f%n",
							  size, step, bestFull / 1e6, partial, copy, step + partial + copy);
		}
	}

	private static void paint(UniversePanel panel, BufferedImage screen)
	{
		Graphics g = screen.getGraphics();
		panel.paint(g);
		g.dispose();
	}
}
