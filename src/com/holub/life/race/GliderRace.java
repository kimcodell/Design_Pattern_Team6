package com.holub.life.race;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

/** Runs one race at a time and owns the chip balance for the current session. */
final class GliderRace
{
	static final int MIN_RACERS = 2;
	static final int MAX_RACERS = 6;
	static final int TICK_MILLIS = 100;
	static final int FINISH_GENERATION = 144;
	static final int STARTING_CHIPS = 1000;

	private static final double TIE_TOLERANCE = 1e-9;

	record Frame(List<Double> progress)
	{
		Frame
		{
			progress = List.copyOf(progress);
		}
	}

	/** A winner number of zero means a dead heat and the stake is returned. */
	record Result(int winner, int pick, long stake, long payout, long balance) {}

	record Finish(int winner, double fraction) {}

	private record Bet(int pick, long stake) {}

	private final RandomGenerator random;
	private final List<RaceObserver> observers = new ArrayList<>();
	private List<RacePace> racers = List.of();
	private Bet activeBet;
	private long balance = STARTING_CHIPS;

	GliderRace(RandomGenerator random)
	{
		this.random = Objects.requireNonNull(random);
	}

	void addObserver(RaceObserver observer)
	{
		observers.add(Objects.requireNonNull(observer));
	}

	long balance()
	{
		return balance;
	}

	boolean isRunning()
	{
		return activeBet != null;
	}

	void start(int racerCount, int pick, long stake)
	{
		if (isRunning())
			throw new IllegalStateException("A race is already running.");
		if (racerCount < MIN_RACERS || racerCount > MAX_RACERS)
			throw new IllegalArgumentException("Choose 2 to 6 gliders.");
		if (pick < 1 || pick > racerCount)
			throw new IllegalArgumentException("Choose a glider in the race.");
		if (stake < 1 || stake > balance)
			throw new IllegalArgumentException("Your stake must be between 1 and your chip balance.");
		if (stake > (Long.MAX_VALUE - balance) / (racerCount - 1))
			throw new IllegalArgumentException("Choose a smaller stake.");

		racers = new ArrayList<>(racerCount);
		for (int racer = 0; racer < racerCount; racer++)
			racers.add(new RacePace(random.nextLong()));

		activeBet = new Bet(pick, stake);
		balance -= stake;
		publishFrame(new Frame(Collections.nCopies(racerCount, 0.0)));
	}

	void advance()
	{
		if (!isRunning())
			return;

		double[] previous = new double[racers.size()];
		double[] next = new double[racers.size()];
		for (int index = 0; index < racers.size(); index++)
		{
			RacePace racer = racers.get(index);
			previous[index] = racer.progress();
			racer.advance();
			next[index] = racer.progress();
		}

		Finish finish = findFinish(previous, next);
		if (finish == null)
		{
			publishFrame(frameAt(previous, next, 1.0));
			return;
		}

		Result result = settle(finish.winner());
		publishFrame(frameAt(previous, next, finish.fraction()));
		for (RaceObserver observer : observers)
			observer.raceFinished(result);
	}

	private void publishFrame(Frame frame)
	{
		for (RaceObserver observer : observers)
			observer.raceAdvanced(frame);
	}

	private Result settle(int winner)
	{
		long payout = 0;
		if (winner == 0)
			payout = activeBet.stake();
		else if (winner == activeBet.pick())
			payout = activeBet.stake() * racers.size();

		balance += payout;
		Result result = new Result(winner, activeBet.pick(), activeBet.stake(), payout, balance);
		activeBet = null;
		return result;
	}

	private static Frame frameAt(double[] previous, double[] next, double fraction)
	{
		List<Double> progress = new ArrayList<>(previous.length);
		for (int index = 0; index < previous.length; index++)
		{
			double position = previous[index] + (next[index] - previous[index]) * fraction;
			progress.add(Math.min(position, FINISH_GENERATION));
		}
		return new Frame(progress);
	}

	/** Compare crossing times inside the same tick, independent of lane order. */
	static Finish findFinish(double[] previous, double[] next)
	{
		double firstFraction = Double.POSITIVE_INFINITY;
		int winner = 0;
		for (int index = 0; index < next.length; index++)
		{
			if (next[index] < FINISH_GENERATION)
				continue;

			double fraction = (FINISH_GENERATION - previous[index]) / (next[index] - previous[index]);
			if (fraction < firstFraction - TIE_TOLERANCE)
			{
				firstFraction = fraction;
				winner = index + 1;
			}
			else if (Math.abs(fraction - firstFraction) <= TIE_TOLERANCE)
			{
				firstFraction = Math.min(fraction, firstFraction);
				winner = 0;
			}
		}
		return Double.isFinite(firstFraction) ? new Finish(winner, firstFraction) : null;
	}
}
