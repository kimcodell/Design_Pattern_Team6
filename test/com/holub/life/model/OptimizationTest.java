package com.holub.life.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.*;

/** 원본의 핵심 최적화(조용한 블록은 계산하지 않음)가 실제로 동작하는지
 *  "계산한 셀 수"로 확인한다. 셀 수는 모델 안의 package-private 카운터로
 *  센다(테스트가 같은 패키지에 있어 public API를 늘리지 않고 볼 수 있음).
 */
@DisplayName("블록 건너뛰기 최적화")
class OptimizationTest
{
	private static Coord at(int row, int col) { return new Coord(row, col); }

	private static long evaluationsForOneStep(LifeModel model)
	{
		long before = model.cellEvaluations();
		model.step();
		return model.cellEvaluations() - before;
	}

	/** 목적: P1(transition()이 "바뀌었다" 대신 "안정"을 반환해 모든 블록이
	 *  매번 깨어나던 결함) 수정 확인. 원본은 틱마다 4,096셀을 계산했다.
	 *  방법: 한 블록 안쪽에 블링커를 두고 10세대 동안 세대별 계산 셀 수를 센다.
	 *  기대: 매 세대 정확히 64셀(블록 하나).
	 */
	@Test @Tag("defect-fix")
	@DisplayName("[결함 수정 P1] 블링커 하나는 매 세대 한 블록(64셀)만 계산한다 (원본 4,096셀)")
	void p1_blinkerInsideOneBlockCostsOneBlockPerGeneration()
	{
		LifeModel model = LifeModel.builder().build();
		model.apply(BoardEdit.set(List.of(at(19, 18), at(19, 19), at(19, 20)), true));
		for (int generation = 1; generation <= 10; ++generation)
			assertEquals(64, evaluationsForOneStep(model), "generation " + generation);
	}

	/** 목적: 조용한 블록을 transition에서 건너뛰면서도, 예전 변화로 남은
	 *  가장자리 표시는 한 번 지워지는지 확인한다. 지워지지 않으면 이웃 블록을
	 *  매 세대 깨운다.
	 *  방법: 블록 모서리에 2×2 정지 패턴, 멀리 블링커를 두고 세대별 계산 셀 수를 센다.
	 *  기대: 둘째 세대부터 블링커 블록의 64셀만 계산한다.
	 */
	@Test @Tag("defect-fix")
	@DisplayName("[결함 수정 N1] 블록 모서리의 정지 패턴은 이웃 블록을 계속 깨우지 않는다")
	void stillLifeOnABlockEdgeStopsWakingItsNeighbors()
	{
		LifeModel model = LifeModel.builder().build();
		model.apply(BoardEdit.set(List.of(at(6, 6), at(6, 7), at(7, 6), at(7, 7)), true));
		model.apply(BoardEdit.set(List.of(at(43, 42), at(43, 43), at(43, 44)), true));

		evaluationsForOneStep(model);
		for (int generation = 2; generation <= 10; ++generation)
			assertEquals(64, evaluationsForOneStep(model),
						 "only the blinker's block should be computed, generation " + generation);
	}

	/** 목적: 원본의 "아무 일도 없으면 계산하지 않는다"는 동작 유지 확인.
	 *  방법: 빈 보드와, 홀로 켜져 한 세대 만에 죽는 셀이 있는 보드를 진행한다.
	 *  기대: 할 일이 없는 세대의 계산 셀 수는 0.
	 */
	@Test @Tag("existing")
	@DisplayName("[기존 기능] 빈 보드는 한 셀도 계산하지 않는다")
	void emptyBoardComputesNothing()
	{
		LifeModel model = LifeModel.builder().build();
		assertEquals(0, evaluationsForOneStep(model));
		model.toggle(5, 5);
		evaluationsForOneStep(model);
		evaluationsForOneStep(model);
		assertEquals(0, evaluationsForOneStep(model));
	}
}
