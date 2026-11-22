package com.holub.life.control;

import com.holub.life.model.BoardEdit;
import com.holub.life.model.Coord;
import com.holub.life.model.LifeModel;
import com.holub.life.view.UniversePanel;
import com.holub.life.view.UniversePanel.CellClickListener;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Installs the selected editing strategy as the board's click listener. */
public final class StampToolbar extends JPanel {
    private record Tool(String label, CellClickListener onClick) {
        @Override public String toString() { return label; }
    }

    public StampToolbar(LifeModel model, UniversePanel board) {
        super(new FlowLayout(FlowLayout.LEADING));
        Tool singleCell = new Tool("Single cell", model::toggle);
        JComboBox<Tool> selector = new JComboBox<>();
        selector.addItem(singleCell);
        selector.addItem(stamp("Block", model, "##", "##"));
        selector.addItem(stamp("Beehive", model, ".##.", "#..#", ".##."));
        selector.addItem(stamp("Blinker", model, "###"));
        selector.addItem(stamp("Toad", model, ".###", "###."));
        selector.addItem(stamp("Beacon", model, "##..", "##..", "..##", "..##"));
        selector.addItem(stamp("Pulsar", model,
            "..###...###..",
            ".............",
            "#....#.#....#",
            "#....#.#....#",
            "#....#.#....#",
            "..###...###..",
            ".............",
            "..###...###..",
            "#....#.#....#",
            "#....#.#....#",
            "#....#.#....#",
            ".............",
            "..###...###.."));
        selector.addItem(stamp("Glider", model, ".#.", "..#", "###"));
        selector.addItem(stamp("Lightweight spaceship", model, ".#..#", "#....", "#...#", "####."));

        board.setCellClickListener(singleCell.onClick());
        selector.addActionListener(event -> {
            Tool selected = selector.getItemAt(selector.getSelectedIndex());
            board.setCellClickListener(selected.onClick());
        });
        JLabel label = new JLabel("Board tool");
        label.setLabelFor(selector);
        add(label);
        add(selector);
    }

    private static Tool stamp(String name, LifeModel model, String... rows) {
        List<Coord> offsets = new ArrayList<>();
        for (int row = 0; row < rows.length; row++)
            for (int col = 0; col < rows[row].length(); col++)
                if (rows[row].charAt(col) == '#')
                    offsets.add(new Coord(row, col));

        return new Tool(name, (top, left) -> {
            List<Coord> cells = new ArrayList<>();
            for (Coord offset : offsets) {
                int row = top + offset.row();
                int col = left + offset.col();
                if (row < model.size() && col < model.size())
                    cells.add(new Coord(row, col));
            }
            model.apply(BoardEdit.set(cells, true));
        });
    }
}
