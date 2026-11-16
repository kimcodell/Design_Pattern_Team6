package com.holub.life.model;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.junit.jupiter.api.*;

/** 모델과 UI의 분리(MVC)가 규칙이 아니라 코드로 보장되는지 확인한다. */
@DisplayName("모델·UI 분리 보장")
class ModelPackageTest
{
	private static final Path MODEL = Paths.get("src/com/holub/life/model");

	/** 목적: 모델이 화면 라이브러리를 전혀 모른다는 것을 보인다(원본은 셀이
	 *  java.awt로 스스로 그렸다: P4, M1).
	 *  방법: 모델 패키지의 모든 소스에서 import 줄을 읽는다.
	 *  기대: java.awt를 제외한 java.* 와 모델 패키지만 import한다.
	 */
	@Test @Tag("design")
	@DisplayName("[구조 M1] 모델은 java.awt·javax.swing·원본 UI 라이브러리를 import하지 않는다")
	void modelImportsOnlyJavaClassesOutsideAwt() throws IOException
	{
		List<String> violations = new ArrayList<>();
		try (Stream<Path> files = Files.list(MODEL))
		{
			for (Path file : files.filter(f -> f.toString().endsWith(".java")).collect(Collectors.toList()))
				for (String line : Files.readAllLines(file))
				{
					String trimmed = line.trim();
					if (!trimmed.startsWith("import ")) continue;
					String imported = trimmed.substring(7).replace("static ", "");
					boolean allowed = (imported.startsWith("java.") && !imported.startsWith("java.awt"))
									  || imported.startsWith("com.holub.life.model.");
					if (!allowed) violations.add(file.getFileName() + ": " + trimmed);
				}
		}
		assertEquals(List.of(), violations);
	}

	/** 목적: 바깥 코드가 Composite를 직접 건드릴 수 없고 입구(Facade)만
	 *  쓸 수 있음을 보인다(N2).
	 *  방법: 리플렉션으로 클래스의 public 여부를 확인한다.
	 *  기대: Composite 클래스는 package-private, 입구와 값 타입은 public.
	 */
	@Test @Tag("design")
	@DisplayName("[구조 N2] Composite는 숨겨지고 LifeModel·BoardView 등 입구만 공개된다")
	void compositeIsHiddenBehindTheFacade()
	{
		for (Class<?> hidden : List.of(Cell.class, Neighborhood.class, Resident.class, Direction.class))
			assertFalse(Modifier.isPublic(hidden.getModifiers()), hidden.getSimpleName() + " must not be public");

		for (Class<?> visible : List.of(LifeModel.class, BoardView.class, BoardListener.class,
										BoardEdit.class, BoardSnapshot.class, Coord.class))
			assertTrue(Modifier.isPublic(visible.getModifiers()), visible.getSimpleName() + " must be public");
	}
}
