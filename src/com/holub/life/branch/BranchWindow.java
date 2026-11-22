package com.holub.life.branch;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import com.holub.life.model.BoardEdit;
import com.holub.life.model.BoardListener;
import com.holub.life.model.BoardView;
import com.holub.life.model.Coord;
import com.holub.life.model.LifeModel;
import com.holub.life.view.UniversePanel;

/** Bookmark comparison using Prototype, Observer and Action commands on EDT. */
public final class BranchWindow extends JDialog
{
	private final BookmarkRepository repository = new BookmarkRepository();
	private final BranchSession session;
	private final DefaultListModel<Bookmark> bookmarkItems = new DefaultListModel<>();
	private final JList<Bookmark> bookmarks = new JList<>(bookmarkItems);
	private final JTextField bookmarkName = new JTextField(12);
	private final JComboBox<BranchSession.Side> source = new JComboBox<>(BranchSession.Side.values());
	private final JLabel status = new JLabel();
	private final JPanel boards = new JPanel(new GridLayout(1, 2, 12, 0));
	private final Timer timer;
	private final Action save;
	private final Action compare;
	private final Action delete;
	private final Action play;
	private final Action pause;
	private final Action step;
	private BoardPane left;
	private BoardPane right;

	public BranchWindow(Window owner, LifeModel initial)
	{
		super(owner, "북마크·분기 비교", ModalityType.MODELESS);
		Bookmark start = new Bookmark("시작 상태", initial);
		repository.add(start);
		bookmarkItems.addElement(start);
		session = new BranchSession(start);
		timer = new Timer(150, e -> session.stepBoth());
		timer.setCoalesce(true);

		// Swing Actions are commands shared by controls without embedding model logic.
		save = command("북마크 저장", this::saveBookmark);
		compare = command("선택 북마크로 A/B 시작", this::compareSelected);
		delete = command("삭제", this::deleteSelected);
		play = command("재생", () -> { timer.start(); updateControls(); });
		pause = command("정지", this::pause);
		step = command("한 단계 진행", session::stepBoth);

		setDefaultCloseOperation(HIDE_ON_CLOSE);
		addWindowListener(new WindowAdapter()
		{
			@Override public void windowClosing(WindowEvent e)
			{
				pause();
			}
		});
		setLayout(new BorderLayout(12, 12));
		JPanel content = new JPanel(new BorderLayout(12, 12));
		content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
		content.add(createBookmarkControls(), BorderLayout.WEST);
		content.add(boards, BorderLayout.CENTER);
		content.add(createPlaybackControls(), BorderLayout.SOUTH);
		add(content);
		bookmarks.setSelectedIndex(0);
		showBoards();
		updateControls();
		pack();
		setMinimumSize(new Dimension(800, 480));
		setLocationRelativeTo(owner);
	}

	private JPanel createBookmarkControls()
	{
		JPanel sidebar = new JPanel(new BorderLayout(6, 8));
		sidebar.setBorder(BorderFactory.createTitledBorder("북마크"));
		bookmarks.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		bookmarks.addListSelectionListener(e -> updateControls());
		JScrollPane list = new JScrollPane(bookmarks);
		list.setPreferredSize(new Dimension(220, 240));
		sidebar.add(list, BorderLayout.CENTER);

		JPanel controls = new JPanel(new GridLayout(0, 1, 0, 6));
		JLabel nameLabel = new JLabel("북마크 이름");
		nameLabel.setLabelFor(bookmarkName);
		controls.add(nameLabel);
		controls.add(bookmarkName);
		JPanel sourceRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		JLabel sourceLabel = new JLabel("저장할 보드  ");
		sourceLabel.setLabelFor(source);
		sourceRow.add(sourceLabel);
		sourceRow.add(source);
		controls.add(sourceRow);
		controls.add(new JButton(save));
		controls.add(new JButton(compare));
		controls.add(new JButton(delete));
		sidebar.add(controls, BorderLayout.SOUTH);
		return sidebar;
	}

	private JPanel createPlaybackControls()
	{
		JPanel footer = new JPanel(new BorderLayout(8, 6));
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		buttons.add(new JButton(play));
		buttons.add(new JButton(pause));
		buttons.add(new JButton(step));
		buttons.add(new JLabel("정지 중 각 보드를 클릭해 편집하세요."));
		footer.add(buttons, BorderLayout.NORTH);
		footer.add(status, BorderLayout.SOUTH);
		return footer;
	}

	private void saveBookmark()
	{
		try
		{
			Bookmark bookmark = session.bookmark(bookmarkName.getText(),
					(BranchSession.Side)source.getSelectedItem());
			repository.add(bookmark);
			bookmarkItems.addElement(bookmark);
			bookmarks.setSelectedValue(bookmark, true);
			bookmarkName.setText("");
		}
		catch (IllegalArgumentException invalid)
		{
			JOptionPane.showMessageDialog(this, invalid.getMessage(), "북마크 저장", JOptionPane.WARNING_MESSAGE);
		}
	}

	private void compareSelected()
	{
		Bookmark selected = bookmarks.getSelectedValue();
		if (selected == null)
			return;
		session.compare(selected);
		showBoards();
	}

	private void deleteSelected()
	{
		Bookmark selected = bookmarks.getSelectedValue();
		if (selected == null)
			return;
		repository.remove(selected);
		bookmarkItems.removeElement(selected);
		updateControls();
	}

	private void showBoards()
	{
		detachBoards();
		boards.removeAll();
		left = new BoardPane(BranchSession.Side.A);
		right = new BoardPane(BranchSession.Side.B);
		boards.add(left);
		boards.add(right);
		boards.revalidate();
		boards.repaint();
		updateControls();
	}

	private void detachBoards()
	{
		if (left != null)
			left.detach();
		if (right != null)
			right.detach();
	}

	private void pause()
	{
		timer.stop();
		updateControls();
	}

	private void updateControls()
	{
		boolean running = timer.isRunning();
		boolean selected = bookmarks.getSelectedValue() != null;
		play.setEnabled(!running);
		pause.setEnabled(running);
		step.setEnabled(!running);
		save.setEnabled(!running);
		compare.setEnabled(!running && selected);
		delete.setEnabled(!running && selected);
		bookmarks.setEnabled(!running);
		bookmarkName.setEnabled(!running);
		source.setEnabled(!running);
		status.setText("출발 북마크: " + session.origin() + "  |  "
				+ (running ? "동시 진행 중 · 150ms/세대" : "정지")
				+ "  |  북마크는 프로그램 실행 중에 보관됩니다.");
	}

	/** Hiding retains bookmarks, but must never leave an invisible simulation running. */
	@Override public void setVisible(boolean visible)
	{
		if (!visible && timer != null)
			pause();
		super.setVisible(visible);
	}

	@Override public void dispose()
	{
		if (timer != null)
			timer.stop();
		detachBoards();
		super.dispose();
	}

	private static Action command(String name, Runnable operation)
	{
		return new AbstractAction(name)
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				operation.run();
			}
		};
	}

	/** Owns and detaches both subscriptions when a branch is replaced. */
	private final class BoardPane extends JPanel implements BoardListener
	{
		private final BoardView board;
		private final UniversePanel panel;
		private final JLabel generation = new JLabel();

		BoardPane(BranchSession.Side side)
		{
			super(new BorderLayout(0, 6));
			board = session.board(side);
			setBorder(BorderFactory.createTitledBorder("분기 " + side));
			panel = new UniversePanel(board, 5);
			panel.setCellClickListener((row, col) ->
			{
				if (!timer.isRunning())
					session.toggle(side, row, col);
			});
			JScrollPane scroll = new JScrollPane(panel);
			scroll.setPreferredSize(new Dimension(340, 340));
			add(scroll, BorderLayout.CENTER);
			add(generation, BorderLayout.SOUTH);
			board.addListener(this);
			updateGeneration();
		}

		private void updateGeneration()
		{
			generation.setText(board.generation() + "세대");
		}

		@Override public void stepped(long current, Set<Coord> changed)
		{
			updateGeneration();
		}

		@Override public void edited(BoardEdit applied)
		{
			updateGeneration();
		}

		@Override public void reset()
		{
			updateGeneration();
		}

		void detach()
		{
			board.removeListener(panel);
			board.removeListener(this);
			panel.setCellClickListener((row, col) -> {});
		}
	}
}
