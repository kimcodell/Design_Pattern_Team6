package com.holub.life;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.*;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Manual GUI check (needs a display, so it is not a JUnit test).
 *  Starts the real Life window and drives it: clicks, single steps,
 *  store/clear/load, a file in the original format, running and halting,
 *  and a bad file. Reads the board back from the painted pixels and
 *  saves a screenshot per step. Run with scripts/gui-check.sh.
 */
public class GuiSmokeCheck
{
	static final File DIR = new File(System.getProperty("check.dir", "test-output/gui-check"));
	static JFrame frame; static JComponent panel;
	static int failures = 0, shot = 0;

	public static void main(String[] args) throws Exception
	{
		DIR.mkdirs();
		Life.main(new String[0]);
		for (int i = 0; i < 100 && frame == null; ++i)
		{   Thread.sleep(100);
			for (Frame f : Frame.getFrames()) if (f.isShowing() && f instanceof JFrame j) frame = j;
		}
		check("창이 열린다", frame != null);
		onEdt(() -> panel = find(frame.getContentPane(), "UniversePanel"));
		check("보드 패널이 있다", panel != null);
		System.out.println("메뉴: " + menuNames());

		// 1. Click a horizontal blinker at row 10, columns 9..11
		for (int c = 9; c <= 11; ++c) click(10, c);
		expect("클릭으로 가로 블링커를 만든다", "clicked-blinker", Set.of("10,9", "10,10", "10,11"));

		// 2. Single step twice: vertical, then horizontal again
		menu("Go", "Tick (Single Step)");
		expect("한 틱 진행하면 세로가 된다", "after-one-tick", Set.of("9,10", "10,10", "11,10"));
		menu("Go", "Tick (Single Step)");
		expect("두 틱 진행하면 다시 가로가 된다", "after-two-ticks", Set.of("10,9", "10,10", "10,11"));

		// 3. Clicking a live cell kills it, clicking again revives it
		click(10, 9); expect("산 셀을 클릭하면 꺼진다", "toggle-off", Set.of("10,10", "10,11"));
		click(10, 9); expect("다시 클릭하면 켜진다", "toggle-on", Set.of("10,9", "10,10", "10,11"));

		// 4. Store, clear, load
		File saved = new File(DIR, "blinker.life"); saved.delete();
		chooseFileThen(saved, () -> menu("Grid", "Store"));
		check("Store로 파일이 저장된다", saved.length() > 0);
		menu("Grid", "Clear"); expect("Clear로 보드가 비워진다", "after-clear", Set.of());
		chooseFileThen(saved, () -> menu("Grid", "Load"));
		expect("Load로 저장한 보드가 돌아온다", "after-load", Set.of("10,9", "10,10", "10,11"));

		// 5. A file in the original format: LinkedList<Point>, each cell twice
		File legacy = new File(DIR, "legacy.life");
		LinkedList<Point> cells = new LinkedList<>();
		for (Point p : List.of(new Point(31, 30), new Point(32, 31), new Point(30, 32), new Point(31, 32), new Point(32, 32)))
		{   cells.add(p); cells.add(p); }
		try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(legacy))) { out.writeObject(cells); }
		chooseFileThen(legacy, () -> menu("Grid", "Load"));
		expect("원본 형식 파일(셀 중복 기록)을 불러온다", "legacy-glider-loaded", Set.of("30,31", "31,32", "32,30", "32,31", "32,32"));

		// 6. Run fast for a while, then halt: the glider must still be 5 cells and have moved
		menu("Go", "Fast");
		Thread.sleep(1500);
		menu("Go", "Halt");
		Thread.sleep(200);
		Set<String> moved = live();
		check("Fast로 실행 후 Halt: 글라이더가 5셀 그대로다 (" + moved.size() + ")", moved.size() == 5);
		check("글라이더가 이동했다", !moved.contains("30,31") || !moved.contains("31,32"));
		screenshot("running-glider");

		// 7. A file that is not a Life file shows the error dialog instead of crashing
		File junk = new File(DIR, "junk.life");
		try (FileWriter w = new FileWriter(junk)) { w.write("not a life file"); }
		AtomicBoolean errorShown = new AtomicBoolean();
		Thread closer = new Thread(() -> {
			for (int i = 0; i < 100; ++i)
			{   try { Thread.sleep(100); } catch (InterruptedException e) { return; }
				for (Window w : Window.getWindows())
					if (w instanceof JDialog d && d.isShowing() && find(d, "JOptionPane") != null)
					{   errorShown.set(true);
						SwingUtilities.invokeLater(d::dispose);
						return;
					}
			}
		});
		closer.start();
		chooseFileThen(junk, () -> menu("Grid", "Load"));
		closer.join();
		check("잘못된 파일은 오류 대화상자를 띄운다", errorShown.get());
		check("잘못된 파일을 불러와도 보드는 그대로다", live().equals(moved));

		System.out.println(failures == 0 ? "GUI 점검 모두 통과" : "GUI 점검 실패 " + failures + "건");
		System.exit(failures == 0 ? 0 : 1);
	}

	// ---- helpers ----

	static void check(String what, boolean ok)
	{   System.out.println((ok ? "  ✔ " : "  ✘ ") + what);
		if (!ok) ++failures;
	}

	static void expect(String what, String file, Set<String> cells) throws Exception
	{   Set<String> live = live();
		check(what + " " + new TreeSet<>(live), live.equals(cells));
		screenshot(file);
	}

	static void onEdt(Runnable r) throws Exception { SwingUtilities.invokeAndWait(r); }

	static JComponent find(Container root, String simpleName)
	{   for (Component c : root.getComponents())
		{   if (c.getClass().getSimpleName().equals(simpleName)) return (JComponent) c;
			if (c instanceof Container k) { JComponent f = find(k, simpleName); if (f != null) return f; }
		}
		return null;
	}

	static List<String> menuNames()
	{   List<String> names = new ArrayList<>();
		JMenuBar bar = frame.getJMenuBar();
		for (int i = 0; i < bar.getMenuCount(); ++i)
		{   JMenu m = bar.getMenu(i);
			for (int j = 0; j < m.getItemCount(); ++j)
				if (m.getItem(j) != null) names.add(m.getText() + "/" + m.getItem(j).getText());
		}
		return names;
	}

	static void menu(String menu, String item) throws Exception
	{   JMenuItem[] found = new JMenuItem[1];
		onEdt(() -> {
			JMenuBar bar = frame.getJMenuBar();
			for (int i = 0; i < bar.getMenuCount(); ++i)
				if (bar.getMenu(i).getText().equals(menu))
					for (int j = 0; j < bar.getMenu(i).getItemCount(); ++j)
					{   JMenuItem it = bar.getMenu(i).getItem(j);
						if (it != null && it.getText().equals(item)) found[0] = it;
					}
		});
		if (found[0] == null) throw new IllegalStateException("no menu item " + menu + "/" + item);
		onEdt(found[0]::doClick);
		onEdt(() -> {});            // let repaint requests run
	}

	/** Answer the next file chooser with the given file, while action runs. */
	static void chooseFileThen(File file, Callable action) throws Exception
	{   Thread answerer = new Thread(() -> {
			for (int i = 0; i < 100; ++i)
			{   try { Thread.sleep(100); } catch (InterruptedException e) { return; }
				for (Window w : Window.getWindows())
					if (w.isShowing() && w instanceof JDialog d)
					{   JComponent chooser = find(d.getContentPane(), "JFileChooser");
						if (chooser instanceof JFileChooser fc)
						{   SwingUtilities.invokeLater(() -> { fc.setSelectedFile(file); fc.approveSelection(); });
							return;
						}
					}
			}
		});
		answerer.start();
		action.call();
		answerer.join();
	}
	interface Callable { void call() throws Exception; }

	static void click(int row, int col) throws Exception
	{   onEdt(() -> {
			int px = Math.min(panel.getWidth(), panel.getHeight()) / 64;
			int x = col * px + px / 2, y = row * px + px / 2;
			panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
											   0, x, y, 1, false, MouseEvent.BUTTON1));
		});
		onEdt(() -> {});
	}

	static BufferedImage paintPanel() throws Exception
	{   BufferedImage[] img = new BufferedImage[1];
		onEdt(() -> {
			img[0] = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
			Graphics g = img[0].getGraphics(); panel.paint(g); g.dispose();
		});
		return img[0];
	}

	/** Live cells read from the pixels: a red cell center is alive. */
	static Set<String> live() throws Exception
	{   BufferedImage img = paintPanel();
		int px = Math.min(img.getWidth(), img.getHeight()) / 64;
		Set<String> live = new HashSet<>();
		for (int r = 0; r < 64; ++r)
			for (int c = 0; c < 64; ++c)
				if ((img.getRGB(c * px + px / 2, r * px + px / 2) & 0xffffff) == 0xff0000) live.add(r + "," + c);
		return live;
	}

	static void screenshot(String name) throws Exception
	{   BufferedImage[] img = new BufferedImage[1];
		onEdt(() -> {
			JRootPane root = frame.getRootPane();
			img[0] = new BufferedImage(root.getWidth(), root.getHeight(), BufferedImage.TYPE_INT_RGB);
			Graphics g = img[0].getGraphics(); root.paint(g); g.dispose();
		});
		ImageIO.write(img[0], "png", new File(DIR, String.format("%02d-%s.png", ++shot, name)));
	}
}
