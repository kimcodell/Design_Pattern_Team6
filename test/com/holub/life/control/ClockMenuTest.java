package com.holub.life.control;

import static org.junit.jupiter.api.Assertions.*;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.*;

@DisplayName("Go 메뉴")
class ClockMenuTest
{
	/** 목적: 메뉴를 열 때 실제 실행 상태와 현재 타이머 간격을 표시하는지 확인. */
	@Test @Tag("new-feature")
	@DisplayName("메뉴를 다시 열면 실제 실행 상태와 속도가 표시된다")
	void readsCurrentStatusOnEachOpen() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			Clock clock = new Clock();
			JMenu goMenu = new JMenu("Go");
			JMenuItem statusItem = new JMenuItem("Status");
			goMenu.add(statusItem);
			ClockMenu.showStatusOnOpen(goMenu, statusItem, clock);

			goMenu.setSelected(true);
			assertEquals("Status: Stopped", statusItem.getText());
			assertFalse(statusItem.isEnabled());

			try
			{
				goMenu.setSelected(false);
				clock.startTicking(30);
				goMenu.setSelected(true);
				assertEquals("Status: Running (30 ms/generation)", statusItem.getText());

				goMenu.setSelected(false);
				clock.startTicking(150);
				goMenu.setSelected(true);
				assertEquals("Status: Running (150 ms/generation)", statusItem.getText());
			}
			finally
			{
				goMenu.setSelected(false);
				clock.stop();
			}

			goMenu.setSelected(true);
			assertEquals("Status: Stopped", statusItem.getText());
		});
	}
}
