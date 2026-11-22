package com.holub.life.pattern;

/** The five groups a known Life pattern belongs to. */
public enum PatternCategory
{
	STILL_LIFE ("Still Life",             "Still", "Same shape every generation"),
	OSCILLATOR ("Oscillator",             "Osc",   "Returns to its shape every period"),
	SPACESHIP  ("Spaceship",              "Ship",  "Repeats its shape while moving"),
	METHUSELAH ("Methuselah",             "Meth",  "Small seed that changes for long (seed only)"),
	GENERATOR  ("Gun · Puffer · Breeder", "Gun",   "Keeps making other patterns");

	private final String title;
	private final String shortName;
	private final String description;

	PatternCategory(String title, String shortName, String description)
	{
		this.title = title;
		this.shortName = shortName;
		this.description = description;
	}

	public String title()       { return title; }
	/** A one-word name for tight spaces, such as a counts line. */
	public String shortName()   { return shortName; }
	public String description() { return description; }
}
