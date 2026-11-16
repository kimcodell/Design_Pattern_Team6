package com.holub.life.model;

/** Counts how many cells computed their next state. Shared by all the
 *  cells of one board (passed along by the Prototype's create()), so
 *  tests can check that quiet blocks are really skipped.
 */
final class EvaluationCounter
{
	private long count = 0;

	void increment()
	{
		++count;
	}

	long count()
	{
		return count;
	}
}
