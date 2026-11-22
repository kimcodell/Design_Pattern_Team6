package com.holub.life.branch;

import java.util.Objects;
import com.holub.life.model.LifeModel;

/** A named, private Prototype. Every branch receives an independent copy. */
public final class Bookmark
{
	private final String name;
	private final LifeModel prototype;

	public Bookmark(String name, LifeModel source)
	{
		this.name = Objects.requireNonNull(name, "name").strip();
		if (this.name.isEmpty())
			throw new IllegalArgumentException("북마크 이름을 입력하세요.");
		prototype = Objects.requireNonNull(source, "source").copy();
	}

	public String name()
	{
		return name;
	}

	public long generation()
	{
		return prototype.generation();
	}

	public LifeModel createBranch()
	{
		return prototype.copy();
	}

	@Override public String toString()
	{
		return name + " (" + generation() + "세대)";
	}
}
