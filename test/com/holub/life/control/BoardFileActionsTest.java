package com.holub.life.control;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.holub.life.model.*;
import com.holub.life.storage.BoardFileIO;
import com.holub.life.storage.format.JavaSerializationFormat;

/**
 * B regression tests through load/store on the EDT, without opening dialogs.
 * Only UI interactions are replaced. Real models, files and the legacy
 * serializer exercise the observable results and failure behavior.
 */
@DisplayName("저장·불러오기 UI 동작 (창 없이 EDT에서 검사)")
class BoardFileActionsTest
{
	@TempDir Path directory;

	private LifeModel model;
	private BoardFileIO files;
	private RecordingClock clock;
	private BoardFileActions actions;
	private File selectedFile;
	private final List<String> buttons = new ArrayList<>();
	private final List<String> errors = new ArrayList<>();

	private static final class RecordingClock extends Clock
	{
		int stops;

		@Override public void stop()
		{
			assertTrue(SwingUtilities.isEventDispatchThread());
			++stops;
			super.stop();
		}
	}

	@BeforeEach
	void setUp() throws Exception
	{
		SwingUtilities.invokeAndWait(() -> {
			model = LifeModel.builder().build();
			files = new BoardFileIO(List.of(new JavaSerializationFormat(model.size())));
			clock = new RecordingClock();
			actions = new BoardFileActions(model, files, clock, button -> {
				assertTrue(SwingUtilities.isEventDispatchThread());
				buttons.add(button);
				return selectedFile;
			}, message -> {
				assertTrue(SwingUtilities.isEventDispatchThread());
				errors.add(message);
			});
		});
	}

	@Test
	@DisplayName("Store는 현재 배치를 기존 .life 형식으로 저장한다")
	void storesCurrentBoard() throws Exception
	{
		selectedFile = directory.resolve("saved.life").toFile();
		SwingUtilities.invokeAndWait(() -> {
			model.toggle(3, 7);
			actions.store();
			assertEquals(List.of("Write"), buttons);
			assertEquals(1, clock.stops);
			assertTrue(errors.isEmpty());
		});
		assertEquals(new BoardSnapshot(64, Set.of(new Coord(3, 7))), files.read(selectedFile));
	}

	@Test
	@DisplayName("Load는 기존 배치를 교체하고 복원 알림을 보낸다")
	void loadsAndNotifiesView() throws Exception
	{
		selectedFile = directory.resolve("loaded.life").toFile();
		BoardSnapshot saved = new BoardSnapshot(64, Set.of(new Coord(5, 9)));
		files.write(selectedFile, saved);
		SwingUtilities.invokeAndWait(() -> {
			model.toggle(1, 2);
			model.step();
			List<String> notifications = new ArrayList<>();
			model.addListener(new BoardListener() {
				@Override public void reset() { notifications.add("reset"); }
			});
			actions.load();
			assertEquals(saved, model.snapshot());
			assertEquals(0, model.generation());
			assertEquals(List.of("reset"), notifications);
			assertEquals(List.of("Load"), buttons);
			assertEquals(1, clock.stops);
			assertTrue(errors.isEmpty());
		});
	}

	@ParameterizedTest
	@ValueSource(strings = {"Load", "Store"})
	@DisplayName("파일 선택 취소는 배치·세대를 유지하고 시계를 정지하지 않는다")
	void cancellationLeavesStateUntouched(String operation) throws Exception
	{
		SwingUtilities.invokeAndWait(() -> {
			model.step();
			model.toggle(2, 4);
			BoardSnapshot before = model.snapshot();
			long generation = model.generation();
			selectedFile = null;
			if (operation.equals("Load"))
				actions.load();
			else
				actions.store();
			assertEquals(before, model.snapshot());
			assertEquals(generation, model.generation());
			assertEquals(0, clock.stops);
			assertTrue(errors.isEmpty());
		});
	}

	@Test
	@DisplayName("깨진 파일을 불러오면 오류를 전달하고 기존 보드를 유지한다")
	void corruptLoadPreservesBoard() throws Exception
	{
		selectedFile = directory.resolve("broken.life").toFile();
		java.nio.file.Files.writeString(selectedFile.toPath(), "not a life file");
		assertLoadFailsWithoutChangingBoard();
	}

	@Test
	@DisplayName("없는 파일을 불러오면 오류를 전달하고 기존 보드를 유지한다")
	void missingLoadPreservesBoard() throws Exception
	{
		selectedFile = directory.resolve("missing.life").toFile();
		assertLoadFailsWithoutChangingBoard();
	}

	@Test
	@DisplayName("보드 밖 좌표가 든 파일은 복원 전에 거부된다")
	void invalidCoordinatesPreserveBoard() throws Exception
	{
		selectedFile = directory.resolve("outside.life").toFile();
		files.write(selectedFile, new BoardSnapshot(64, Set.of(new Coord(64, 0))));
		assertLoadFailsWithoutChangingBoard();
	}

	private void assertLoadFailsWithoutChangingBoard() throws Exception
	{
		SwingUtilities.invokeAndWait(() -> {
			model.step();
			model.toggle(2, 4);
			BoardSnapshot before = model.snapshot();
			long generation = model.generation();
			actions.load();
			assertEquals(before, model.snapshot());
			assertEquals(generation, model.generation());
			assertEquals(1, clock.stops);
			assertEquals(List.of("Read Failed!"), errors);
		});
	}

	@Test
	@DisplayName("저장 실패는 오류를 전달하고 보드 상태를 유지한다")
	void writeFailurePreservesBoard() throws Exception
	{
		selectedFile = directory.resolve("directory.life").toFile();
		java.nio.file.Files.createDirectory(selectedFile.toPath());
		SwingUtilities.invokeAndWait(() -> {
			model.step();
			model.toggle(2, 4);
			BoardSnapshot before = model.snapshot();
			long generation = model.generation();
			actions.store();
			assertEquals(before, model.snapshot());
			assertEquals(generation, model.generation());
			assertEquals(1, clock.stops);
			assertEquals(List.of("Write Failed!"), errors);
		});
	}
}
