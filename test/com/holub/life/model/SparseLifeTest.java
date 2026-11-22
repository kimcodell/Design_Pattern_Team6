package com.holub.life.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.*;

/** A 수정안: 무한 평면 계산(SparseLife), 읽기 전용 BoardView의 규칙·경계,
 *  좌표 수준 경계 Boundary.locate, 리스너가 없을 때의 step().
 */
@DisplayName("A 수정: SparseLife · BoardView 설정 · Boundary.locate · step()")
class SparseLifeTest
{
	private static Coord at(int row, int col) { return new Coord(row, col); }

	private static Set<Coord> cells(int rowOffset, int colOffset, int[][] pattern)
	{
		Set<Coord> set = new HashSet<>();
		for (int[] c : pattern) set.add(at(c[0] + rowOffset, c[1] + colOffset));
		return set;
	}

	/** 목적: 경계 없는 계산이 정확한지 확인(A1).
	 *  방법: 가로 블링커를 1세대, 2세대 진행한다.
	 *  기대: 1세대 뒤 세로, 2세대 뒤 처음 모양.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[A1] 블링커가 2세대마다 제자리로 돌아온다")
	void blinkerOscillates()
	{
		Set<Coord> horizontal = Set.of(at(0, 0), at(0, 1), at(0, 2));
		assertEquals(Set.of(at(-1, 1), at(0, 1), at(1, 1)), SparseLife.step(horizontal, Rule.CONWAY));
		assertEquals(horizontal, SparseLife.step(horizontal, Rule.CONWAY, 2));
	}

	/** 목적: 음수 좌표로 넘어가도 계산되는지(보드 가장자리가 없음) 확인.
	 *  방법: (0,0)에 놓은 글라이더를 반사해 북서쪽으로 4세대 보낸다.
	 *  기대: 같은 모양이 (-1,-1) 옮겨져 있다.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[A1] 글라이더가 음수 좌표로 넘어가며 4세대마다 대각선 한 칸 이동한다")
	void gliderCrossesIntoNegativeCoordinates()
	{
		int[][] northWest = { {0, 0}, {0, 1}, {0, 2}, {1, 0}, {2, 1} };
		assertEquals(cells(-1, -1, northWest), SparseLife.step(cells(0, 0, northWest), Rule.CONWAY, 4));
	}

	/** 목적: 보드 계산(Composite)과 같은 답을 내는지 확인.
	 *  방법: 가장자리에서 떨어진 무작위 덩어리를 30세대 동안 두 방식으로 진행한다.
	 *  기대: 보드에 닿기 전까지 매 세대 같다.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[A1] 무작위 덩어리 30세대가 LifeModel 결과와 같다")
	void agreesWithTheBoard()
	{
		Random random = new Random(7);
		Set<Coord> live = new HashSet<>();
		for (int r = 0; r < 10; ++r)
			for (int c = 0; c < 10; ++c)
				if (random.nextBoolean()) live.add(at(r + 59, c + 59));     // middle of a 128 board
		LifeModel model = LifeModel.builder().size(128).build();
		model.restore(new BoardSnapshot(128, live));
		for (int g = 1; g <= 30; ++g)
		{
			live = SparseLife.step(live, Rule.CONWAY);
			model.step();
			assertEquals(live, model.snapshot().liveCells(), "generation " + g);
		}
	}

	/** 목적: 읽기만 하는 코드(분석기)가 BoardView만으로 규칙과 경계를 알 수 있는지(A2).
	 *  방법: HighLife·TORUS 모델을 BoardView 타입으로 받아 묻는다.
	 *  기대: 만들 때 준 규칙과 경계.
	 */
	@Test @Tag("design")
	@DisplayName("[A2] BoardView로 규칙과 경계를 읽을 수 있다")
	void boardViewTellsRuleAndBoundary()
	{
		BoardView view = LifeModel.builder().rule(Rule.parse("B36/S23")).boundary(Boundary.TORUS).build();
		assertEquals(Rule.parse("B36/S23"), view.rule());
		assertEquals(Boundary.TORUS, view.boundary());
	}

	/** 목적: 경계를 좌표로 물을 수 있는지(A3). Cell을 드러내지 않고 바깥 코드가 경계를 쓴다.
	 *  방법: 보드 안, 밖, 모서리 너머 좌표를 두 경계에 묻는다.
	 *  기대: DEAD는 밖이면 없음, TORUS는 반대편으로 감싼 좌표.
	 */
	@Test @Tag("design")
	@DisplayName("[A3] Boundary.locate: DEAD는 보드 밖이 없고 TORUS는 반대편으로 감싼다")
	void locateFollowsTheBoundary()
	{
		assertEquals(Optional.of(at(3, 4)), Boundary.DEAD.locate(3, 4, 64));
		assertEquals(Optional.empty(), Boundary.DEAD.locate(-1, 4, 64));
		assertEquals(Optional.empty(), Boundary.DEAD.locate(3, 64, 64));
		assertEquals(Optional.of(at(63, 0)), Boundary.TORUS.locate(-1, 64, 64));
		assertEquals(Optional.of(at(1, 62)), Boundary.TORUS.locate(129, -2, 64));
	}

	/** 목적: 리스너가 없을 때 바뀐 셀 목록을 만들지 않게 바꾼 step()이(A4) 여전히
	 *  맞는 값을 돌려주는지 확인.
	 *  방법: 리스너 없는 보드에서 블링커와 블록을 진행한다.
	 *  기대: 블링커는 true, 블록만 있으면 false, 셀은 정답과 같다.
	 */
	@Test @Tag("defect-fix")
	@DisplayName("[A4] 리스너가 없어도 step()의 반환값과 결과가 맞다")
	void stepWithoutListenersStillReportsChange()
	{
		LifeModel blinker = LifeModel.builder().build();
		blinker.apply(BoardEdit.set(Set.of(at(5, 4), at(5, 5), at(5, 6)), true));
		assertTrue(blinker.step());
		assertEquals(Set.of(at(4, 5), at(5, 5), at(6, 5)), blinker.snapshot().liveCells());

		LifeModel block = LifeModel.builder().build();
		block.apply(BoardEdit.set(Set.of(at(5, 5), at(5, 6), at(6, 5), at(6, 6)), true));
		assertFalse(block.step());
	}
}
