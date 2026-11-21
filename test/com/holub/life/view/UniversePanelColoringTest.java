package com.holub.life.view;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import com.holub.life.model.*;

/** 화면(UniversePanel)이 셀 나이에 따라 색을 칠하는지 확인한다. 화면을 이미지에 그려
 *  셀 가운데 픽셀 색을 읽는다. 화면을 붙인 보드를 움직이므로 EDT에서 실행한다.
 */
@DisplayName("나이에 따라 진해지는 셀 색 (화면)")
@ExtendWith(OnEventDispatchThread.class)
class UniversePanelColoringTest
{
	private static final int CELL = 8;
	private static final int RED = 0xFF0000, BLUE = 0x0000FF, PALE_PINK = 0xFFB0B0;

	/** 목적: 새 기능을 넣어도 프로그램을 켰을 때 화면은 원본과 같은지 확인.
	 *  방법: 새 패널에 블록을 놓고 30세대 진행한다.
	 *  기대: 기본 색칠은 Classic이고, 오래된 블록도 빨강 그대로.
	 */
	@Test @Tag("existing")
	@DisplayName("[기존 기능] 처음에는 원본처럼 오래된 셀도 빨강이다 (기본 Classic)")
	void startsWithTheOriginalLook()
	{
		LifeModel model = LifeModel.builder().build();
		UniversePanel panel = panel(model);
		assertInstanceOf(ClassicColoring.class, panel.coloring());
		model.apply(BoardEdit.set(block(10, 10), true));
		for (int k = 0; k < 30; ++k)
			model.step();
		assertEquals(RED, colorAt(render(panel), 10, 10));
	}

	/** 목적: By age에서 오래 산 셀이 실제로 진하게 그려지는지 확인. 셀이 변하지 않아도
	 *  (바뀐 셀만 다시 그리는데도) 색이 바뀌는 나이에 다시 칠해져야 한다.
	 *  방법: By age 패널에 블록을 놓고 20세대 진행하며 매 세대 블록 네 셀의 색을 읽는다.
	 *  기대: 매 세대 AgeColoring이 그 나이에 정한 색과 같다 (1살 연분홍 → 15살 이상 거의 검정).
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] By age: 변하지 않는 블록이 세대가 지날수록 진하게 다시 칠해진다")
	void stillLifeDarkensOnScreen()
	{
		LifeModel model = LifeModel.builder().build();
		UniversePanel panel = panel(model);
		panel.setColoring(new AgeColoring());
		model.apply(BoardEdit.set(block(10, 10), true));
		assertEquals(PALE_PINK, colorAt(render(panel), 10, 10), "newborn");

		CellColoring byAge = new AgeColoring();
		for (int k = 1; k <= 20; ++k)
		{
			model.step();
			BufferedImage image = render(panel);
			int expected = byAge.live(k + 1).getRGB() & 0xffffff;
			for (Coord cell : block(10, 10))
				assertEquals(expected, colorAt(image, cell.row(), cell.col()), "after " + k + " steps, " + cell);
		}
	}

	/** 목적: 나이는 Classic일 때도 계속 세고 있어서, 중간에 By age로 바꾸면 바로 실제
	 *  나이 색으로 보이는지 확인. 반대로 Classic으로 돌아가면 다시 빨강인지도 확인.
	 *  방법: Classic으로 블록을 10세대 진행한 뒤 By age로 바꾸고, 다시 Classic으로 바꾼다.
	 *  기대: By age로 바꾸면 11살 색(#780000), Classic으로 돌아가면 빨강.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] 실행 도중 By age로 바꾸면 그동안의 실제 나이 색으로, Classic으로 돌아가면 빨강으로 보인다")
	void switchingShowsTheRealAgesAtOnce()
	{
		LifeModel model = LifeModel.builder().build();
		UniversePanel panel = panel(model);
		model.apply(BoardEdit.set(block(10, 10), true));
		for (int k = 0; k < 10; ++k)
			model.step();
		assertEquals(RED, colorAt(render(panel), 10, 10), "classic");

		panel.setColoring(new AgeColoring());
		assertEquals(0x780000, colorAt(render(panel), 10, 10), "by age, 11 generations old");

		panel.setColoring(new ClassicColoring());
		assertEquals(RED, colorAt(render(panel), 10, 10), "back to classic");
	}

	/** 목적: 바뀐 곳만 다시 그리는 최적화가 나이 색에서도 틀린 그림을 만들지 않는지 확인.
	 *  방법: 같은 보드에 By age 패널 두 개를 단다. 하나는 평소처럼 바뀐 곳만 그리고,
	 *  다른 하나는 매 세대 색칠 방식을 다시 지정해 전체를 새로 그리게 한다.
	 *  무작위 보드를 60세대 진행하며 중간에 클릭·지우기·불러오기를 섞고 매번 픽셀 단위로 비교한다.
	 *  기대: 모든 픽셀이 같다 (파란 활성 테두리 포함).
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] By age에서도 바뀐 곳만 그린 화면이 전체를 새로 그린 화면과 픽셀 단위로 같다")
	void partialRedrawMatchesFullRedraw()
	{
		LifeModel model = LifeModel.builder().build();
		Random random = new Random(3);
		Set<Coord> live = new HashSet<>();
		for (int r = 0; r < 64; ++r)
			for (int c = 0; c < 64; ++c)
				if (random.nextDouble() < 0.25) live.add(new Coord(r, c));
		BoardSnapshot start = new BoardSnapshot(64, live);
		model.restore(start);

		UniversePanel partial = panel(model);
		UniversePanel full    = panel(model);
		partial.setColoring(new AgeColoring());
		render(partial);

		for (int generation = 1; generation <= 60; ++generation)
		{
			if (generation % 9 == 4)
				model.toggle(random.nextInt(64), random.nextInt(64));
			if (generation == 30)
				model.clear();
			if (generation == 31)
				model.restore(start);
			model.step();

			full.setColoring(new AgeColoring());        // forces a full redraw
			BufferedImage expected = render(full), actual = render(partial);
			for (int y = 0; y < expected.getHeight(); ++y)
				for (int x = 0; x < expected.getWidth(); ++x)
					assertEquals(expected.getRGB(x, y), actual.getRGB(x, y),
								 "generation " + generation + " pixel (" + x + ", " + y + ")");
		}
	}

	/** 목적: 파란 테두리는 "그 블록에서 계산이 일어났다"는 원본의 표시다. 나이 때문에
	 *  셀을 다시 칠했다고 해서 테두리가 생기면 원본 의미가 깨진다.
	 *  방법: By age 패널에 블록 하나만 놓고 20세대 진행한다. 블록은 변하지 않으므로
	 *  첫 세대 뒤로는 계산할 곳이 없다 (색은 2·3·4·6·8·11·15살에 바뀜).
	 *  기대: 첫 세대 뒤로 파란 픽셀이 하나도 없다.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] 나이 때문에 다시 칠해도 파란 활성 테두리는 생기지 않는다")
	void recoloringDoesNotMarkBlocksActive()
	{
		LifeModel model = LifeModel.builder().build();
		UniversePanel panel = panel(model);
		panel.setColoring(new AgeColoring());
		model.apply(BoardEdit.set(block(10, 10), true));
		render(panel);
		for (int k = 1; k <= 20; ++k)
		{
			model.step();
			BufferedImage image = render(panel);
			for (int y = 0; y < image.getHeight(); ++y)
				for (int x = 0; x < image.getWidth(); ++x)
					assertNotEquals(BLUE, image.getRGB(x, y) & 0xffffff,
									"after " + k + " steps, pixel (" + x + ", " + y + ")");
		}
	}

	/** 목적: 잘못된 입력을 조용히 넘기지 않는지 확인.
	 *  기대: 색칠 방식으로 null을 주면 NullPointerException, 기존 색칠은 그대로.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] 색칠 방식으로 null을 줄 수 없다")
	void rejectsNullColoring()
	{
		UniversePanel panel = panel(LifeModel.builder().build());
		assertThrows(NullPointerException.class, () -> panel.setColoring(null));
		assertInstanceOf(ClassicColoring.class, panel.coloring());
	}

	// ---- helpers ----

	private static UniversePanel panel(BoardView board)
	{
		UniversePanel panel = new UniversePanel(ReadOnlyBoardView.of(board), CELL);
		panel.setSize(board.size() * CELL, board.size() * CELL);
		render(panel);                  // first full paint, as when the window opens
		return panel;
	}

	private static BufferedImage render(UniversePanel panel)
	{
		BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics g = image.getGraphics();
		panel.paint(g);
		g.dispose();
		return image;
	}

	private static int colorAt(BufferedImage image, int row, int col)
	{
		return image.getRGB(col * CELL + CELL / 2, row * CELL + CELL / 2) & 0xffffff;
	}

	private static List<Coord> block(int row, int col)
	{
		return List.of(new Coord(row, col), new Coord(row, col + 1),
					   new Coord(row + 1, col), new Coord(row + 1, col + 1));
	}
}
