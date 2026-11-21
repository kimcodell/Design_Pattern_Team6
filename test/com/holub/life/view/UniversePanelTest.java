package com.holub.life.view;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import com.holub.life.model.*;

/** 화면(UniversePanel)도 창을 띄우지 않고 시험한다. 패널을 이미지에
 *  그린 뒤 각 셀 가운데 픽셀 색으로 산 셀을 읽는다(빨강 = 살아 있음).
 *  실제 프로그램처럼 모든 테스트를 EDT에서 실행한다(패널은 EDT 밖의 알림을 거부함).
 */
@DisplayName("화면 UniversePanel (창 없이 이미지로 검사)")
@ExtendWith(OnEventDispatchThread.class)
class UniversePanelTest
{
	private static final int CELL = 8;

	/** 저장된 스냅샷 하나를 보여 주는 BoardView. LifeModel이 아니어도
	 *  같은 패널로 그릴 수 있음을 보이는 데 쓴다(분기 비교, 도장 미리보기 용도).
	 */
	static final class SnapshotView implements BoardView
	{
		private final BoardSnapshot snapshot;
		SnapshotView(BoardSnapshot snapshot) { this.snapshot = snapshot; }
		public int size() { return snapshot.size(); }
		public long generation() { return 0; }
		public boolean isAlive(int row, int col) { return snapshot.liveCells().contains(new Coord(row, col)); }
		public BoardSnapshot snapshot() { return snapshot; }
		public void addListener(BoardListener listener) {}
		public void removeListener(BoardListener listener) {}
	}

	private static UniversePanel panelFor(BoardView board)
	{
		UniversePanel panel = new UniversePanel(board, CELL);
		panel.setSize(board.size() * CELL, board.size() * CELL);
		return panel;
	}

	/** 패널을 그려서 빨간 셀의 좌표를 읽는다. */
	private static Set<Coord> livePixels(UniversePanel panel, int size)
	{
		BufferedImage image = new BufferedImage(size * CELL, size * CELL, BufferedImage.TYPE_INT_RGB);
		Graphics g = image.getGraphics();
		panel.paint(g);
		g.dispose();
		Set<Coord> live = new HashSet<>();
		for (int r = 0; r < size; ++r)
			for (int c = 0; c < size; ++c)
				if ((image.getRGB(c * CELL + CELL / 2, r * CELL + CELL / 2) & 0xffffff) == 0xff0000)
					live.add(new Coord(r, c));
		return live;
	}

	/** 목적: 화면이 모델 상태를 그대로 그리는지(원본의 화면 표시 유지) 확인.
	 *  방법: 모델에 글라이더를 놓고 패널을 그린 뒤, 한 세대 진행하고 다시 그린다.
	 *  기대: 그린 빨간 셀이 매번 모델의 산 셀과 같다.
	 */
	@Test @Tag("existing")
	@DisplayName("[기존 기능] 패널은 모델의 산 셀을 그대로 그린다")
	void drawsTheModel()
	{
		LifeModel model = LifeModel.builder().build();
		model.apply(BoardEdit.set(List.of(new Coord(1, 2), new Coord(2, 3), new Coord(3, 1),
										  new Coord(3, 2), new Coord(3, 3)), true));
		UniversePanel panel = panelFor(model);

		assertEquals(model.snapshot().liveCells(), livePixels(panel, 64));
		model.step();
		assertEquals(model.snapshot().liveCells(), livePixels(panel, 64));
	}

	/** 목적: 원본의 "셀 클릭" 입력이 유지되고, 화면은 좌표만 알릴 뿐 모델을
	 *  직접 바꾸지 않음(입력 → 컨트롤러 → 모델)을 확인.
	 *  방법: 패널의 (행 5, 열 9) 셀 위치에 마우스 누름 이벤트를 보낸다.
	 *  기대: 클릭 리스너가 (5, 9)를 받는다.
	 */
	@Test @Tag("existing")
	@DisplayName("[기존 기능] 마우스 클릭을 (행, 열) 좌표로 바꿔 알린다")
	void reportsClicksAsCells()
	{
		UniversePanel panel = panelFor(LifeModel.builder().build());
		List<Coord> clicked = new ArrayList<>();
		panel.setCellClickListener((row, col) -> clicked.add(new Coord(row, col)));

		panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_PRESSED, 0, 0,
										   9 * CELL + 3, 5 * CELL + 3, 1, false, MouseEvent.BUTTON1));
		assertEquals(List.of(new Coord(5, 9)), clicked);
	}

	/** 목적: BoardView를 분리한 이점을 보인다. 렌더러가 LifeModel이 아니라
	 *  BoardView에 의존하므로, 저장해 둔 과거 상태도 같은 패널로 그릴 수 있다.
	 *  방법: 모델의 스냅샷을 찍고 모델을 3세대 더 진행한 뒤, 스냅샷을 감싼
	 *  BoardView를 새 패널에 그린다.
	 *  기대: 패널에는 현재 모델이 아니라 스냅샷 시점의 셀이 그려진다.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] 같은 패널로 저장된 스냅샷도 그린다 (BoardView 분리의 이점)")
	void drawsAStoredSnapshotThroughBoardView()
	{
		LifeModel model = LifeModel.builder().build();
		model.apply(BoardEdit.set(List.of(new Coord(10, 9), new Coord(10, 10), new Coord(10, 11)), true));
		BoardSnapshot past = model.snapshot();
		model.step();
		model.step();
		model.step();

		UniversePanel panel = panelFor(new SnapshotView(past));
		assertEquals(past.liveCells(), livePixels(panel, 64));
		assertNotEquals(model.snapshot().liveCells(), livePixels(panel, 64));
	}

	/** 목적: 보드 크기를 바꿔도 화면이 그 크기에 맞춰 그리는지 확인.
	 *  방법: 16×16 보드의 네 모서리 셀을 켜고 그린다.
	 *  기대: 네 모서리만 빨갛다.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] 다른 크기의 보드(16×16)도 맞게 그린다")
	void drawsOtherBoardSizes()
	{
		LifeModel model = LifeModel.builder().size(16).build();
		Set<Coord> corners = Set.of(new Coord(0, 0), new Coord(0, 15), new Coord(15, 0), new Coord(15, 15));
		model.apply(BoardEdit.set(corners, true));
		assertEquals(corners, livePixels(panelFor(model), 16));
	}

	/** 그리기 결과 이미지를 그대로 돌려준다. */
	private static BufferedImage render(UniversePanel panel, int size)
	{
		BufferedImage image = new BufferedImage(size * CELL, size * CELL, BufferedImage.TYPE_INT_RGB);
		Graphics g = image.getGraphics();
		panel.paint(g);
		g.dispose();
		return image;
	}

	private static boolean isBlue(BufferedImage image, int x, int y)
	{
		return (image.getRGB(x, y) & 0xffffff) == 0x0000ff;
	}

	/** 목적: 원본 Neighborhood.redraw가 그리던 "활성 블록 파란 테두리"가
	 *  유지되는지 확인. 원본은 바뀐 셀이 있는 블록(amActive)에 파란 사각형을
	 *  그리고, 조용해진 블록은 한 번 더 그려 테두리를 지웠다.
	 *  방법: 블록 (2, 3) 안에 혼자 있는 셀을 켜고(다음 세대에 죽음), 편집 직후,
	 *  1세대 뒤(셀이 죽음), 2세대 뒤(아무 변화 없음)에 블록 안쪽 테두리 픽셀을 본다.
	 *  기대: 편집 직후와 1세대 뒤에는 그 블록과 보드 둘레가 파랗고, 다른 블록은
	 *  파랗지 않다. 2세대 뒤에는 파란 테두리가 모두 지워진다.
	 */
	@Test @Tag("existing")
	@DisplayName("[기존 기능] 바뀐 블록에 파란 테두리를 그리고, 조용해지면 지운다")
	void outlinesActiveBlocks()
	{
		LifeModel model = LifeModel.builder().build();
		UniversePanel panel = panelFor(model);
		render(panel, 64);

		int blockX = 3 * 8 * CELL, blockY = 2 * 8 * CELL;      // block (2, 3)
		int quietX = 5 * 8 * CELL, quietY = 5 * 8 * CELL;      // block (5, 5)
		model.toggle(20, 28);

		BufferedImage edited = render(panel, 64);
		assertTrue(isBlue(edited, blockX + 1, blockY + 20), "편집한 블록");
		assertFalse(isBlue(edited, quietX + 1, quietY + 20), "다른 블록");
		assertTrue(isBlue(edited, 1, 100), "보드 둘레");

		model.step();                                           // the cell dies
		BufferedImage changed = render(panel, 64);
		assertTrue(isBlue(changed, blockX + 1, blockY + 20));
		assertTrue(isBlue(changed, 1, 100));

		model.step();                                           // nothing changes
		BufferedImage quiet = render(panel, 64);
		assertFalse(isBlue(quiet, blockX + 1, blockY + 20));
		assertFalse(isBlue(quiet, 1, 100));
	}

	/** 목적: 바뀐 곳만 다시 그려도 화면이 전체를 새로 그린 것과 한 픽셀도
	 *  다르지 않은지 확인 (부분 그리기가 지우거나 빠뜨리는 곳이 없음).
	 *  방법: 무작위 보드(셀 20%)에서 40세대 진행하며 중간에 클릭 편집도 섞는다.
	 *  매 세대 그린 이미지와, 셀 크기를 바꿔 강제로 전체를 다시 그린 이미지를 비교한다.
	 *  기대: 모든 픽셀이 같다.
	 */
	@Test @Tag("existing")
	@DisplayName("[기존 기능] 바뀐 곳만 그린 화면은 전체를 다시 그린 화면과 픽셀 단위로 같다")
	void partialRedrawMatchesFullRedraw()
	{
		LifeModel model = LifeModel.builder().build();
		Random random = new Random(7);
		Set<Coord> live = new HashSet<>();
		for (int r = 0; r < 64; ++r)
			for (int c = 0; c < 64; ++c)
				if (random.nextDouble() < 0.2) live.add(new Coord(r, c));
		model.restore(new BoardSnapshot(64, live));
		UniversePanel panel = panelFor(model);
		render(panel, 64);

		for (int generation = 0; generation < 40; ++generation)
		{
			if (generation % 10 == 5)
				model.toggle(random.nextInt(64), random.nextInt(64));
			model.step();
			BufferedImage partial = render(panel, 64);

			panel.setSize(64 * CELL * 2, 64 * CELL * 2);          // other cell size: full redraw
			render(panel, 64);
			panel.setSize(64 * CELL, 64 * CELL);
			BufferedImage full = render(panel, 64);

			for (int y = 0; y < 64 * CELL; ++y)
				for (int x = 0; x < 64 * CELL; ++x)
					assertEquals(full.getRGB(x, y), partial.getRGB(x, y),
								 "generation " + generation + " pixel (" + x + ", " + y + ")");
		}
	}

	/** 셀을 읽은 횟수를 세는 BoardView. 화면이 보드를 얼마나 읽는지 본다. */
	static final class CountingView implements BoardView
	{
		private final LifeModel model;
		int reads = 0;
		CountingView(LifeModel model) { this.model = model; }
		public int size() { return model.size(); }
		public long generation() { return model.generation(); }
		public boolean isAlive(int row, int col) { ++reads; return model.isAlive(row, col); }
		public BoardSnapshot snapshot() { return model.snapshot(); }
		public void addListener(BoardListener listener) { model.addListener(listener); }
		public void removeListener(BoardListener listener) { model.removeListener(listener); }
	}

	/** 목적: 원본의 그리기 최적화("바뀐 블록만 다시 그리기")가 MVC로 나눈 뒤에도
	 *  유지되는지 확인. 원본은 redraw가 amActive인 블록만 그렸다.
	 *  방법: 256×256 보드에 블링커 하나를 두고 한 번 전체를 그린 뒤, 한 세대
	 *  진행하고 다시 그릴 때 화면이 읽은 셀 수를 센다.
	 *  기대: 처음 전체 그리기는 65,536셀을 읽고, 한 세대 뒤에는 바뀐 셀 4개만 읽는다.
	 */
	@Test @Tag("existing")
	@DisplayName("[기존 기능] 한 세대 뒤에는 바뀐 셀만 다시 그린다 (256×256 블링커: 65,536셀 대신 4셀)")
	void redrawsOnlyWhatChanged()
	{
		LifeModel model = LifeModel.builder().size(256).build();
		model.apply(BoardEdit.set(List.of(new Coord(100, 99), new Coord(100, 100), new Coord(100, 101)), true));
		CountingView view = new CountingView(model);
		UniversePanel panel = panelFor(view);

		render(panel, 256);
		assertEquals(256 * 256, view.reads);

		view.reads = 0;
		model.step();
		render(panel, 256);
		assertEquals(4, view.reads);
	}
}
