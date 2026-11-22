package com.holub.life.control;

import com.holub.ui.MenuSite;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Tag("design")
class MenuSiteTest {
    @Test
    void identicalRegistrationsAreIndependentAcrossWindows() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JMenuBar firstBar = new JMenuBar();
            JMenuBar secondBar = new JMenuBar();
            MenuSite first = new MenuSite(firstBar);
            MenuSite second = new MenuSite(secondBar);
            Object owner = new Object();
            AtomicInteger clicks = new AtomicInteger();
            first.addLine(owner, "Grid", "Clear", event -> fail("Wrong window"));
            second.addLine(owner, "Grid", "Clear", event -> clicks.incrementAndGet());

            first.setEnable(owner, false);
            assertFalse(first.getMyMenuItem(owner, "Grid", "Clear").isEnabled());
            assertTrue(second.getMyMenuItem(owner, "Grid", "Clear").isEnabled());
            first.removeMyMenus(owner);
            assertEquals(0, firstBar.getMenuCount());
            assertEquals(1, secondBar.getMenuCount());
            secondBar.getMenu(0).getItem(0).doClick();
            assertEquals(1, clicks.get());
        });
    }
}
