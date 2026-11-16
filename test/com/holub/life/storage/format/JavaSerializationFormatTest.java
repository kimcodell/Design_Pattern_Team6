package com.holub.life.storage.format;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.*;
import com.holub.life.model.*;

/** 원본 ".life" 파일 형식을 담당하는 serializer를 파일·화면 없이
 *  (바이트 배열만으로) 확인한다.
 */
@DisplayName("원본 .life 저장 형식")
class JavaSerializationFormatTest
{
	private final JavaSerializationFormat format = new JavaSerializationFormat(64);

	/** 목적: 원본의 저장·불러오기 기능이 유지되는지 확인.
	 *  방법: 스냅샷을 바이트로 쓴 뒤 다시 읽는다.
	 *  기대: 같은 스냅샷이 나온다.
	 */
	@Test @Tag("existing")
	@DisplayName("[기존 기능] 저장한 보드를 다시 읽으면 같은 보드가 나온다")
	void writeThenReadGivesTheSameSnapshot() throws IOException
	{
		BoardSnapshot snapshot = new BoardSnapshot(64,
						Set.of(new Coord(0, 0), new Coord(3, 7), new Coord(63, 62)));
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		format.write(snapshot, bytes);
		assertEquals(snapshot, format.read(new ByteArrayInputStream(bytes.toByteArray())));
	}

	/** 목적: 원본 프로그램이 저장한 파일을 그대로 읽을 수 있는지 확인. 원본은
	 *  Point(x = 열, y = 행)의 LinkedList를 저장했고, 버그로 모든 셀을 두 번
	 *  기록했다(S1).
	 *  방법: 원본과 같은 객체 구조(중복 포함)로 만든 바이트를 읽는다.
	 *  기대: 중복 없이 행·열이 맞게 읽힌다.
	 */
	@Test @Tag("existing")
	@DisplayName("[기존 기능] 원본 프로그램이 저장한 파일(셀 중복 기록 포함)을 읽는다")
	void readsFilesInTheOriginalFormatIncludingDuplicates() throws IOException
	{
		LinkedList<Point> original = new LinkedList<>();
		for (Point p : List.of(new Point(7, 3), new Point(1, 2)))
		{   original.add(p);
			original.add(p);
		}
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(original); }

		BoardSnapshot read = format.read(new ByteArrayInputStream(bytes.toByteArray()));
		assertEquals(64, read.size());
		assertEquals(Set.of(new Coord(3, 7), new Coord(2, 1)), read.liveCells());
	}

	/** 목적: 불러오기 검증의 serializer 쪽 단계(구문)와, 허용한 클래스만
	 *  역직렬화하는 필터가 동작하는지 확인.
	 *  방법: Life 파일이 아닌 객체(Date)와 아무 바이트나 읽는다.
	 *  기대: 둘 다 IOException.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] Life 파일이 아닌 내용은 IOException으로 거부된다")
	void rejectsStreamsThatAreNotLifeFiles() throws IOException
	{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(new Date()); }
		assertThrows(IOException.class, () -> format.read(new ByteArrayInputStream(bytes.toByteArray())));

		assertThrows(IOException.class,
					 () -> format.read(new ByteArrayInputStream("not a life file".getBytes())));
	}
}
