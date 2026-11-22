package com.holub.life.pattern;

import static org.junit.jupiter.api.Assertions.*;
import static com.holub.life.pattern.Fixtures.*;

import java.util.*;
import org.junit.jupiter.api.*;
import com.holub.life.model.*;

@DisplayName("패턴 모니터 PatternMonitor (Observer)")
class PatternMonitorTest
{
	/** 목적: 보드가 바뀔 때마다(세대·편집·초기화) 새 결과를 알리는지.
	 *  방법: 결과 리스너를 붙이고 편집, step, clear를 한다.
	 *  기대: 세 번 알림, 마지막 결과는 빈 보드.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[패턴] 편집·세대·초기화마다 새 분석 결과를 알린다")
	void reportsOnEveryChange()
	{
		LifeModel model = LifeModel.builder().build();
		PatternMonitor monitor = new PatternMonitor(model, new PatternAnalyzer(CONWAY));
		List<PatternReport> reports = new ArrayList<>();
		monitor.addReportListener(reports::add);

		put(model, "Blinker", 10, 10);
		model.step();
		model.clear();

		assertEquals(3, reports.size());
		assertEquals(Map.of("Blinker", 1), names(reports.get(0)));
		assertEquals(1, reports.get(1).generation());
		assertTrue(monitor.latest().detections().isEmpty());
	}

	/** 목적: 결과를 보여 주는 화면이 숨어 있으면 분석 비용을 쓰지 않는지.
	 *  방법: setActive(false) 뒤 10세대 진행, 다시 켠다.
	 *  기대: 꺼진 동안 알림 0번, 켜면 한 번 분석해 최신 세대 결과.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[패턴] 꺼져 있으면 분석하지 않고, 다시 켜면 한 번만 분석한다")
	void inactiveMonitorSkipsAnalysis()
	{
		LifeModel model = LifeModel.builder().build();
		PatternMonitor monitor = new PatternMonitor(model, new PatternAnalyzer(CONWAY));
		List<PatternReport> reports = new ArrayList<>();
		monitor.addReportListener(reports::add);

		monitor.setActive(false);
		put(model, "Glider", 5, 5);
		for (int i = 0; i < 10; ++i) model.step();
		assertEquals(0, reports.size());

		monitor.setActive(true);
		assertEquals(1, reports.size());
		assertEquals(10, monitor.latest().generation());
		assertEquals(Map.of("Glider", 1), names(monitor.latest()));
	}
}
