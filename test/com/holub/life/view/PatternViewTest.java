package com.holub.life.view;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import com.holub.life.model.*;
import com.holub.life.pattern.*;

/** 실제 프로그램처럼 EDT에서 실행한다(패널은 EDT 밖의 알림을 거부함). */
@DisplayName("패턴 화면: 윤곽 PatternOverlay")
@ExtendWith(OnEventDispatchThread.class)
class PatternViewTest
{
	/** 목적: 윤곽이 보드 그림 위에 그려지고, 끄면 사라지며, 보드 그림 자체(바뀐
	 *  셀만 그리는 캔버스)는 건드리지 않는지.
	 *  방법: 블록 하나를 놓은 패널을 이미지로 그려, 윤곽 자리의 픽셀을 윤곽 켜기/끄기로 비교.
	 *  기대: 켜면 윤곽색, 끄면 원래 보드 색.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[패턴] 윤곽은 보드 위에만 그려지고 끄면 사라진다")
	void overlayDrawsOverTheBoard()
	{
		LifeModel model = LifeModel.builder().build();
		UniversePanel panel = new UniversePanel(model, 8);
		panel.setSize(512, 512);
		PatternMonitor monitor = new PatternMonitor(model, new PatternAnalyzer(PatternCatalog.conway()));
		PatternOverlay overlay = new PatternOverlay(monitor);
		panel.addOverlay(overlay);
		model.apply(BoardEdit.set(Set.of(new Coord(10, 10), new Coord(10, 11),
										 new Coord(11, 10), new Coord(11, 11)), true));

		int x = 10 * 8 - 2, y = 10 * 8 + 4;        // left edge of the outline
		int on = render(panel).getRGB(x, y);
		overlay.setVisible(false);
		int off = render(panel).getRGB(x, y);
		assertNotEquals(off, on);
		assertEquals(0xFF282828, on);
	}

	/** 목적: 목록에서 잔해(Debris)를 골라도 보드에 윤곽이 남는지. 원래는 고른
	 *  그룹만 그리는 규칙과 잔해는 그리지 않는 규칙이 겹쳐 윤곽이 모두 사라졌다.
	 *  방법: 한 세대 지난 R-pentomino(잔해)를 두고 Debris를 고른 뒤, 잔해 상자의
	 *  왼쪽 테두리 픽셀을 본다.
	 *  기대: 윤곽색.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[패턴] 잔해를 고르면 잔해에 윤곽을 그린다 (윤곽이 사라지지 않음)")
	void selectingDebrisOutlinesTheDebris()
	{
		LifeModel model = LifeModel.builder().build();
		UniversePanel panel = new UniversePanel(model, 8);
		panel.setSize(512, 512);
		PatternMonitor monitor = new PatternMonitor(model, new PatternAnalyzer(PatternCatalog.conway()));
		PatternOverlay overlay = new PatternOverlay(monitor);
		panel.addOverlay(overlay);
		model.apply(BoardEdit.set(Set.of(new Coord(20, 21), new Coord(20, 22), new Coord(21, 20),
										 new Coord(21, 21), new Coord(22, 21)), true));   // R-pentomino
		model.step();

		Detection debris = monitor.latest().detections().stream()
								  .filter(d -> !d.isClassified()).findFirst().orElseThrow();
		overlay.select(debris.name());
		BufferedImage image = render(panel);
		int x = debris.left() * 8 - 2, top = debris.top() * 8;
		boolean outlined = false;                  // dashed: look along the edge, not at one pixel
		for (int y = top; y < top + 16; ++y)
			outlined |= image.getRGB(x, y) == 0xFF282828;
		assertTrue(outlined);
	}

	/** 목적: 목록에서 고른 패턴을 다시 누르거나 목록 아래 빈 곳을 누르면 선택이
	 *  풀리고 모든 윤곽으로 돌아가는지. Swing 목록은 기본으로 다시 눌러도 선택을
	 *  유지하고, 빈 곳을 누르면 마지막 줄(잔해)을 고른다.
	 *  방법: Block 줄을 누르고, 다시 누르고, 빈 곳을 누른다. 목록 자체의 마우스
	 *  처리는 화면 없는 환경에서 돌지 않으므로, 그 동작(누른 줄 선택, 빈 곳이면
	 *  마지막 줄 선택)을 흉내 낸 뒤 패널이 붙인 리스너만 부른다.
	 *  기대: Block 선택 → 선택 없음 → 선택 없음.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[패턴] 고른 줄을 다시 누르거나 빈 곳을 누르면 전체 윤곽으로 돌아간다")
	void clickingTheSelectedRowAgainShowsAllOutlines()
	{
		LifeModel model = LifeModel.builder().build();
		PatternMonitor monitor = new PatternMonitor(model, new PatternAnalyzer(PatternCatalog.conway()));
		PatternOverlay overlay = new PatternOverlay(monitor);
		PatternPanel side = new PatternPanel(monitor, overlay);
		model.apply(BoardEdit.set(Set.of(new Coord(5, 5), new Coord(5, 6),
										 new Coord(6, 5), new Coord(6, 6)), true));    // block
		side.setSize(side.getPreferredSize().width, 800);
		layOut(side);
		@SuppressWarnings("unchecked")
		javax.swing.JList<Object> list = find(side, javax.swing.JList.class);

		int lastRow = list.getModel().getSize() - 1;
		java.awt.Rectangle block = list.getCellBounds(1, 1);   // row 0 is the "Still Life" heading
		java.awt.Rectangle last  = list.getCellBounds(lastRow, lastRow);

		press(list, 1, block.x + 5, block.y + 5);
		assertEquals("Block", overlay.selected());
		press(list, 1, block.x + 5, block.y + 5);              // the same row again
		assertNull(overlay.selected());

		press(list, 1, block.x + 5, block.y + 5);
		press(list, lastRow, 5, last.y + last.height + 40);     // empty space below the rows
		assertNull(overlay.selected());
		assertEquals(-1, list.getSelectedIndex());
	}

	/** A press and release as the list sees them. On the press the list's
	 *  own handler starts adjusting and selects <code>listSelects</code> (it
	 *  picks the last row for a press below the rows); then the listeners
	 *  the panel added run; the release ends adjusting.
	 */
	private static void press(javax.swing.JList<?> list, int listSelects, int x, int y)
	{
		list.setValueIsAdjusting(true);
		list.setSelectedIndex(listSelects);
		java.awt.event.MouseEvent e = new java.awt.event.MouseEvent(list,
				java.awt.event.MouseEvent.MOUSE_PRESSED, 0, 0, x, y, 1, false,
				java.awt.event.MouseEvent.BUTTON1);
		for (java.awt.event.MouseListener l : list.getMouseListeners())
			if (l.getClass().getName().startsWith(PatternPanel.class.getName()))
				l.mousePressed(e);
		list.setValueIsAdjusting(false);
	}

	private static void layOut(java.awt.Component c)
	{
		if (c instanceof java.awt.Container k)
		{	k.doLayout();
			for (java.awt.Component child : k.getComponents()) layOut(child);
		}
	}

	private static <T> T find(java.awt.Component root, Class<T> type)
	{
		if (type.isInstance(root)) return type.cast(root);
		if (root instanceof java.awt.Container k)
			for (java.awt.Component child : k.getComponents())
			{	T found = find(child, type);
				if (found != null) return found;
			}
		return null;
	}

	private static BufferedImage render(UniversePanel panel)
	{
		BufferedImage image = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);
		java.awt.Graphics2D g = image.createGraphics();
		panel.paint(g);
		g.dispose();
		return image;
	}
}
