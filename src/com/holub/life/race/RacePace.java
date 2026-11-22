package com.holub.life.race;

import java.util.Random;

/** The same pace and fatigue rules apply to every glider. */
final class RacePace
{
	private enum Effort
	{
		EASY(0.30), STEADY(0.60), SPRINT(1.00);

		private final double effort;

		Effort(double effort)
		{
			this.effort = effort;
		}
	}

	private static final double TICK_SECONDS = GliderRace.TICK_MILLIS / 1000.0;
	private final Random random;
	private double progress;
	private double speed = 10.0;
	private double stamina = 100.0;
	private Effort pace = Effort.STEADY;
	private int elapsedTicks;
	private int nextPaceChange;

	RacePace(long seed)
	{
		random = new Random(seed);
		nextPaceChange = random.nextInt(4, 11);
	}

	double progress()
	{
		return progress;
	}

	void advance()
	{
		elapsedTicks++;
		if (elapsedTicks == nextPaceChange)
		{
			pace = choosePace();
			nextPaceChange += random.nextInt(6, 16);
		}

		// Sprinting spends more stamina. Below 40, fatigue limits acceleration.
		double effort = pace.effort;
		stamina = Math.max(0.0, stamina - TICK_SECONDS * (2.0 + 20.0 * effort * effort));
		double staminaFactor = 0.42 + 0.58 * Math.min(stamina / 40.0, 1.0);
		double targetSpeed = 8.0 + 16.0 * effort * staminaFactor;
		speed += 0.28 * (targetSpeed - speed);
		progress += speed * TICK_SECONDS;
	}

	private Effort choosePace()
	{
		double raceProgress = progress / GliderRace.FINISH_GENERATION;
		double easyLimit;
		double sprintLimit;
		if (raceProgress < 0.20)
		{
			easyLimit = 0.25;
			sprintLimit = 0.80;
		}
		else if (raceProgress < 0.72)
		{
			easyLimit = 0.36;
			sprintLimit = 0.82;
		}
		else
		{
			easyLimit = 0.15;
			sprintLimit = 0.55;
		}

		double choice = random.nextDouble();
		if (choice < easyLimit)
			return Effort.EASY;
		if (choice >= sprintLimit)
			return Effort.SPRINT;
		return Effort.STEADY;
	}
}
