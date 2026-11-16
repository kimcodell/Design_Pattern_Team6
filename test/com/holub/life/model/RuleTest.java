package com.holub.life.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.*;

/** 생존 규칙을 Strategy(Rule)로 분리한 결과를 확인한다. 원본은
 *  B3/S23이 final 클래스 Resident에 박혀 있었다(P5).
 */
@DisplayName("생존 규칙 Rule (Strategy)")
class RuleTest
{
	/** 목적: 기본 규칙이 원본과 같은 Conway 규칙(B3/S23)인지 확인.
	 *  방법: 대표적인 이웃 수에 대해 다음 상태를 묻고 표기를 비교한다.
	 *  기대: 죽은 셀은 3에서 태어나고, 산 셀은 2·3에서 살아남는다.
	 */
	@Test @Tag("existing")
	@DisplayName("[기존 기능] 기본 규칙은 원본과 같은 B3/S23이다")
	void conwayIsB3S23()
	{
		Rule conway = Rule.CONWAY;
		assertTrue(conway.nextState(false, 3));
		assertFalse(conway.nextState(false, 2));
		assertTrue(conway.nextState(true, 2));
		assertTrue(conway.nextState(true, 3));
		assertFalse(conway.nextState(true, 4));
		assertEquals("B3/S23", conway.toString());
		assertEquals(conway, Rule.parse(" b3/s32 "));
		assertEquals(Rule.CONWAY, LifeModel.builder().build().rule());
	}

	/** 목적: 새 기능(규칙 교체)이 실제 진화에 반영되는지 보인다.
	 *  방법: HighLife(B36/S23) 모델의 무작위 보드를 60세대 진행하며,
	 *  같은 규칙의 정답과 비교한다.
	 *  기대: 매 세대 일치한다.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] HighLife(B36/S23) 규칙으로 바꾸면 그 규칙대로 진화한다")
	void highLifeMatchesBruteForce()
	{
		int size = 64;
		LifeModel model = LifeModel.builder().rule(Rule.parse("B36/S23")).build();
		assertEquals("B36/S23", model.rule().toString());

		Set<Integer> expected = BruteForce.random(size, 0.35, 7);
		Boards.load(model, expected);
		for (int generation = 1; generation <= 60; ++generation)
		{
			model.step();
			expected = BruteForce.step(expected, size, false, Set.of(3, 6), Set.of(2, 3));
			assertEquals(expected, Boards.read(model), "generation " + generation);
		}
	}

	/** 목적: 잘못된 규칙 표기를 받아들이지 않는지 확인.
	 *  방법: 형식이 틀린 표기 여섯 가지를 parse한다.
	 *  기대: 모두 예외.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] 잘못된 B/S 표기는 거부된다")
	void malformedNotationIsRejected()
	{
		for (String bad : List.of("", "B3", "S23", "B9/S23", "B3/S2x", "23/3"))
			assertThrows(IllegalArgumentException.class, () -> Rule.parse(bad), bad);
	}
}
