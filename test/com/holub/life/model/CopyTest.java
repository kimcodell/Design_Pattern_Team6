package com.holub.life.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.*;

/** copy()(Prototype)로 같은 설정의 독립 보드를 만드는 새 기능을 확인한다.
 *  분기 분석과 레이스(플레이어별 보드)가 이 기능을 쓴다(N7).
 */
@DisplayName("copy() 복제 (Prototype)")
class CopyTest
{
	/** 목적: 복제본이 크기·규칙·경계·세대·셀을 모두 물려받는지 확인.
	 *  방법: 기본값이 아닌 설정(96, HighLife, 토러스)으로 두 세대 진행한 뒤 복제한다.
	 *  기대: 모든 값이 같다.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] 복제본은 크기·규칙·경계·세대·셀이 원본과 같다")
	void copyHasTheSameSettingsStateAndGeneration()
	{
		LifeModel original = LifeModel.builder().size(96).rule(Rule.parse("B36/S23"))
									  .boundary(Boundary.TORUS).build();
		Boards.load(original, BruteForce.random(96, 0.3, 11));
		original.step();
		original.step();

		LifeModel copy = original.copy();
		assertEquals(96, copy.size());
		assertEquals(original.rule(), copy.rule());
		assertEquals(Boundary.TORUS, copy.boundary());
		assertEquals(2, copy.generation());
		assertEquals(original.snapshot(), copy.snapshot());
	}

	/** 목적: 복제본이 원본과 똑같이 진화하되, 서로 영향을 주지 않는지 확인.
	 *  방법: 둘을 20세대 나란히 진행하며 비교한 뒤, 복제본만 편집한다.
	 *  기대: 진행 중에는 같고, 편집은 복제본에만 반영된다.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] 복제본은 원본과 똑같이 진화하지만 서로 독립적이다")
	void copyAndOriginalEvolveIdenticallyButIndependently()
	{
		LifeModel original = LifeModel.builder().boundary(Boundary.TORUS).build();
		Boards.load(original, BruteForce.random(64, 0.3, 12));
		original.step();
		LifeModel copy = original.copy();

		for (int i = 0; i < 20; ++i)
		{
			original.step();
			copy.step();
			assertEquals(original.snapshot(), copy.snapshot(), "step " + i);
		}

		copy.toggle(0, 0);
		assertNotEquals(original.isAlive(0, 0), copy.isAlive(0, 0), "editing the copy leaves the original alone");
	}

	/** 목적: 원본에 붙은 화면·관찰자가 복제본 변화에 반응하지 않는지 확인.
	 *  방법: 원본에 관찰자를 달고 복제본을 편집·진행한다.
	 *  기대: 관찰자는 아무 알림도 받지 않는다.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] 관찰자는 복제되지 않는다")
	void listenersAreNotCopied()
	{
		LifeModel original = LifeModel.builder().build();
		LifeModelTest.Recorder recorder = new LifeModelTest.Recorder();
		original.addListener(recorder);

		LifeModel copy = original.copy();
		copy.toggle(1, 1);
		copy.step();
		assertTrue(recorder.events.isEmpty());
	}
}
