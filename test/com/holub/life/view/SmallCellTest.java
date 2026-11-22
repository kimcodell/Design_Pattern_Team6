package com.holub.life.view;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import com.holub.ui.Colors;
import com.holub.life.model.*;

/** 셀이 1·2픽셀로 아주 작아졌을 때(큰 보드를 ScreenFit이 줄였거나 창을 작게 줄였을 때)
 *  화면이 맞게 그려지는지 확인한다. 이 크기에서는 선을 그릴 자리가 없어서, 셀이 칸 전체를
 *  칠하고 격자선·블록선·파란 테두리는 그리지 않는다.
 *  고치기 전에는 1픽셀 셀이 아예 칠해지지 않았고(빈 판), 2픽셀에서는 사라져야 할 파란
 *  테두리가 남았다. 화면을 붙인 보드를 움직이므로 EDT에서 실행한다.
 */
@DisplayName("아주 작은 셀(1·2픽셀) 그리기")
@ExtendWith(OnEventDispatchThread.class)
class SmallCellTest
{
	private static final int SIZE = 64;
	private static final int RED = 0xFF0000, BLUE = 0x0000FF;
	private static final int DEAD = Colors.LIGHT_YELLOW.getRGB() & 0xffffff;

	/** 목적: 셀을 1·2픽셀로 줄여도 산 셀과 죽은 셀이 보이는지 확인.
	 *  방법: 무작위 보드(64×64, 25%)를 1·2픽셀 셀로 그린다. 클릭 편집도 하나 넣어
	 *  파란 테두리가 생길 상황을 만든다.
	 *  기대: 모든 칸의 모든 픽셀이 산 셀이면 빨강, 죽은 셀이면 연노랑. 선이나 파란 픽셀은 없다.
	 */
	@Test @Tag("defect-fix")
	@DisplayName("[결함 수정 D3] 1·2픽셀로 줄인 셀도 산 셀은 빨강, 죽은 셀은 연노랑으로 칸 전체를 칠한다")
	void tinyCellsFillTheirSquares()
	{
		for (int px = 1; px <= 2; ++px)
		{
			LifeModel model = LifeModel.builder().size(SIZE).build();
			model.restore(randomBoard(new Random(11)));
			UniversePanel panel = panel(model, px);
			model.toggle(20, 20);

			BufferedImage image = render(panel);
			for (int row = 0; row < SIZE; ++row)
				for (int col = 0; col < SIZE; ++col)
				{	int expected = model.isAlive(row, col) ? RED : DEAD;
					for (int y = row * px; y < (row + 1) * px; ++y)
						for (int x = col * px; x < (col + 1) * px; ++x)
							assertEquals(expected, image.getRGB(x, y) & 0xffffff,
										 px + "px, cell (" + row + ", " + col + ")");
				}
		}
	}

	/** 목적: 1·2픽셀 셀에서 사라져야 할 파란 테두리가 남던 결함이 고쳐졌는지 확인.
	 *  방법: 빈 보드에서 셀 하나를 클릭해 켜고(블록이 활성), 한 세대 뒤 그 셀이 죽고,
	 *  한 세대 더 지나 블록이 조용해지게 한다. 단계마다 화면을 그린다.
	 *  기대: 어느 단계에서도 파란 픽셀이 없고, 마지막 화면은 셀이 모두 연노랑이다.
	 */
	@Test @Tag("defect-fix")
	@DisplayName("[결함 수정 D3] 1·2픽셀 셀에서 블록이 조용해져도 파란 테두리가 남지 않는다")
	void noStaleBlueBorderAtTwoPixels()
	{
		for (int px = 1; px <= 2; ++px)
		{
			LifeModel model = LifeModel.builder().size(SIZE).build();
			UniversePanel panel = panel(model, px);
			model.toggle(10, 10);
			assertNoBlue(render(panel), px + "px, after click");
			model.step();
			assertNoBlue(render(panel), px + "px, after the cell died");
			model.step();
			BufferedImage quiet = render(panel);
			assertNoBlue(quiet, px + "px, after the block went quiet");
			for (int y = 0; y < SIZE * px; ++y)
				for (int x = 0; x < SIZE * px; ++x)
					assertEquals(DEAD, quiet.getRGB(x, y) & 0xffffff, px + "px, pixel (" + x + ", " + y + ")");
		}
	}

	/** 목적: 바뀐 곳만 다시 그리는 최적화가 작은 셀에서도 틀린 그림을 만들지 않는지 확인.
	 *  방법: 같은 보드에 패널 두 개를 단다. 하나는 평소처럼 바뀐 곳만 그리고, 다른 하나는
	 *  매 세대 색칠 방식을 다시 지정해 전체를 새로 그리게 한다. 1·2픽셀 셀, Classic과
	 *  By age 각각에서 무작위 보드를 40세대 진행하며 중간에 클릭을 섞고 매번 비교한다.
	 *  기대: 모든 픽셀이 같다.
	 */
	@Test @Tag("defect-fix")
	@DisplayName("[결함 수정 D3] 1·2픽셀 셀에서도 바뀐 곳만 그린 화면이 전체를 새로 그린 화면과 픽셀 단위로 같다")
	void partialRedrawMatchesFullRedrawAtTinyCells()
	{
		for (int px = 1; px <= 2; ++px)
			for (boolean byAge : new boolean[] { false, true })
			{
				LifeModel model = LifeModel.builder().size(SIZE).build();
				Random random = new Random(5);
				model.restore(randomBoard(random));
				UniversePanel partial = panel(model, px);
				UniversePanel full    = panel(model, px);
				partial.setColoring(coloring(byAge));
				render(partial);

				for (int generation = 1; generation <= 40; ++generation)
				{
					if (generation % 7 == 3)
						model.toggle(random.nextInt(SIZE), random.nextInt(SIZE));
					model.step();
					full.setColoring(coloring(byAge));      // forces a full redraw
					BufferedImage expected = render(full), actual = render(partial);
					for (int y = 0; y < expected.getHeight(); ++y)
						for (int x = 0; x < expected.getWidth(); ++x)
							assertEquals(expected.getRGB(x, y), actual.getRGB(x, y),
										 px + "px, " + (byAge ? "by age" : "classic") + ", generation "
										 + generation + ", pixel (" + x + ", " + y + ")");
				}
			}
	}

	// ---- helpers ----

	private static CellColoring coloring(boolean byAge)
	{
		return byAge ? new AgeColoring() : new ClassicColoring();
	}

	private static BoardSnapshot randomBoard(Random random)
	{
		Set<Coord> live = new HashSet<>();
		for (int r = 0; r < SIZE; ++r)
			for (int c = 0; c < SIZE; ++c)
				if (random.nextDouble() < 0.25) live.add(new Coord(r, c));
		return new BoardSnapshot(SIZE, live);
	}

	private static UniversePanel panel(BoardView board, int px)
	{
		UniversePanel panel = new UniversePanel(ReadOnlyBoardView.of(board), px);
		panel.setSize(board.size() * px, board.size() * px);
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

	private static void assertNoBlue(BufferedImage image, String when)
	{
		for (int y = 0; y < image.getHeight(); ++y)
			for (int x = 0; x < image.getWidth(); ++x)
				assertNotEquals(BLUE, image.getRGB(x, y) & 0xffffff, when + ", pixel (" + x + ", " + y + ")");
	}
}
