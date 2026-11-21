package com.holub.life.control;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JMenu;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.*;

/** 시계를 메뉴·창 없이 단독으로 시험한다. */
@DisplayName("시계 Clock")
class ClockTest
{
	/** 목적: P2·C2(시계가 Singleton이고 생성자에서 메뉴를 만들어, 메뉴바 없이는
	 *  만들 수 없던 문제) 수정 확인, 그리고 원본의 "한 틱 진행" 동작 유지.
	 *  방법: 메뉴 없이 new Clock()을 만들고 tick()을 두 번 부른다.
	 *  기대: 관찰자가 두 번 불린다.
	 */
	@Test @Tag("design")
	@DisplayName("[구조 P2] 메뉴바 없이 시계를 만들 수 있고, 한 틱씩 진행된다")
	void canBeCreatedWithoutAMenuBarAndTicksOnDemand()
	{
		Clock clock = new Clock();
		AtomicInteger ticks = new AtomicInteger();
		clock.addClockListener(ticks::incrementAndGet);
		clock.tick();
		clock.tick();
		assertEquals(2, ticks.get());
	}

	/** 목적: T1(원본은 별도 Timer 스레드에서 계산해 화면 스레드와 경쟁) 수정 확인.
	 *  모델은 한 스레드(EDT)에서만 쓰기로 했으므로 틱도 EDT에서 와야 한다.
	 *  방법: 10ms 간격으로 시계를 돌리고 틱 세 번이 어느 스레드에서 오는지 본다.
	 *  기대: 모두 EDT.
	 */
	@Test @Tag("design")
	@DisplayName("[구조 T1] 시계의 틱은 화면 스레드(EDT)에서 온다")
	void runningClockTicksOnTheEventDispatchThread() throws Exception
	{
		Clock clock = new Clock();
		CountDownLatch threeTicks = new CountDownLatch(3);
		AtomicInteger offEdt = new AtomicInteger();
		clock.addClockListener(() ->
		{
			if (!SwingUtilities.isEventDispatchThread()) offEdt.incrementAndGet();
			threeTicks.countDown();
		});
		clock.startTicking(10);
		try
		{
			assertTrue(threeTicks.await(5, TimeUnit.SECONDS), "clock did not tick");
		}
		finally
		{
			clock.stop();
		}
		assertEquals(0, offEdt.get(), "every tick must arrive on the event-dispatch thread");
	}

	/** 목적: 정지 상태에서 한 세대만 진행하고 계속 정지하는지 확인. */
	@Test @Tag("defect-fix")
	@DisplayName("정지 상태에서 한 세대 진행을 누르면 한 틱만 발생한다")
	void stepsOnceWhenStopped() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			Clock clock = new Clock();
			AtomicInteger ticks = new AtomicInteger();
			clock.addClockListener(ticks::incrementAndGet);

			clock.stepOnce();

			assertEquals(1, ticks.get());
			assertFalse(clock.isRunning());
		});
	}

	/** 목적: 메뉴가 선택된 상태에서도 수동 틱은 진행되고 자동 실행은 멈추는지 확인. */
	@Test @Tag("defect-fix")
	@DisplayName("자동 실행 중 한 세대 진행을 누르면 한 틱 진행 후 정지한다")
	void stepsOnceAndStopsAutomaticTicks() throws Exception
	{
		Clock clock = new Clock();
		AtomicInteger ticks = new AtomicInteger();
		SwingUtilities.invokeAndWait(() ->
		{
			clock.addClockListener(ticks::incrementAndGet);
			MenuSelectionManager menuSelection = MenuSelectionManager.defaultManager();
			clock.startTicking(30);
			try
			{
				assertTrue(clock.isRunning());
				menuSelection.setSelectedPath(new MenuElement[] {new JMenu("Go")});

				clock.stepOnce();

				assertEquals(1, ticks.get());
				assertFalse(clock.isRunning());
			}
			finally
			{
				menuSelection.clearSelectedPath();
				clock.stop();
			}
		});
	}
}
