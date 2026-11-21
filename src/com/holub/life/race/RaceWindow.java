package com.holub.life.race;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.text.ParseException;
import java.util.random.RandomGenerator;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;

/** Modeless race mode with its own clock and chip balance. */
public final class RaceWindow extends JDialog implements RaceObserver
{
	private static final Color PANEL = new Color(0xF4F0E5);
	private static final Color INK = new Color(0x183531);
	private final GliderRace race = new GliderRace(RandomGenerator.getDefault());
	private final RaceTrackPanel track = new RaceTrackPanel();
	private final Timer timer = new Timer(GliderRace.TICK_MILLIS, e -> race.advance());
	private final JComboBox<Integer> racerCount = new JComboBox<>();
	private final JComboBox<Integer> pick = new JComboBox<>();
	private final SpinnerNumberModel stake = new SpinnerNumberModel(
			Long.valueOf(100), Long.valueOf(1), Long.valueOf(GliderRace.STARTING_CHIPS), Long.valueOf(1));
	private final JSpinner stakeInput = new JSpinner(stake);
	private final JButton startButton = new JButton("Start Race");
	private final JLabel balance = new JLabel();
	private final JLabel payout = new JLabel();
	private final JLabel message = new JLabel("Choose a glider and place your bet.");

	public RaceWindow(Window owner)
	{
		super(owner, "Glider Race", ModalityType.MODELESS);
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		setResizable(false);

		for (int count = GliderRace.MIN_RACERS; count <= GliderRace.MAX_RACERS; count++)
			racerCount.addItem(count);
		racerCount.setSelectedItem(4);

		JPanel header = new JPanel(new BorderLayout(12, 8));
		header.setBackground(PANEL);
		header.setBorder(BorderFactory.createEmptyBorder(12, 14, 8, 14));
		JLabel title = new JLabel("Glider Race");
		title.setFont(title.getFont().deriveFont(Font.BOLD, 21f));
		title.setForeground(INK);
		balance.setForeground(INK);
		balance.setFont(balance.getFont().deriveFont(Font.BOLD, 14f));
		JPanel heading = new JPanel(new BorderLayout());
		heading.setOpaque(false);
		heading.add(title, BorderLayout.WEST);
		heading.add(balance, BorderLayout.EAST);
		header.add(heading, BorderLayout.NORTH);
		header.add(createBetControls(), BorderLayout.CENTER);

		message.setFont(message.getFont().deriveFont(Font.BOLD, 14f));
		message.setForeground(INK);
		message.setOpaque(true);
		message.setBackground(PANEL);
		message.setBorder(BorderFactory.createEmptyBorder(11, 14, 12, 14));

		add(header, BorderLayout.NORTH);
		add(track, BorderLayout.CENTER);
		add(message, BorderLayout.SOUTH);
		getRootPane().setDefaultButton(startButton);

		race.addObserver(track);
		race.addObserver(this);
		resetLanes();
		updateBalance();

		racerCount.addActionListener(e ->
		{
			resetLanes();
			pack();
		});
		pick.addActionListener(e ->
		{
			if (pick.getSelectedItem() != null)
				track.showStartingLine(selectedCount(), selectedGlider());
		});
		startButton.addActionListener(e -> startRace());
		pack();
		setLocationRelativeTo(owner);
	}

	private JPanel createBetControls()
	{
		JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 2));
		controls.setOpaque(false);

		JLabel racersLabel = new JLabel("Gliders");
		racersLabel.setLabelFor(racerCount);
		controls.add(racersLabel);
		controls.add(racerCount);
		JLabel pickLabel = new JLabel("Your pick");
		pickLabel.setLabelFor(pick);
		controls.add(pickLabel);
		controls.add(pick);
		JLabel stakeLabel = new JLabel("Stake");
		stakeLabel.setLabelFor(stakeInput);
		((JSpinner.DefaultEditor)stakeInput.getEditor()).getTextField().setColumns(8);
		controls.add(stakeLabel);
		controls.add(stakeInput);
		controls.add(payout);
		controls.add(startButton);
		return controls;
	}

	private int selectedCount()
	{
		return (Integer)racerCount.getSelectedItem();
	}

	private int selectedGlider()
	{
		return (Integer)pick.getSelectedItem();
	}

	private void resetLanes()
	{
		int count = selectedCount();
		int previousPick = pick.getSelectedItem() == null ? 1 : selectedGlider();
		pick.removeAllItems();
		for (int glider = 1; glider <= count; glider++)
			pick.addItem(glider);
		pick.setSelectedItem(Math.min(previousPick, count));
		payout.setText(count + "× total payout");
		track.showStartingLine(count, selectedGlider());
	}

	private void startRace()
	{
		try
		{
			stakeInput.commitEdit();
			long selectedStake = ((Number)stakeInput.getValue()).longValue();
			race.start(selectedCount(), selectedGlider(), selectedStake);
			track.showStartingLine(selectedCount(), selectedGlider());
			updateBalance();
			setBetControlsEnabled(false);
			message.setText(String.format("Race in progress  ·  #%d, %,d chips", selectedGlider(), selectedStake));
			timer.start();
		}
		catch (ParseException e)
		{
			message.setText("Enter a valid whole number of chips.");
		}
		catch (IllegalArgumentException e)
		{
			message.setText(e.getMessage());
		}
	}

	private void setBetControlsEnabled(boolean enabled)
	{
		racerCount.setEnabled(enabled);
		pick.setEnabled(enabled);
		stakeInput.setEnabled(enabled);
		startButton.setEnabled(enabled);
	}

	private void updateBalance()
	{
		balance.setText(String.format("%,d chips", race.balance()));
	}

	private void updateStakeLimit()
	{
		long maxStake = Math.max(1L, race.balance());
		stake.setMaximum(maxStake);
		if (((Number)stake.getValue()).longValue() > maxStake)
			stake.setValue(maxStake);
	}

	@Override public void raceFinished(GliderRace.Result result)
	{
		timer.stop();
		updateBalance();
		updateStakeLimit();
		setBetControlsEnabled(result.balance() > 0);
		if (result.winner() == 0)
			message.setText(String.format("Dead heat. Your %,d-chip stake was returned.", result.stake()));
		else if (result.winner() == result.pick())
			message.setText(String.format("#%d wins! Payout: %,d chips.", result.winner(), result.payout()));
		else if (result.balance() == 0)
			message.setText(String.format("#%d wins. No chips left in this session.", result.winner()));
		else
			message.setText(String.format("#%d wins. Your #%d bet lost %,d chips.", result.winner(), result.pick(), result.stake()));
	}
}
