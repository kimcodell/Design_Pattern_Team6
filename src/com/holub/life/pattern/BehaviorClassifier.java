package com.holub.life.pattern;

import java.util.*;
import com.holub.life.model.*;

/** Groups clusters that are not in the catalog by running each one on
 *  its own (with {@link SparseLife}) for up to {@value #GENERATIONS}
 *  generations: the same shape in the same place means a still life
 *  or an oscillator, the same shape elsewhere means a spaceship.
 *  Clusters that do neither are left for the next stage.
 *
 *  <p>The verdict depends only on the shape and the rule, so it is
 *  remembered; debris tends to repeat the same few shapes.
 */
final class BehaviorClassifier implements DetectionStage
{
	static final int GENERATIONS = 60;
	private static final int GIVE_UP_POPULATION = 400;
	private static final int CACHE_SIZE = 2_000;

	private record Verdict(PatternCategory category, int period) {}
	private static final Verdict NONE = new Verdict(null, 0);

	private Rule rule = Rule.CONWAY;
	private final Map<String, Verdict> cache = new LinkedHashMap<>(256, 0.75f, true)
	{
		@Override protected boolean removeEldestEntry(Map.Entry<String, Verdict> eldest)
		{
			return size() > CACHE_SIZE;
		}
	};

	@Override public boolean appliesTo(BoardView board)
	{
		if (!board.rule().equals(rule))
		{	rule = board.rule();
			cache.clear();
		}
		return true;
	}

	@Override public List<Detection> detect(CellClaims claims)
	{
		List<Detection> found = new ArrayList<>();
		for (CellClaims.Cluster cluster : claims.unclaimedClusters())
		{
			Shape shape = new Shape(cluster.unwrapped());
			Verdict verdict = cache.computeIfAbsent(shape.key(), key -> run(shape.normalized()));
			if (verdict == NONE)
				continue;
			cluster.cells().forEach(claims::claim);
			found.add(new Detection(nameOf(verdict), verdict.category(), Detection.Source.BEHAVIOR,
									verdict.period(), shape.top(), shape.left(),
									shape.height(), shape.width(), cluster.cells()));
		}
		return found;
	}

	private Verdict run(Shape start)
	{
		Set<Coord> state = start.cells();
		String startKey = start.key();
		for (int generation = 1; generation <= GENERATIONS; ++generation)
		{
			state = SparseLife.step(state, rule);
			if (state.isEmpty() || state.size() > GIVE_UP_POPULATION)
				return NONE;
			Shape now = new Shape(state);
			if (now.key().equals(startKey))
			{
				boolean moved = now.top() != 0 || now.left() != 0;
				if (moved)
					return new Verdict(PatternCategory.SPACESHIP, generation);
				return generation == 1 ? new Verdict(PatternCategory.STILL_LIFE, 1)
									   : new Verdict(PatternCategory.OSCILLATOR, generation);
			}
		}
		return NONE;
	}

	private static String nameOf(Verdict v)
	{
		return switch (v.category())
		{
			case STILL_LIFE -> "Unlisted still life";
			case OSCILLATOR -> "Unlisted oscillator (p" + v.period() + ")";
			case SPACESHIP  -> "Unlisted spaceship (p" + v.period() + ")";
			default         -> throw new AssertionError(v);
		};
	}
}
