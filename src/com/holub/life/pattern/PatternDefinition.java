package com.holub.life.pattern;

import java.util.Objects;

/** One entry of a {@link PatternCatalog}: a named pattern, its group
 *  and its starting shape.
 *
 *  @param period generations until the pattern repeats: 1 for a still
 *                life, 0 if it never repeats (a methuselah).
 *  @param note   a short human-readable fact, such as a speed.
 */
public record PatternDefinition(String name, PatternCategory category, Shape seed,
								int period, String note)
{
	public PatternDefinition
	{
		Objects.requireNonNull(name, "name");
		Objects.requireNonNull(category, "category");
		Objects.requireNonNull(seed, "seed");
		Objects.requireNonNull(note, "note");
		if (seed.isEmpty())
			throw new IllegalArgumentException("empty pattern: " + name);
		if (period < 0)
			throw new IllegalArgumentException("negative period: " + name);
		seed = seed.normalized();
	}
}
