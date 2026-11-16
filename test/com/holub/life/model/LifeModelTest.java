package com.holub.life.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.*;

/** 모델의 입구 LifeModel을 화면 없이 단독으로 시험한다. 이 클래스의
 *  테스트는 Swing 창을 하나도 만들지 않는다(GUI 없는 환경에서 실행).
 */
@DisplayName("LifeModel 입구 (화면 없이 단독 테스트)")
class LifeModelTest
{
	/** 모델이 보내는 알림을 문자열로 기록하는 테스트용 관찰자. */
	static final class Recorder implements BoardListener
	{
		final List<String> events = new ArrayList<>();
		final List<Set<Coord>> changed = new ArrayList<>();
		@Override public void stepped(long generation, Set<Coord> cells)
		{   events.add("stepped " + generation); changed.add(Set.copyOf(cells)); }
		@Override public void edited(BoardEdit applied) { events.add("edited " + applied.cells()); }
		@Override public void reset() { events.add("reset"); }
	}

	private static Coord at(int row, int col) { return new Coord(row, col); }

	/** 목적: P2(Singleton과 생성 사슬 때문에 창 없이는 모델을 만들 수 없던 결함)
	 *  수정 확인. 원본은 ExceptionInInitializerError가 났다.
	 *  방법: GUI 없는 환경에서 builder로 모델을 만든다.
	 *  기대: 64×64 빈 보드, 0세대.
	 */
	@Test @Tag("design")
	@DisplayName("[구조 P2] 창 없이 모델만 만들 수 있다")
	void newBoardIsEmptyAndCanBeBuiltWithoutAGui()
	{
		LifeModel model = LifeModel.builder().build();
		assertEquals(64, model.size());
		assertEquals(0, model.generation());
		assertTrue(model.snapshot().liveCells().isEmpty());
	}

	/** 목적: 원본의 클릭 동작(셀 뒤집기)이 유지되는지 확인한다.
	 *  방법: 같은 셀을 두 번 toggle한다.
	 *  기대: 켜졌다가 꺼지고, 알림은 바뀐 셀 하나씩 두 번 온다.
	 */
	@Test @Tag("existing")
	@DisplayName("[기존 기능] 클릭(toggle)은 셀을 뒤집고 알림을 한 번 보낸다")
	void toggleFlipsACellAndNotifiesOnce()
	{
		LifeModel model = LifeModel.builder().build();
		Recorder recorder = new Recorder();
		model.addListener(recorder);

		model.toggle(3, 4);
		assertTrue(model.isAlive(3, 4));
		model.toggle(3, 4);
		assertFalse(model.isAlive(3, 4));
		assertEquals(List.of("edited {Coord[row=3, col=4]=true}",
							 "edited {Coord[row=3, col=4]=false}"), recorder.events);
	}

	/** 목적: 원본의 "지우기"와 "불러오기"가 유지되고, 새로 생긴 세대 번호가
	 *  초기화되는지 확인한다.
	 *  방법: 블링커를 두 세대 돌린 뒤 clear, 다시 한 세대 뒤 restore한다.
	 *  기대: 보드가 비거나 스냅샷대로 돌아오고, 세대는 0, 알림은 reset이다.
	 */
	@Test @Tag("existing")
	@DisplayName("[기존 기능] 지우기와 불러오기는 보드를 바꾸고 세대를 0으로 돌린다")
	void clearAndRestoreResetTheGeneration()
	{
		LifeModel model = LifeModel.builder().build();
		model.apply(BoardEdit.set(List.of(at(10, 9), at(10, 10), at(10, 11)), true));
		BoardSnapshot snapshot = model.snapshot();
		model.step();
		model.step();
		Recorder recorder = new Recorder();
		model.addListener(recorder);

		model.clear();
		assertEquals(0, model.generation());
		assertTrue(model.snapshot().liveCells().isEmpty());

		model.step();
		model.restore(snapshot);
		assertEquals(0, model.generation());
		assertEquals(snapshot, model.snapshot());
		assertEquals(List.of("reset", "stepped 1", "reset"), recorder.events);
	}

	/** 목적: 새 기능 apply(BoardEdit)가 되돌리기 재료(역편집)를 돌려주는지 보인다.
	 *  도장 찍기의 실행 취소(Command)를 이 값으로 만든다.
	 *  방법: 셀 하나가 켜진 보드에 세 셀을 켜는 편집을 적용한 뒤 역편집을 적용한다.
	 *  기대: 역편집에는 실제로 바뀐 두 셀만 있고, 적용하면 원래 보드로 돌아온다.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] apply는 되돌리기용 역편집을 돌려주고, 역편집으로 원상 복구된다")
	void applyReturnsAnInverseThatUndoesTheEdit()
	{
		LifeModel model = LifeModel.builder().build();
		model.toggle(1, 1);
		BoardSnapshot before = model.snapshot();

		BoardEdit inverse = model.apply(BoardEdit.set(List.of(at(1, 1), at(1, 2), at(1, 3)), true));
		assertEquals(Map.of(at(1, 2), false, at(1, 3), false), inverse.cells(),
					 "only cells that changed appear in the inverse");

		model.apply(inverse);
		assertEquals(before, model.snapshot());
	}

	/** 목적: 여러 셀을 한 번에 바꿔도 알림이 한 번만, 바뀐 셀만 담겨 오는지 확인
	 *  (도장처럼 많은 셀을 찍을 때 화면이 수십 번 다시 그려지지 않게).
	 *  방법: 이미 켜진 셀이 섞인 편집과, 아무것도 바뀌지 않는 편집을 적용한다.
	 *  기대: 첫 편집은 알림 1번(바뀐 셀 1개), 두 번째는 알림 없음.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] apply는 알림을 한 번만, 실제로 바뀐 셀만 담아 보낸다")
	void applyNotifiesOnceWithOnlyTheChangedCells()
	{
		LifeModel model = LifeModel.builder().build();
		model.toggle(0, 0);
		Recorder recorder = new Recorder();
		model.addListener(recorder);

		model.apply(BoardEdit.set(List.of(at(0, 0), at(0, 1)), true));
		assertEquals(List.of("edited {Coord[row=0, col=1]=true}"), recorder.events);

		model.apply(BoardEdit.set(List.of(at(0, 0)), true));
		assertEquals(1, recorder.events.size(), "no notification when nothing changes");
	}

	/** 목적: 보드 밖 좌표가 섞인 편집은 일부만 적용되지 않고 통째로 거부되는지 확인.
	 *  방법: 보드 안 셀과 보드 밖 셀(64, 0)을 함께 적용한다.
	 *  기대: 예외가 나고, 보드 안 셀도 바뀌지 않는다.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] 보드 밖 셀이 섞인 편집은 아무것도 바꾸지 않고 거부된다")
	void applyOffTheBoardChangesNothing()
	{
		LifeModel model = LifeModel.builder().build();
		BoardEdit edit = BoardEdit.set(List.of(at(0, 0), at(64, 0)), true);
		assertThrows(IllegalArgumentException.class, () -> model.apply(edit));
		assertFalse(model.isAlive(0, 0), "the valid cell was not applied either");
	}

	/** 목적: 한 세대 진행 알림에 세대 번호와 "바뀐 셀"이 정확히 담기는지 확인.
	 *  화면은 이 정보로 바뀐 셀만 다시 그리고, 나이 추적기는 나이를 센다.
	 *  방법: 가로 블링커를 한 세대 진행한다.
	 *  기대: 세대 1, 바뀐 셀은 꺼진 양 끝 2개와 켜진 위아래 2개.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] 한 세대 알림에 세대 번호와 바뀐 셀이 정확히 담긴다")
	void stepReportsGenerationAndExactlyTheChangedCells()
	{
		LifeModel model = LifeModel.builder().build();
		model.apply(BoardEdit.set(List.of(at(10, 9), at(10, 10), at(10, 11)), true));
		Recorder recorder = new Recorder();
		model.addListener(recorder);

		assertTrue(model.step());
		assertEquals(1, model.generation());
		assertEquals(List.of("stepped 1"), recorder.events);
		assertEquals(Set.of(at(10, 9), at(10, 11), at(9, 10), at(11, 10)), recorder.changed.get(0));
	}

	/** 목적: 아무것도 바뀌지 않아도 세대는 진행됨을 확인(세대 번호의 의미 고정).
	 *  방법: 2×2 정지 패턴을 두 세대 진행한다.
	 *  기대: step()은 false, 세대는 2, 바뀐 셀은 없음.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] 정지한 보드도 세대는 늘고 바뀐 셀은 비어 있다")
	void stepOnAStillBoardStillAdvancesTheGeneration()
	{
		LifeModel model = LifeModel.builder().build();
		model.apply(BoardEdit.set(List.of(at(5, 5), at(5, 6), at(6, 5), at(6, 6)), true));
		model.step();
		Recorder recorder = new Recorder();
		model.addListener(recorder);

		assertFalse(model.step());
		assertEquals(2, model.generation());
		assertEquals(Set.of(), recorder.changed.get(0));
	}

	/** 목적: 불러오기 검증의 모델 쪽 단계(크기·범위)가 동작하는지 확인.
	 *  방법: 크기가 다른 스냅샷과 보드 밖 셀이 있는 스냅샷을 restore한다.
	 *  기대: 둘 다 거부되고 보드는 그대로다.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] 크기가 다르거나 보드 밖 셀이 있는 스냅샷은 거부된다")
	void restoreRejectsAWrongSizeOrCellsOffTheBoard()
	{
		LifeModel model = LifeModel.builder().build();
		model.toggle(2, 2);
		BoardSnapshot before = model.snapshot();

		assertThrows(IllegalArgumentException.class,
					 () -> model.restore(new BoardSnapshot(32, Set.of())));
		assertThrows(IllegalArgumentException.class,
					 () -> model.restore(new BoardSnapshot(64, Set.of(at(0, 0), at(70, 1)))));
		assertEquals(before, model.snapshot(), "a rejected restore leaves the board unchanged");
	}

	/** 목적: 관찰자 해제가 동작하는지 확인(분기 전환 때 화면을 떼어 내는 데 필요).
	 *  방법: 등록 후 해제하고 셀을 바꾼다.
	 *  기대: 알림이 오지 않는다.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] 해제한 관찰자에게는 알림이 가지 않는다")
	void removedListenerIsNotNotified()
	{
		LifeModel model = LifeModel.builder().build();
		Recorder recorder = new Recorder();
		model.addListener(recorder);
		model.removeListener(recorder);
		model.toggle(0, 0);
		assertTrue(recorder.events.isEmpty());
	}

	/** 목적: 스냅샷이 불변 복사본이라 바깥에 내줘도 모델이 오염되지 않음을 확인.
	 *  방법: 스냅샷을 고치려 하고, 모델을 바꾼 뒤 스냅샷을 다시 본다.
	 *  기대: 수정은 거부되고, 스냅샷 내용은 찍은 시점 그대로다.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] 스냅샷은 불변 복사본이라 이후 변경이 새어 들지 않는다")
	void snapshotsAreImmutable()
	{
		LifeModel model = LifeModel.builder().build();
		model.toggle(0, 0);
		BoardSnapshot snapshot = model.snapshot();
		assertThrows(UnsupportedOperationException.class, () -> snapshot.liveCells().add(at(1, 1)));
		model.toggle(0, 0);
		assertEquals(Set.of(at(0, 0)), snapshot.liveCells(), "later changes do not leak into a snapshot");
	}
}
