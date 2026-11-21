package com.holub.life.race;

/** A race view can subscribe to progress, the settled result, or both. */
interface RaceObserver
{
	default void raceAdvanced(GliderRace.Frame frame) {}

	default void raceFinished(GliderRace.Result result) {}
}
