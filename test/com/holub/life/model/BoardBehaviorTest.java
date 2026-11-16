package com.holub.life.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.*;

/** 리팩터링한 모델이 Life 규칙대로 정확히 계산하는지, 그리고 원본의
 *  계산 관련 결함이 고쳐졌는지 확인한다. 정답은 블록 최적화 없이
 *  모든 셀을 직접 계산하는 {@link BruteForce}로 만든다.
 */
@DisplayName("보드 계산 정확성")
class BoardBehaviorTest
{
	private static final int SIZE = 64;

	/** 목적: 구조를 크게 바꾼 뒤에도 원본과 같은 Life 계산이 나오는지 보인다.
	 *  방법: 무작위 보드 10개(셀 30%)를 각각 100세대 진행하며 매 세대 정답과 비교한다.
	 *  기대: 1,000번의 비교가 모두 일치한다.
	 */
	@Test @Tag("existing")
	@DisplayName("[기존 기능] 무작위 보드 10개 × 100세대가 정답과 같다")
	void randomBoardsMatchBruteForce()
	{
		for (long seed = 1; seed <= 10; ++seed)
		{
			Set<Integer> expected = BruteForce.random(SIZE, 0.3, seed);
			LifeModel model = LifeModel.builder().build();
			Boards.load(model, expected);
			for (int generation = 1; generation <= 100; ++generation)
			{
				model.step();
				expected = BruteForce.conway(expected, SIZE, false);
				assertEquals(expected, Boards.read(model), "seed " + seed + ", generation " + generation);
			}
		}
	}

	/** 목적: 클릭으로 만든 패턴이 블록 경계를 넘어 진화하는 경우(이웃 블록을
	 *  깨우는 최적화 경로)를 확인한다.
	 *  방법: 두 블록 경계에 걸친 블링커를 toggle로 만들고 6세대 진행한다.
	 *  기대: 매 세대 정답과 같다.
	 */
	@Test @Tag("existing")
	@DisplayName("[기존 기능] 클릭으로 블록 경계에 만든 블링커가 바르게 진동한다")
	void cellsToggledAcrossABlockEdgeEvolveCorrectly()
	{
		LifeModel model = LifeModel.builder().build();
		model.toggle(7, 10);
		model.toggle(8, 10);
		model.toggle(9, 10);
		Set<Integer> expected = Boards.read(model);
		for (int generation = 1; generation <= 6; ++generation)
		{
			model.step();
			expected = BruteForce.conway(expected, SIZE, false);
			assertEquals(expected, Boards.read(model), "generation " + generation);
		}
	}

	/** 목적: P-L(불러온 셀이 "안정"으로 보여 이웃 블록을 깨우지 않던 결함)이
	 *  고쳐졌음을 보인다. 원본 클래스로는 64곳 중 9곳이 틀렸다(커밋 5b50441의
	 *  특성 테스트).
	 *  방법: 글라이더를 블록 안 64가지 위치에서 불러와 각각 40세대 진행한다.
	 *  기대: 64곳 모두 정답과 같다.
	 */
	@Test @Tag("defect-fix")
	@DisplayName("[결함 수정 P-L] 불러온 글라이더가 64가지 시작 위치 모두에서 바르게 움직인다")
	void gliderLoadedAtEveryBlockOffsetMatchesBruteForce()
	{
		for (int offset = 0; offset < 64; ++offset)
		{
			Set<Integer> expected = BruteForce.pattern(SIZE, 4 + offset / 8, 4 + offset % 8, BruteForce.GLIDER);
			LifeModel model = LifeModel.builder().build();
			Boards.load(model, expected);
			for (int generation = 1; generation <= 40; ++generation)
			{
				model.step();
				expected = BruteForce.conway(expected, SIZE, false);
				assertEquals(expected, Boards.read(model), "offset " + offset + ", generation " + generation);
			}
		}
	}

	/** 목적: P10(Neighborhood.isAlive()가 항상 true) 수정 확인.
	 *  방법: 빈 블록과 셀 하나를 켠 블록에 isAlive()를 묻는다.
	 *  기대: 빈 블록은 false, 셀이 있으면 true.
	 */
	@Test @Tag("defect-fix")
	@DisplayName("[결함 수정 P10] 블록의 isAlive()는 산 셀이 있을 때만 true다")
	void p10_neighborhoodIsAliveOnlyIfACellIsAlive()
	{
		Neighborhood block = new Neighborhood(8, new Resident(Rule.CONWAY, new EvaluationCounter()));
		assertFalse(block.isAlive());
		block.set(3, 4, true);
		assertTrue(block.isAlive());
	}

	/** 목적: P12(항상 죽어 있는 DUMMY가 "곧 바뀐다"고 답함) 수정 확인.
	 *  방법: DUMMY에 다음 상태를 계산하게 한다.
	 *  기대: false(바뀌지 않음).
	 */
	@Test @Tag("defect-fix")
	@DisplayName("[결함 수정 P12] 경계 밖 DUMMY 셀은 안정 상태라고 답한다")
	void p12_dummyIsStable()
	{
		Cell d = Cell.DUMMY;
		assertFalse(d.figureNextState(d, d, d, d, d, d, d, d));
	}
}
