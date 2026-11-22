package com.holub.life.branch;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Bookmarks for one comparison window, in insertion order. */
public final class BookmarkRepository
{
	private final List<Bookmark> bookmarks = new ArrayList<>();

	public void add(Bookmark bookmark)
	{
		Objects.requireNonNull(bookmark, "bookmark");
		if (bookmarks.stream().anyMatch(saved -> saved.name().equals(bookmark.name())))
			throw new IllegalArgumentException("이미 사용 중인 북마크 이름입니다.");
		bookmarks.add(bookmark);
	}

	public List<Bookmark> bookmarks()
	{
		return List.copyOf(bookmarks);
	}

	public void remove(Bookmark bookmark)
	{
		bookmarks.remove(bookmark);
	}
}
