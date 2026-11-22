package com.holub.life.control;

import com.holub.life.model.Coord;
import com.holub.life.pattern.*;
import com.holub.life.model.LifeModel;
import com.holub.life.view.UniversePanel;
import java.awt.event.MouseEvent;
import java.util.Set;
import java.util.HashSet;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Tag("new-feature")
class StampToolbarTest {
    private static final PatternCatalog CATALOG = PatternCatalog.conway();

    private static class Editor {
        final LifeModel model = LifeModel.builder().build();
        final UniversePanel board = new UniversePanel(model, 4);
        final StampToolbar toolbar = new StampToolbar(model, board, CATALOG);

        Editor() { board.setSize(board.getPreferredSize()); }

        void choose(String name) {
            JComboBox<?> selector = (JComboBox<?>) toolbar.getComponent(1);
            for (int i = 0; i < selector.getItemCount(); i++) {
                if (selector.getItemAt(i).toString().equals(name)) {
                    selector.setSelectedIndex(i);
                    return;
                }
            }
            fail("Missing tool " + name);
        }

        void click(int row, int col) {
            board.dispatchEvent(new MouseEvent(board, MouseEvent.MOUSE_PRESSED, 1, 0,
                col * 4 + 1, row * 4 + 1, 1, false, MouseEvent.BUTTON1));
        }
    }

    @ParameterizedTest(name = "{0} evolves with period {2}")
    @CsvSource({
        "Block,4,1,0,0", "Beehive,6,1,0,0", "Blinker,3,2,0,0", "Toad,6,2,0,0",
        "Beacon,8,2,0,0", "Pulsar,48,3,0,0", "Glider,5,4,1,1", "LWSS,9,4,0,-2"
    })
    void presetEvolution(String name, int population, int period, int dy, int dx) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Editor e = new Editor();
            PatternMonitor monitor = new PatternMonitor(e.model, new PatternAnalyzer(CATALOG));
            e.choose(name);
            e.click(8, 8);
            Set<Coord> initial = e.model.snapshot().liveCells();
            assertEquals(population, initial.size());
            assertNamed(monitor.latest(), name);
            for (int generation = 1; generation <= period; generation++) {
                e.model.step();
                assertNamed(monitor.latest(), name);
                if (generation < period)
                    assertNotEquals(initial, e.model.snapshot().liveCells());
            }
            Set<Coord> expected = new HashSet<>();
            for (Coord c : initial)
                expected.add(new Coord(c.row() + dy, c.col() + dx));
            assertEquals(expected, e.model.snapshot().liveCells());
        });
    }

    @ParameterizedTest
    @CsvSource({"R-pentomino,METHUSELAH,5", "Acorn,METHUSELAH,7",
                "Diehard,METHUSELAH,7", "Gosper glider gun,GENERATOR,36"})
    void stampedSeedAppearsInItsPanelCategory(String name, PatternCategory category, int population)
            throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Editor e = new Editor();
            PatternMonitor monitor = new PatternMonitor(e.model, new PatternAnalyzer(CATALOG));
            e.choose(name);
            e.click(1, 1);
            assertEquals(population, e.model.snapshot().liveCells().size());
            assertNamed(monitor.latest(), name);
            assertEquals(1, monitor.latest().count(category));
        });
    }

    private static void assertNamed(PatternReport report, String name) {
        assertTrue(report.unclassified().isEmpty());
        assertEquals(1, report.detections().size());
        assertEquals(name, report.detections().get(0).name());
    }

    @Test void gliderRejectsObstructionsAndCanBeRepeated() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Editor e = new Editor();
            e.click(2, 3);
            e.choose("Glider");
            e.click(2, 3);
            assertEquals(Set.of(new Coord(2, 3)), e.model.snapshot().liveCells());
            e.model.clear();
            e.click(2, 3);
            Set<Coord> expected = Set.of(new Coord(2, 4),
                new Coord(3, 5), new Coord(4, 3), new Coord(4, 4), new Coord(4, 5));
            assertEquals(expected, e.model.snapshot().liveCells());
            e.click(2, 3);
            assertEquals(expected, e.model.snapshot().liveCells());
            e.click(2, 6);
            assertEquals(expected, e.model.snapshot().liveCells());
            e.click(12, 13);
            assertEquals(10, e.model.snapshot().liveCells().size());
            e.choose("Single cell");
            e.click(2, 4);
            assertFalse(e.model.isAlive(2, 4));
        });
    }

    @Test void blockRejectsClippingAtCornerAndBlinkerOscillates() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Editor e = new Editor();
            int last = e.model.size() - 1;
            e.choose("Block");
            e.click(last, last);
            assertTrue(e.model.snapshot().liveCells().isEmpty());
            e.model.clear();
            e.click(3, 3);
            assertEquals(Set.of(new Coord(3, 3), new Coord(3, 4), new Coord(4, 3),
                new Coord(4, 4)), e.model.snapshot().liveCells());
            e.model.clear();
            e.choose("Blinker");
            e.click(5, 6);
            e.model.step();
            assertEquals(Set.of(new Coord(4, 7), new Coord(5, 7), new Coord(6, 7)),
                e.model.snapshot().liveCells());
        });
    }
}
