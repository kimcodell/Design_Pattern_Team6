package com.holub.life.view;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import java.util.*;
import org.junit.jupiter.api.*;
import com.holub.ui.Colors;

/** 셀 색칠 Strategy(ClassicColoring, AgeColoring)가 정한 색을 돌려주는지,
 *  그리고 "색이 바뀌는 나이" 목록이 실제 색 변화와 맞물리는지 확인한다.
 */
@DisplayName("셀 색칠 방식 CellColoring (Strategy)")
class CellColoringTest
{
	/** 목적: 기본 색칠이 원본 화면 색과 같은지 확인 (기존 기능 유지).
	 *  기대: 나이와 상관없이 산 셀은 빨강, 죽은 셀은 연노랑이고, 색이 바뀌는 나이가 없다.
	 */
	@Test @Tag("existing")
	@DisplayName("[기존 기능] Classic은 나이와 상관없이 원본처럼 빨강·연노랑이다")
	void classicIsTheOriginalLook()
	{
		CellColoring classic = new ClassicColoring();
		for (long age : new long[] { 1, 2, 7, 16, 1_000_000 })
			assertEquals(Color.RED, classic.live(age), "age " + age);
		assertEquals(Colors.LIGHT_YELLOW, classic.dead());
		assertEquals(0, classic.colorChangesAt().length);
	}

	/** 목적: 나이별 색이 정한 대로인지 확인.
	 *  기대: 1살 연분홍 #FFB0B0, 2살 #FF8C8C, 3살 #FF6060, 4~5살 빨강 #FF3030, 6~7살 #E00000,
	 *  8~10살 #B00000, 11~14살 #780000, 15살 이상 거의 검정 #300000.
	 *  죽은 셀은 원본과 같은 연노랑.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] By age는 연분홍에서 시작해 15살에 거의 검정이 될 때까지 8단계로 진해진다")
	void ageColorsByStep()
	{
		CellColoring byAge = new AgeColoring();
		Map<Long, Integer> expected = new LinkedHashMap<>();
		expected.put(1L,  0xFFB0B0);
		expected.put(2L,  0xFF8C8C);
		expected.put(3L,  0xFF6060);
		expected.put(4L,  0xFF3030);  expected.put(5L,  0xFF3030);
		expected.put(6L,  0xE00000);  expected.put(7L,  0xE00000);
		expected.put(8L,  0xB00000);  expected.put(10L, 0xB00000);
		expected.put(11L, 0x780000);  expected.put(14L, 0x780000);
		expected.put(15L, 0x300000);  expected.put(1_000_000L, 0x300000);
		for (Map.Entry<Long, Integer> e : expected.entrySet())
			assertEquals(e.getValue(), byAge.live(e.getKey()).getRGB() & 0xffffff, "age " + e.getKey());
		assertEquals(Colors.LIGHT_YELLOW, byAge.dead());
	}

	/** 목적: "오래 살수록 진해진다"는 요구를 확인.
	 *  방법: 1살부터 100살까지 색의 밝기를 잰다.
	 *  기대: 밝기는 나이가 들수록 줄기만 하고 늘지 않으며, 1살과 15살 이상은 확연히 다르다.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] By age는 나이가 들수록 어두워지기만 한다")
	void olderIsNeverLighter()
	{
		CellColoring byAge = new AgeColoring();
		double previous = Double.MAX_VALUE;
		for (long age = 1; age <= 100; ++age)
		{
			double brightness = brightness(byAge.live(age));
			assertTrue(brightness <= previous, "age " + age + " is lighter than age " + (age - 1));
			previous = brightness;
		}
		assertTrue(brightness(byAge.live(1)) > 3 * brightness(byAge.live(15)));
	}

	/** 목적: 화면은 colorChangesAt()에 적힌 나이에만 셀을 다시 칠한다. 이 목록이 실제
	 *  색 변화와 다르면 화면에 옛 색이 남거나 쓸데없이 다시 칠하게 된다.
	 *  방법: 2살부터 100살까지, 전 나이와 색이 다른 나이를 모아 목록과 비교한다.
	 *  기대: 색이 바뀌는 나이 = 목록 (2, 3, 4, 6, 8, 11, 15). 돌려받은 목록을 고쳐도 원래 목록은 그대로.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] '색이 바뀌는 나이' 목록이 실제로 색이 바뀌는 나이와 정확히 같다")
	void colorChangesMatchTheColors()
	{
		CellColoring byAge = new AgeColoring();
		List<Long> changes = new ArrayList<>();
		for (long age = 2; age <= 100; ++age)
			if (!byAge.live(age).equals(byAge.live(age - 1)))
				changes.add(age);

		List<Long> listed = new ArrayList<>();
		for (long age : byAge.colorChangesAt())
			listed.add(age);
		assertEquals(changes, listed);
		assertEquals(List.of(2L, 3L, 4L, 6L, 8L, 11L, 15L), listed);

		byAge.colorChangesAt()[0] = 99;
		assertEquals(2, byAge.colorChangesAt()[0]);
	}

	private static double brightness(Color color)
	{
		return 0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue();
	}
}
