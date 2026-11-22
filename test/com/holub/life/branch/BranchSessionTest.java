package com.holub.life.branch;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import com.holub.life.model.*;

@Tag("new-feature")
@DisplayName("북마크·분기 비교")
class BranchSessionTest
{
	private LifeModel blinker()
	{
		LifeModel model = LifeModel.builder().size(16).rule(Rule.parse("B36/S23"))
				.boundary(Boundary.TORUS).build();
		model.apply(BoardEdit.set(Set.of(new Coord(7, 6), new Coord(7, 7), new Coord(7, 8)), true));
		model.step();
		model.step();
		return model;
	}

	/** Saving captures the instant, even if the live source is subsequently cleared. */
	@Test @DisplayName("북마크는 저장 당시의 배치·세대·규칙·경계를 유지한다")
	void bookmarkSurvivesChangesToItsSource()
	{
		LifeModel original = blinker();
		BoardSnapshot expected = original.snapshot();
		Bookmark bookmark = new Bookmark("  충돌 직전  ", original);
		original.clear();
		LifeModel branch = bookmark.createBranch();
		assertEquals("충돌 직전", bookmark.name());
		assertEquals(expected, branch.snapshot());
		assertEquals(2, branch.generation());
		assertEquals(16, branch.size());
		assertEquals("B36/S23", branch.rule().toString());
		assertEquals(Boundary.TORUS, branch.boundary());
	}

	/** Editing and advancing A must not change B or the saved prototype. */
	@Test @DisplayName("분기 A 편집·진행은 B와 북마크에 영향을 주지 않는다")
	void independentBranches()
	{
		Bookmark bookmark = new Bookmark("시작", blinker());
		LifeModel a = bookmark.createBranch();
		LifeModel b = bookmark.createBranch();
		BoardSnapshot expected = b.snapshot();
		a.toggle(0, 0);
		a.step();
		assertEquals(expected, b.snapshot());
		assertEquals(2, b.generation());
		assertEquals(expected, bookmark.createBranch().snapshot());
		assertEquals(2, bookmark.generation());
	}

	/** The UI's side selection must route clicks to exactly one board. */
	@Test @DisplayName("A와 B를 각각 편집하고 같은 세대끼리 비교한다")
	void sessionRoutesEditsAndStepsTogether()
	{
		BranchSession session = new BranchSession(new Bookmark("시작", blinker()));
		session.toggle(BranchSession.Side.A, 0, 0);
		session.toggle(BranchSession.Side.B, 1, 1);
		assertTrue(session.board(BranchSession.Side.A).isAlive(0, 0));
		assertFalse(session.board(BranchSession.Side.B).isAlive(0, 0));
		assertTrue(session.board(BranchSession.Side.B).isAlive(1, 1));
		assertFalse(session.board(BranchSession.Side.A).isAlive(1, 1));
		session.stepBoth();
		assertEquals(3, session.board(BranchSession.Side.A).generation());
		assertEquals(3, session.board(BranchSession.Side.B).generation());
	}

	/** Unchanged boards still need a generation increment and an Observer notification. */
	@Test @DisplayName("빈 보드도 양쪽 모두 한 세대씩 진행하고 각 화면에 알린다")
	void emptyBoardsAdvanceAndNotifyIndependently()
	{
		BranchSession session = new BranchSession(new Bookmark("빈 보드", LifeModel.builder().build()));
		long[] events = new long[2];
		session.board(BranchSession.Side.A).addListener(new BoardListener()
		{
			@Override public void stepped(long generation, Set<Coord> changed)
			{
				events[0]++;
			}
		});
		session.board(BranchSession.Side.B).addListener(new BoardListener()
		{
			@Override public void stepped(long generation, Set<Coord> changed)
			{
				events[1]++;
			}
		});
		session.stepBoth();
		assertArrayEquals(new long[] { 1, 1 }, events);
		assertEquals(1, session.board(BranchSession.Side.A).generation());
		assertEquals(1, session.board(BranchSession.Side.B).generation());
	}

	/** Returning to a bookmark restores its generation instead of resetting it to zero. */
	@Test @DisplayName("북마크 재선택은 두 보드를 저장 당시 배치와 세대로 되돌린다")
	void selectingBookmarkRestartsBothBranches()
	{
		Bookmark bookmark = new Bookmark("12세대 전의 실험", blinker());
		BranchSession session = new BranchSession(bookmark);
		BoardView oldLeft = session.board(BranchSession.Side.A);
		session.toggle(BranchSession.Side.A, 0, 0);
		session.stepBoth();
		session.compare(bookmark);
		for (BranchSession.Side side : BranchSession.Side.values())
		{
			assertEquals(bookmark.createBranch().snapshot(), session.board(side).snapshot());
			assertEquals(2, session.board(side).generation());
		}
		assertNotSame(oldLeft, session.board(BranchSession.Side.A));
		assertEquals(bookmark.name(), session.origin());
	}

	/** A later bookmark may start from either branch, not just the original source. */
	@Test @DisplayName("B에서 새 북마크를 만들어 다시 A와 B로 분기한다")
	void bookmarkCanCaptureEitherBranch()
	{
		BranchSession session = new BranchSession(new Bookmark("처음", blinker()));
		session.stepBoth();
		session.toggle(BranchSession.Side.B, 0, 0);
		Bookmark next = session.bookmark("B의 발견", BranchSession.Side.B);
		BoardSnapshot expected = session.board(BranchSession.Side.B).snapshot();
		session.stepBoth();
		session.compare(next);
		assertEquals(3, session.board(BranchSession.Side.A).generation());
		assertEquals(expected, session.board(BranchSession.Side.A).snapshot());
		assertEquals(expected, session.board(BranchSession.Side.B).snapshot());
	}

	/** Invalid names must not create ambiguous list entries. */
	@Test @DisplayName("빈 이름과 중복 이름을 거부하고 기존 북마크를 유지한다")
	void validatesNamesAndKeepsExistingBookmarks()
	{
		LifeModel model = blinker();
		assertThrows(IllegalArgumentException.class, () -> new Bookmark("  ", model));
		BookmarkRepository repository = new BookmarkRepository();
		Bookmark first = new Bookmark("실험", model);
		repository.add(first);
		assertThrows(IllegalArgumentException.class, () -> repository.add(new Bookmark(" 실험 ", model)));
		assertEquals(List.of(first), repository.bookmarks());
		assertThrows(UnsupportedOperationException.class, () -> repository.bookmarks().clear());
	}

	/** Removing the list entry must leave already-created branches usable. */
	@Test @DisplayName("북마크 삭제 후에도 현재 비교는 계속 진행된다")
	void deletingBookmarkDoesNotDeleteActiveBranches()
	{
		BookmarkRepository repository = new BookmarkRepository();
		Bookmark saved = new Bookmark("시작", blinker());
		repository.add(saved);
		BranchSession session = new BranchSession(saved);
		repository.remove(saved);
		assertTrue(repository.bookmarks().isEmpty());
		session.stepBoth();
		assertEquals(3, session.board(BranchSession.Side.A).generation());
	}
}
