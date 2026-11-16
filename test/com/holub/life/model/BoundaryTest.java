package com.holub.life.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.*;

/** 경계(Boundary, Strategy)와 보드 크기 지정을 확인한다. 원본은 경계가
 *  죽은 셀로, 크기가 64로 고정되어 있었다(P6, N8).
 */
@DisplayName("경계 Boundary와 보드 크기")
class BoundaryTest
{
	private static Coord at(int row, int col) { return new Coord(row, col); }

	private static void assertMatchesBruteForce(LifeModel model, Set<Integer> expected, int generations)
	{
		boolean torus = model.boundary() == Boundary.TORUS;
		Boards.load(model, expected);
		for (int generation = 1; generation <= generations; ++generation)
		{
			model.step();
			expected = BruteForce.conway(expected, model.size(), torus);
			assertEquals(expected, Boards.read(model), "generation " + generation);
		}
	}

	/** 목적: 아무것도 지정하지 않으면 원본과 같은 죽은 경계인지 확인.
	 *  방법: 기본 모델의 경계를 묻는다.
	 *  기대: DEAD.
	 */
	@Test @Tag("existing")
	@DisplayName("[기존 기능] 기본 경계는 원본과 같은 죽은 경계(DEAD)다")
	void deadIsTheDefault()
	{
		assertEquals(Boundary.DEAD, LifeModel.builder().build().boundary());
	}

	/** 목적: 새 기능 토러스(위아래·좌우가 이어진 보드)가 정확한지 보인다.
	 *  방법: 무작위 보드 10개를 100세대씩, 좌표를 감싸는 정답과 비교한다.
	 *  기대: 모두 일치한다.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] 토러스: 무작위 보드 10개 × 100세대가 정답과 같다")
	void torusRandomBoardsMatchBruteForce()
	{
		for (long seed = 1; seed <= 10; ++seed)
			assertMatchesBruteForce(LifeModel.builder().boundary(Boundary.TORUS).build(),
									BruteForce.random(64, 0.3, seed), 100);
	}

	/** 목적: 토러스의 가장 까다로운 경우(대각선으로 모서리를 넘어감) 확인.
	 *  모서리 셀의 대각선 이웃은 반대쪽 모서리여야 한다.
	 *  방법: 오른쪽 아래 모서리 근처에서 남동쪽으로 가는 글라이더를 256세대 진행한다.
	 *  기대: 매 세대 정답과 같다(모서리를 여러 번 통과).
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] 토러스: 글라이더가 대각선으로 모서리를 넘어 반대편에서 나온다")
	void torusGliderCrossesTheCornerDiagonally()
	{
		assertMatchesBruteForce(LifeModel.builder().boundary(Boundary.TORUS).build(),
								BruteForce.pattern(64, 60, 60, BruteForce.GLIDER), 256);
	}

	/** 목적: 새 기능 보드 크기 지정이 정확한지 확인.
	 *  방법: 96×96(죽은 경계)과 8×8(토러스, 블록 하나짜리 보드)을 40세대 진행한다.
	 *  기대: 정답과 같다.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] 96×96, 8×8 보드도 정확히 계산된다")
	void otherSizesWork()
	{
		assertMatchesBruteForce(LifeModel.builder().size(96).build(), BruteForce.random(96, 0.3, 3), 40);
		assertMatchesBruteForce(LifeModel.builder().size(8).boundary(Boundary.TORUS).build(),
								BruteForce.random(8, 0.4, 5), 40);
	}

	/** 목적: 블록(8칸) 구조로 만들 수 없는 크기를 막는지 확인.
	 *  방법: 0, 음수, 8의 배수가 아닌 크기로 builder를 설정한다.
	 *  기대: 모두 예외.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] 8의 배수가 아닌 크기는 거부된다")
	void sizeMustBeAMultipleOfTheBlockSize()
	{
		for (int bad : List.of(0, -8, 4, 12, 100))
			assertThrows(IllegalArgumentException.class, () -> LifeModel.builder().size(bad), "size " + bad);
	}

	/** 목적: N1(아무것도 안 바뀐 세대에 가장자리 표시를 지우지 않던 문제) 수정 확인.
	 *  토러스에서는 가장 바깥 블록이 자기 자신의 이웃이라, 남은 표시가 자신을
	 *  영원히 깨웠다. 원래 로직으로 되돌리면 매 세대 192셀을 계산한다.
	 *  방법: 토러스 보드 가장자리에 2×2 정지 패턴을 두고 3~10세대의 계산 셀 수를 센다.
	 *  기대: 0.
	 */
	@Test @Tag("defect-fix")
	@DisplayName("[결함 수정 N1] 토러스 가장자리의 정지 패턴은 더 이상 계산되지 않는다 (원래 로직 192셀/세대)")
	void n1_torusStillLifeOnTheBoardEdgeStopsComputing()
	{
		LifeModel model = LifeModel.builder().boundary(Boundary.TORUS).build();
		model.apply(BoardEdit.set(List.of(at(62, 30), at(62, 31), at(63, 30), at(63, 31)), true));

		model.step();
		model.step();
		for (int generation = 3; generation <= 10; ++generation)
		{
			long before = model.cellEvaluations();
			model.step();
			assertEquals(0, model.cellEvaluations() - before, "generation " + generation);
		}
	}
}
