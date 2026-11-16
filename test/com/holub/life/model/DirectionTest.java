package com.holub.life.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.*;

/** 블록 가장자리 표시(Direction)를 불변 값 객체로 바꾼 결과를 확인한다.
 *  원본은 가변 클래스에 "변경을 거부하는" 하위 클래스가 있어 LSP를
 *  어겼고, 값 비교(equals)도 없었다(P8, P9).
 */
@DisplayName("Direction 불변 값 객체")
class DirectionTest
{
	/** 목적: P8(equals(Object)·hashCode 없음) 수정 확인.
	 *  방법: 같은 방향을 다른 순서로 조합해 비교하고 HashSet에 넣는다.
	 *  기대: 같은 값으로 취급되고, 두 변(NORTH|WEST)과 한 모서리(NORTHWEST)는 다르다.
	 */
	@Test @Tag("defect-fix")
	@DisplayName("[결함 수정 P8] 같은 방향은 같은 값으로 비교된다")
	void equalDirectionsAreEqualValues()
	{
		Direction northWest = Direction.NORTH.plus(Direction.WEST);
		assertEquals(Direction.WEST.plus(Direction.NORTH), northWest);
		assertEquals((Object) northWest, Direction.NORTH.plus(Direction.WEST));
		assertEquals(1, new HashSet<>(List.of(northWest, Direction.WEST.plus(Direction.NORTH))).size(),
					 "usable in hash sets");
		assertNotEquals(Direction.NORTH.plus(Direction.WEST), Direction.NORTHWEST,
						"NORTH|WEST (two edges) is not NORTHWEST (one corner)");
	}

	/** 목적: P8(LSP 위반) 수정 확인. 모든 방향이 같은 방식으로(새 값을 돌려주며)
	 *  동작해야 한다.
	 *  방법: NONE에 SOUTH를 더한다.
	 *  기대: 새 값이 나오고 NONE은 그대로다.
	 */
	@Test @Tag("defect-fix")
	@DisplayName("[결함 수정 P8] plus는 새 값을 돌려주고 원래 값은 바뀌지 않는다")
	void plusLeavesTheOriginalUnchanged()
	{
		Direction edges = Direction.NONE;
		Direction more = edges.plus(Direction.SOUTH);
		assertTrue(edges.isEmpty());
		assertTrue(more.containsAll(Direction.SOUTH));
	}

	/** 목적: P8(the(NONE)이 항상 true라 이름과 뜻이 어긋남) 수정 확인.
	 *  방법: containsAll, intersects, isEmpty를 여러 조합으로 묻는다.
	 *  기대: 이름 그대로의 결과가 나온다.
	 */
	@Test @Tag("defect-fix")
	@DisplayName("[결함 수정 P8] containsAll·intersects·isEmpty가 이름 그대로 동작한다")
	void containsAllAndIntersectsSayWhatTheyMean()
	{
		Direction northEdges = Direction.NORTH.plus(Direction.NORTHEAST);
		assertTrue(northEdges.containsAll(Direction.NORTH));
		assertFalse(northEdges.containsAll(Direction.NORTH.plus(Direction.SOUTH)));
		assertTrue(northEdges.intersects(Direction.NORTH.plus(Direction.SOUTH)));
		assertFalse(northEdges.intersects(Direction.SOUTH));

		assertTrue(northEdges.containsAll(Direction.NONE));
		assertFalse(northEdges.intersects(Direction.NONE));
		assertTrue(Direction.NONE.isEmpty());
	}

	/** 목적: P9(블록이 내부의 가변 Direction을 그대로 내줘 바깥에서 바꿀 수
	 *  있던 결함) 수정 확인.
	 *  방법: 블록의 isDisruptiveTo() 결과에 방향을 더해 본다.
	 *  기대: 블록의 상태는 바뀌지 않는다.
	 */
	@Test @Tag("defect-fix")
	@DisplayName("[결함 수정 P9] 블록이 내준 가장자리 표시로 블록 상태를 바꿀 수 없다")
	void aBlockCannotLeakItsEdgeStateToCallers()
	{
		Neighborhood block = new Neighborhood(8, new Resident(Rule.CONWAY, new EvaluationCounter()));
		block.set(0, 3, true);
		Direction edges = block.isDisruptiveTo();
		assertTrue(edges.containsAll(Direction.NORTH));
		edges.plus(Direction.SOUTH);
		assertFalse(block.isDisruptiveTo().intersects(Direction.SOUTH));
	}
}
