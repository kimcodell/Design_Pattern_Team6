package com.holub.life.pattern;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.*;
import com.holub.life.model.Coord;

@DisplayName("패턴 모양 Shape (불변 값 객체)")
class ShapeTest
{
	/** 목적: 문자열로 모양을 적을 수 있고, 위치는 왼쪽 위 (0,0)으로 맞춰지는지.
	 *  방법: 앞뒤에 빈 줄·칸이 있는 글라이더를 읽는다.
	 *  기대: 5셀, 3×3, top·left 0.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[패턴] 문자열로 읽은 모양은 왼쪽 위가 (0,0)이다")
	void parseNormalizes()
	{
		Shape glider = Shape.parse("....", "..O.", "...O", ".OOO");
		assertEquals(5, glider.size());
		assertEquals(3, glider.height());
		assertEquals(3, glider.width());
		assertEquals(Set.of(new Coord(0, 1), new Coord(1, 2), new Coord(2, 0), new Coord(2, 1), new Coord(2, 2)),
					 glider.cells());
	}

	/** 목적: 회전·반사 8방향을 만들 때 대칭이면 중복을 없애는지.
	 *  방법: 대칭이 없는 글라이더, 완전 대칭인 블록, 2방향뿐인 블링커의 방향 수.
	 *  기대: 8, 1, 2.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[패턴] 방향 수: 글라이더 8, 블록 1, 블링커 2 (대칭 중복 제거)")
	void orientationsRemoveSymmetricDuplicates()
	{
		assertEquals(8, Shape.parse(".O.", "..O", "OOO").orientations().size());
		assertEquals(1, Shape.parse("OO", "OO").orientations().size());
		assertEquals(2, Shape.parse("OOO").orientations().size());
	}

	/** 목적: 값 객체라 같은 셀이면 같고, 연산은 원본을 바꾸지 않는지.
	 *  방법: translate 후 원본과 비교, 같은 셀로 만든 두 모양 비교.
	 *  기대: 원본 그대로, 두 모양은 equals.
	 */
	@Test @Tag("design")
	@DisplayName("[패턴] Shape는 불변이고 같은 셀이면 같다")
	void shapesAreValues()
	{
		Shape block = Shape.parse("OO", "OO");
		Shape moved = block.translate(5, 7);
		assertEquals(0, block.top());
		assertEquals(5, moved.top());
		assertEquals(block, moved.normalized());
	}
}
