package com.holub.life.pattern;

import static org.junit.jupiter.api.Assertions.*;
import static com.holub.life.pattern.Fixtures.*;

import java.util.*;
import org.junit.jupiter.api.*;
import com.holub.life.model.*;

/** 보드 위 패턴을 찾아 5그룹으로 묶는 PatternAnalyzer. 단계는
 *  도감 일치(CatalogMatcher) → 행동 판정(BehaviorClassifier) → 잔해(Unclassified).
 */
@DisplayName("패턴 분석 PatternAnalyzer")
class PatternAnalyzerTest
{
	private final PatternAnalyzer analyzer = new PatternAnalyzer(CONWAY);

	/** 목적: 도감의 모든 패턴을 모든 방향·모든 위상에서 이름과 그룹까지 맞게 찾는지.
	 *  방법: 패턴마다 8방향으로 빈 보드 가운데에 놓고, 주기만큼(최대 30세대)
	 *  진행하며 매 세대 분석한다. Methuselah는 씨앗(0세대)만.
	 *  기대: 매번 그 패턴의 그룹에서 그 패턴 하나만, 도감 일치로 나온다
	 *  (Gun이 쏜 Glider는 다른 그룹이라 세지 않음).
	 */
	@Test @Tag("new-feature")
	@DisplayName("[패턴] 도감 패턴 18개를 8방향·모든 위상에서 이름과 그룹까지 찾는다")
	void everyCatalogPatternIsFoundInEveryOrientationAndPhase()
	{
		for (PatternDefinition d : CONWAY.definitions())
			for (int o = 0; o < Shape.ORIENTATIONS; ++o)
			{
				LifeModel model = LifeModel.builder().size(128).build();
				put(model, d.seed().oriented(o), 40, 40);
				int generations = d.category() == PatternCategory.METHUSELAH ? 1 : Math.max(1, d.period());
				for (int g = 0; g < generations; ++g)
				{
					List<Detection> found = analyzer.analyze(model).detections().stream()
										   .filter(x -> x.source() == Detection.Source.CATALOG
													  && x.category() == d.category()).toList();
					String where = d.name() + " orientation " + o + " generation " + g;
					assertEquals(1, found.size(), where + ": " + found);
					assertEquals(d.name(), found.get(0).name(), where);
					assertEquals(d.category(), found.get(0).category(), where);
					model.step();
				}
			}
	}

	/** 목적: 둘레가 비어 있어야 일치로 본다는 규칙 확인. 큰 덩어리의 일부를
	 *  작은 패턴으로 잘못 세면 안 된다.
	 *  방법: 블록 바로 옆에 셀 하나를 붙인다.
	 *  기대: 도감의 Block으로 나오지 않는다.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[패턴] 둘레에 셀이 붙은 블록은 Block으로 세지 않는다")
	void aNeighborSpoilsTheMatch()
	{
		LifeModel model = LifeModel.builder().build();
		put(model, "Block", 10, 10);
		model.toggle(10, 12);
		assertFalse(names(analyzer.analyze(model)).containsKey("Block"));
	}

	/** 목적: 생성 패턴(Gun)이 계속 Glider를 쏘아도 매 세대 찾고, 쏘아 낸 Glider는
	 *  Spaceship으로 따로 세는지.
	 *  방법: Gosper glider gun을 150세대 진행하며 매 세대 분석한다.
	 *  기대: 매 세대 gun 1개. 120세대에는 Glider가 1개 이상.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[패턴] Gosper glider gun을 150세대 동안 매 세대 찾고, 쏜 Glider는 따로 센다")
	void gunIsFoundEveryGenerationAndItsGlidersSeparately()
	{
		LifeModel model = LifeModel.builder().build();
		put(model, "Gosper glider gun", 1, 1);
		for (int g = 0; g < 150; ++g)
		{
			Map<String, Integer> names = names(analyzer.analyze(model));
			assertEquals(1, names.getOrDefault("Gosper glider gun", 0), "generation " + g + ": " + names);
			if (g == 120)
				assertTrue(names.getOrDefault("Glider", 0) >= 1, names.toString());
			model.step();
		}
	}

	/** 목적: 도감에 없는 패턴도 행동으로 그룹을 정하는지.
	 *  방법: 도감에 없는 Ship(정적), Long boat(정적)을 놓는다.
	 *  기대: Still Life 그룹, 출처는 행동 판정.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[패턴] 도감에 없는 정적 패턴(Ship)은 행동으로 Still Life로 분류한다")
	void unknownStillLifeIsGroupedByBehavior()
	{
		LifeModel model = LifeModel.builder().build();
		put(model, Shape.parse("OO.", "O.O", ".OO"), 10, 10);         // ship
		List<Detection> found = analyzer.analyze(model).detections();
		assertEquals(1, found.size());
		assertEquals(PatternCategory.STILL_LIFE, found.get(0).category());
		assertEquals(Detection.Source.BEHAVIOR, found.get(0).source());
	}

	/** 목적: 규칙이 도감과 다르면 도감을 쓰지 않는지(모양은 같아도 다른 패턴일 수 있다).
	 *  방법: HighLife 보드에 Block과 Glider를 놓는다.
	 *  기대: 도감 일치는 하나도 없고, 둘 다 행동으로 정적·이동 그룹.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[패턴] 규칙이 B3/S23가 아니면 도감 단계를 건너뛰고 행동으로만 분류한다")
	void otherRulesSkipTheCatalog()
	{
		LifeModel model = LifeModel.builder().rule(Rule.parse("B36/S23")).build();
		put(model, "Block", 5, 5);
		put(model, "Glider", 20, 20);
		PatternReport report = analyzer.analyze(model);
		assertFalse(analyzer.usesCatalog(model));
		assertTrue(report.detections().stream().allMatch(d -> d.source() == Detection.Source.BEHAVIOR));
		assertEquals(1, report.count(PatternCategory.STILL_LIFE));
		assertEquals(1, report.count(PatternCategory.SPACESHIP));
	}

	/** 목적: TORUS 보드에서 가장자리를 넘어간 패턴도 하나로 찾는지(A3의 locate 사용).
	 *  방법: 오른쪽 아래 모서리에 걸친 글라이더를 12세대 진행한다.
	 *  기대: 매 세대 Glider 하나.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[패턴] 토러스: 모서리에 걸친 글라이더를 매 세대 하나로 찾는다")
	void torusPatternsAcrossTheEdge()
	{
		LifeModel model = LifeModel.builder().boundary(Boundary.TORUS).build();
		put(model, "Glider", 62, 62);
		for (int g = 0; g < 12; ++g)
		{
			assertEquals(Map.of("Glider", 1), names(analyzer.analyze(model)), "generation " + g);
			model.step();
		}
	}

	/** 목적: 모든 산 셀이 정확히 한 탐지에 속하는지(빠지거나 두 번 세지 않음).
	 *  방법: 여러 패턴과 R-pentomino 잔해가 섞인 보드를 100세대 진행하며 확인한다.
	 *  기대: 탐지 셀의 합집합 = 산 셀, 탐지 셀 수의 합 = 산 셀 수.
	 */
	@Test @Tag("design")
	@DisplayName("[패턴] 모든 산 셀이 정확히 한 탐지에 속한다 (100세대 무작위 진행)")
	void everyLiveCellIsInExactlyOneDetection()
	{
		LifeModel model = LifeModel.builder().build();
		put(model, "Pulsar", 2, 2);
		put(model, "R-pentomino", 30, 30);
		put(model, "LWSS", 50, 5);
		put(model, "Beacon", 5, 50);
		for (int g = 0; g < 100; ++g)
		{
			PatternReport report = analyzer.analyze(model);
			Set<Coord> union = new HashSet<>();
			int total = 0;
			for (Detection d : report.detections()) { union.addAll(d.cells()); total += d.cells().size(); }
			Set<Coord> live = model.snapshot().liveCells();
			assertEquals(live, union, "generation " + g);
			assertEquals(live.size(), total, "generation " + g);
			model.step();
		}
	}

	/** 목적: 결과를 5그룹으로 묶는 PatternReport.grouped()가 모든 그룹을 담고
	 *  같은 이름을 하나로 세는지.
	 *  방법: 블록 2개, 블링커 1개를 놓고 묶는다.
	 *  기대: 5그룹 모두 있음, Still Life에 Block ×2, Oscillator에 Blinker ×1.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[패턴] 결과를 5그룹으로 묶고 같은 패턴은 개수로 센다")
	void reportGroupsByCategoryAndName()
	{
		LifeModel model = LifeModel.builder().build();
		put(model, "Block", 5, 5);
		put(model, "Block", 5, 20);
		put(model, "Blinker", 30, 30);
		Map<PatternCategory, List<PatternReport.Group>> grouped = analyzer.analyze(model).grouped();
		assertEquals(EnumSet.allOf(PatternCategory.class), grouped.keySet());
		assertEquals("Block", grouped.get(PatternCategory.STILL_LIFE).get(0).name());
		assertEquals(2, grouped.get(PatternCategory.STILL_LIFE).get(0).count());
		assertEquals(1, grouped.get(PatternCategory.OSCILLATOR).get(0).count());
		assertTrue(grouped.get(PatternCategory.SPACESHIP).isEmpty());
	}
}
