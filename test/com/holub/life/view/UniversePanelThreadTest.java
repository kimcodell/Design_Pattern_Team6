package com.holub.life.view;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.*;
import com.holub.life.model.*;

/** 화면이 "보드와 화면은 EDT에서만 쓴다"는 규칙을 스스로 지키게 하는지 확인한다.
 *  일부러 EDT 밖에서 보드를 움직여야 하므로 OnEventDispatchThread 확장을 쓰지 않는다.
 */
@DisplayName("화면 스레드 규칙 (EDT 전용)")
class UniversePanelThreadTest
{
	/** 목적: 계산을 다른 스레드로 옮기면(가이드 C의 대안) 화면이 그리는 도중 보드가
	 *  바뀌어 가끔 틀린 그림이 나올 수 있다. 그런 실수를 "가끔 깨지는 화면" 대신
	 *  즉시 오류로 드러내는지 확인.
	 *  방법: 패널을 단 보드를 테스트 스레드(EDT 아님)에서 진행·편집·초기화한다.
	 *  기대: 세 경우 모두 IllegalStateException이 나고, 메시지에 EDT 규칙이 적혀 있다.
	 */
	@Test @Tag("design")
	@DisplayName("[구조 D2] EDT 밖에서 보드를 바꾸면 화면이 즉시 오류를 낸다")
	void rejectsNotificationsOffTheEventThread()
	{
		assertFalse(SwingUtilities.isEventDispatchThread());
		LifeModel model = LifeModel.builder().build();
		new UniversePanel(ReadOnlyBoardView.of(model), 8);

		IllegalStateException step  = assertThrows(IllegalStateException.class, model::step);
		IllegalStateException edit  = assertThrows(IllegalStateException.class, () -> model.toggle(1, 1));
		IllegalStateException clear = assertThrows(IllegalStateException.class, model::clear);
		for (IllegalStateException e : new IllegalStateException[] { step, edit, clear })
			assertTrue(e.getMessage().contains("event-dispatch thread"), e.getMessage());
	}

	/** 목적: 오류가 났을 때 원인을 바로 찾을 수 있는지 확인.
	 *  방법: EDT 밖에서 진행·편집·초기화를 각각 해 보고 메시지를 읽는다.
	 *  기대: 메시지에 어떤 알림(stepped, edited, reset)이 어느 스레드에서 왔는지 적혀 있다.
	 */
	@Test @Tag("design")
	@DisplayName("[구조 D2] 오류 메시지에 어떤 알림이 어느 스레드에서 왔는지 적힌다")
	void errorSaysWhichNotificationAndThread()
	{
		LifeModel model = LifeModel.builder().build();
		new UniversePanel(ReadOnlyBoardView.of(model), 8);
		String thread = Thread.currentThread().getName();

		assertMessage(model::step,               "stepped", thread);
		assertMessage(() -> model.toggle(1, 1),  "edited",  thread);
		assertMessage(model::clear,              "reset",   thread);
	}

	/** 목적: EDT 규칙은 화면이 붙은 보드에만 적용되는지 확인. 모델 자체가 EDT를
	 *  요구하면 모델을 화면과 따로(창 없이, 아무 스레드에서) 테스트할 수 없게 된다.
	 *  방법: 패널을 달지 않은 보드를 테스트 스레드(EDT 아님)에서 편집·진행·초기화한다.
	 *  기대: 예외 없이 동작하고 결과가 맞다 (블링커가 한 세대 뒤 세로로 선다).
	 */
	@Test @Tag("design")
	@DisplayName("[구조 D2] 화면을 달지 않은 보드는 EDT 밖에서도 쓸 수 있다 (모델 단독 테스트 가능)")
	void boardWithoutPanelWorksOffTheEventThread()
	{
		assertFalse(SwingUtilities.isEventDispatchThread());
		LifeModel model = LifeModel.builder().build();
		model.toggle(10, 9);
		model.toggle(10, 10);
		model.toggle(10, 11);
		model.step();
		assertTrue(model.isAlive(9, 10) && model.isAlive(10, 10) && model.isAlive(11, 10));
		assertFalse(model.isAlive(10, 9) || model.isAlive(10, 11));
		model.clear();
		assertFalse(model.isAlive(10, 10));
	}

	private static void assertMessage(org.junit.jupiter.api.function.Executable change,
									  String notification, String thread)
	{
		String message = assertThrows(IllegalStateException.class, change).getMessage();
		assertTrue(message.contains("UniversePanel." + notification + "()"), message);
		assertTrue(message.contains("\"" + thread + "\""), message);
	}

	/** 목적: 규칙을 지키면(실제 프로그램처럼 EDT에서 실행) 아무 문제 없이 동작하는지 확인.
	 *  방법: 같은 일을 SwingUtilities.invokeAndWait로 EDT에서 한다.
	 *  기대: 예외가 없고, 클릭한 셀이 살아 있다.
	 */
	@Test @Tag("design")
	@DisplayName("[구조 D2] EDT에서 보드를 바꾸면 정상 동작한다")
	void acceptsNotificationsOnTheEventThread() throws Exception
	{
		LifeModel model = LifeModel.builder().build();
		AtomicReference<Throwable> failure = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() ->
		{
			try
			{	new UniversePanel(ReadOnlyBoardView.of(model), 8);
				model.toggle(1, 1);
				model.step();
				model.toggle(5, 5);
			}
			catch (Throwable thrown)
			{	failure.set(thrown);
			}
		});
		assertNull(failure.get());
		assertTrue(model.isAlive(5, 5));
	}
}
