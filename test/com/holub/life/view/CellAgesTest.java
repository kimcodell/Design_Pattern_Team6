package com.holub.life.view;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.*;
import com.holub.life.model.*;

/** 셀 나이 추적 CellAges가 보드 알림만으로 나이를 정확히 세는지 확인한다.
 *  화면 없이 모델과 CellAges만 쓴다(화면이 하던 알림 전달을 테스트가 대신 한다).
 */
@DisplayName("셀 나이 추적 CellAges")
class CellAgesTest
{
	private static final long[] SHADE_STEPS = { 2, 4, 8, 16 };

	/** 목적: 나이의 기준점 확인.
	 *  기대: 방금 클릭해 살린 셀은 1살, 죽은 셀은 0살.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] 방금 클릭한 셀은 1살, 죽은 셀은 0살이다")
	void clickedCellIsNewborn()
	{
		LifeModel model = LifeModel.builder().build();
		CellAges ages = track(model);
		model.toggle(5, 5);
		assertEquals(1, ages.age(5, 5));
		assertEquals(0, ages.age(5, 6));
	}

	/** 목적: 계속 살아 있는 셀이 세대마다 한 살씩 먹는지 확인.
	 *  방법: 변하지 않는 블록(2×2)을 놓고 20세대 진행한다.
	 *  기대: k세대 뒤 네 셀 모두 k+1살.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] 변하지 않는 블록은 세대마다 한 살씩 먹는다")
	void stillLifeAgesEveryGeneration()
	{
		LifeModel model = LifeModel.builder().build();
		CellAges ages = track(model);
		model.apply(BoardEdit.set(block(10, 10), true));
		for (int k = 1; k <= 20; ++k)
		{
			model.step();
			for (Coord cell : block(10, 10))
				assertEquals(k + 1, ages.age(cell.row(), cell.col()), "after " + k + " steps, " + cell);
		}
	}

	/** 목적: 죽었다 다시 태어난 셀은 나이가 처음부터 다시 시작하는지 확인.
	 *  방법: 블링커(가로 3칸)를 진행한다. 가운데는 계속 살고, 양 끝은 세대마다 죽고 새로 태어난다.
	 *  기대: 가운데는 계속 나이를 먹고, 새로 태어난 양 끝은 항상 1살.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] 블링커: 가운데는 계속 나이를 먹고, 새로 태어난 끝 셀은 1살부터 다시 센다")
	void rebornCellsStartOver()
	{
		LifeModel model = LifeModel.builder().build();
		CellAges ages = track(model);
		model.apply(BoardEdit.set(List.of(new Coord(10, 9), new Coord(10, 10), new Coord(10, 11)), true));
		for (int k = 1; k <= 6; ++k)
		{
			model.step();
			assertEquals(k + 1, ages.age(10, 10), "center after " + k);
			for (int r = 9; r <= 11; ++r)
				for (int c = 9; c <= 11; ++c)
					if ((r != 10 || c != 10) && model.isAlive(r, c))
						assertEquals(1, ages.age(r, c), "end cell (" + r + ", " + c + ") after " + k);
		}
	}

	/** 목적: 클릭으로 끈 셀은 나이가 사라지고, 다시 켜면 1살부터인지 확인.
	 *  기대: 끄면 0살, 다시 켜면 1살.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] 클릭으로 끄면 0살, 다시 켜면 1살부터 센다")
	void toggledOffAndOnStartsOver()
	{
		LifeModel model = LifeModel.builder().build();
		CellAges ages = track(model);
		model.apply(BoardEdit.set(block(10, 10), true));
		for (int k = 0; k < 5; ++k)
			model.step();
		assertEquals(6, ages.age(10, 10));
		model.toggle(10, 10);
		assertEquals(0, ages.age(10, 10));
		model.toggle(10, 10);
		assertEquals(1, ages.age(10, 10));
	}

	/** 목적: 보드 전체가 바뀌면(지우기, 불러오기) 나이를 새로 시작하는지 확인.
	 *  저장 파일에는 나이가 없으므로 불러온 셀은 모두 갓 태어난 셀로 본다.
	 *  기대: 지우면 모두 0살, 불러오면 산 셀이 모두 1살이고 한 세대 뒤 2살.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] 지우면 모두 0살, 불러온 셀은 모두 1살부터 센다")
	void clearAndRestoreStartOver()
	{
		LifeModel model = LifeModel.builder().build();
		CellAges ages = track(model);
		model.apply(BoardEdit.set(block(10, 10), true));
		BoardSnapshot saved = model.snapshot();
		for (int k = 0; k < 5; ++k)
			model.step();

		model.clear();
		assertEquals(0, ages.age(10, 10));

		model.restore(saved);
		for (Coord cell : block(10, 10))
			assertEquals(1, ages.age(cell.row(), cell.col()), "restored " + cell);
		model.step();
		assertEquals(2, ages.age(10, 10));
	}

	/** 목적: 화면이 색을 바꿔 칠해야 할 셀(색이 바뀌는 나이에 막 도달한 셀)을
	 *  정확히 골라내는지 확인. 이게 틀리면 화면에 옛 색이 남는다.
	 *  방법: 블록을 놓고 20세대 진행하며 세대마다 reached({2, 4, 8, 16})를 확인한다.
	 *  기대: 블록이 2·4·8·16살이 되는 세대에만 블록 네 셀이 나오고, 다른 세대에는 비어 있다.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] 색이 바뀌는 나이(2·4·8·16살)에 막 도달한 셀만 골라낸다")
	void findsCellsThatJustReachedAnAge()
	{
		LifeModel model = LifeModel.builder().build();
		CellAges ages = track(model);
		model.apply(BoardEdit.set(block(10, 10), true));
		List<Long> agesWhenReached = new ArrayList<>();
		for (int k = 1; k <= 20; ++k)
		{
			model.step();
			Set<Coord> reached = ages.reached(SHADE_STEPS);
			if (!reached.isEmpty())
			{	assertEquals(new HashSet<>(block(10, 10)), reached, "after " + k);
				agesWhenReached.add(ages.age(10, 10));
			}
		}
		assertEquals(List.of(2L, 4L, 8L, 16L), agesWhenReached);
	}

	/** 목적: 복잡한 보드에서도 나이가 정확한지, 단순한 방식과 비교해 확인.
	 *  방법: 무작위 보드(64×64, 25%)를 150세대 진행한다. 비교 기준은 매 세대 모든 칸을 읽어
	 *  "살아 있으면 전 세대 나이 + 1, 죽었으면 0"으로 센 배열. 중간에 클릭도 섞는다.
	 *  기대: 매 세대 모든 칸의 나이가 기준과 같다.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] 무작위 보드 150세대 동안 모든 칸의 나이가 단순 계산과 같다")
	void matchesBruteForceOnARandomBoard()
	{
		LifeModel model = LifeModel.builder().build();
		Random random = new Random(7);
		Set<Coord> live = new HashSet<>();
		for (int r = 0; r < 64; ++r)
			for (int c = 0; c < 64; ++c)
				if (random.nextDouble() < 0.25) live.add(new Coord(r, c));
		model.restore(new BoardSnapshot(64, live));
		CellAges ages = track(model);

		long[][] expected = new long[64][64];
		for (Coord cell : live)
			expected[cell.row()][cell.col()] = 1;

		for (int generation = 1; generation <= 150; ++generation)
		{
			model.step();
			for (int r = 0; r < 64; ++r)
				for (int c = 0; c < 64; ++c)
					expected[r][c] = model.isAlive(r, c) ? expected[r][c] + 1 : 0;
			if (generation % 10 == 5)
			{	int r = random.nextInt(64), c = random.nextInt(64);
				model.toggle(r, c);
				expected[r][c] = model.isAlive(r, c) ? 1 : 0;
			}
			for (int r = 0; r < 64; ++r)
				for (int c = 0; c < 64; ++c)
					assertEquals(expected[r][c], ages.age(r, c),
								 "generation " + generation + ", cell (" + r + ", " + c + ")");
		}
	}

	/** 목적: 움직이는 패턴은 왜 오래돼도 밝게 남는지 확인. 글라이더는 판 위를 미끄러지는
	 *  것이 아니라, 뒤쪽 셀이 죽고 앞쪽에 새 셀이 태어나며 나아간다.
	 *  방법: 글라이더를 40세대 진행하며 다섯 셀의 나이를 본다.
	 *  기대: 어느 셀도 4살을 넘지 않고, 3세대째부터는 늘 1·1·2·3·4살이다 (매 세대 둘은 갓 태어난 셀).
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] 글라이더는 계속 새로 태어나며 움직여서 어느 셀도 4살을 넘지 않는다")
	void gliderCellsStayYoung()
	{
		LifeModel model = LifeModel.builder().build();
		CellAges ages = track(model);
		model.apply(BoardEdit.set(List.of(new Coord(1, 2), new Coord(2, 3), new Coord(3, 1),
										  new Coord(3, 2), new Coord(3, 3)), true));
		for (int k = 1; k <= 40; ++k)
		{
			model.step();
			List<Long> glider = new ArrayList<>();
			for (Coord cell : model.snapshot().liveCells())
				glider.add(ages.age(cell.row(), cell.col()));
			Collections.sort(glider);
			assertEquals(5, glider.size(), "after " + k);
			if (k >= 3)
				assertEquals(List.of(1L, 1L, 2L, 3L, 4L), glider, "after " + k);
		}
	}

	// ---- helpers ----

	/** Feeds the board's notifications to a CellAges, as UniversePanel does. */
	private static CellAges track(LifeModel model)
	{
		CellAges ages = new CellAges(model);
		model.addListener(new BoardListener()
		{
			@Override public void stepped(long generation, Set<Coord> changed) { ages.stepped(generation, changed); }
			@Override public void edited(BoardEdit applied) { ages.edited(applied); }
			@Override public void reset() { ages.reset(model); }
		});
		return ages;
	}

	private static List<Coord> block(int row, int col)
	{
		return List.of(new Coord(row, col), new Coord(row, col + 1),
					   new Coord(row + 1, col), new Coord(row + 1, col + 1));
	}
}
