package com.holub.life.control;

import com.holub.life.model.BoardEdit;
import com.holub.life.model.Coord;
import com.holub.life.model.LifeModel;
import com.holub.life.view.UniversePanel;
import com.holub.life.view.UniversePanel.CellClickListener;
import java.awt.FlowLayout;
import com.holub.life.pattern.PatternCatalog;
import com.holub.life.pattern.PatternDefinition;
import com.holub.life.pattern.Shape;
import java.util.Set;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Installs the selected editing strategy as the board's click listener. */
public final class StampToolbar extends JPanel {
    private record Tool(String label, CellClickListener onClick) {
        @Override public String toString() { return label; }
    }

    private static final Set<String> PRESETS = Set.of(
        "Block", "Beehive", "Blinker", "Toad", "Beacon", "Pulsar", "Glider", "LWSS",
        "R-pentomino", "Acorn", "Diehard", "Gosper glider gun");
    private final JLabel feedback = new JLabel();

    public StampToolbar(LifeModel model, UniversePanel board, PatternCatalog catalog) {
        super(new FlowLayout(FlowLayout.LEADING));
        Tool singleCell = new Tool("Single cell", model::toggle);
        JComboBox<Tool> selector = new JComboBox<>();
        selector.addItem(singleCell);
        for (PatternDefinition pattern : catalog.definitions()) {
            if (PRESETS.contains(pattern.name()))
                selector.addItem(new Tool(pattern.name(),
                    (row, col) -> stamp(model, pattern.seed(), row, col)));
        }

        board.setCellClickListener(singleCell.onClick());
        selector.addActionListener(event -> {
            feedback.setText("");
            Tool selected = selector.getItemAt(selector.getSelectedIndex());
            board.setCellClickListener(selected.onClick());
        });
        JLabel label = new JLabel("Board tool");
        label.setLabelFor(selector);
        add(label);
        add(selector);
        add(feedback);
    }

    private void stamp(LifeModel model, Shape seed, int top, int left) {
        int bottom = top + seed.height();
        int right = left + seed.width();
        if (top < 0 || left < 0 || bottom > model.size() || right > model.size()) {
            feedback.setText("Move inward to fit the whole stamp.");
            return;
        }
        // Catalog matching requires an empty border and empty gaps in the seed.
        Set<Coord> cells = seed.translate(top, left).cells();
        for (int r = top - 1; r <= bottom; r++) {
            for (int c = left - 1; c <= right; c++) {
                var at = model.boundary().locate(r, c, model.size());
                if (at.isPresent() && model.isAlive(at.get().row(), at.get().col())
                        && !cells.contains(at.get())) {
                    feedback.setText("Leave one empty cell around the stamp.");
                    return;
                }
            }
        }
        feedback.setText("");
        model.apply(BoardEdit.set(cells, true));
    }
}
