package com.holub.life.storage;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.holub.life.model.*;
import com.holub.life.storage.format.*;

/** 파일 입출력(BoardFileIO)이 확장자로 형식을 고르고, 형식 추가에
 *  닫혀 있는지(OCP) 확인한다. 실제 파일은 임시 폴더에 쓴다.
 */
@DisplayName("파일 입출력과 형식 선택")
class BoardFileIOTest
{
	/** 테스트용 두 번째 형식: 한 줄에 "행,열" 하나. BoardFileIO를 고치지
	 *  않고 형식을 추가할 수 있음을 보이려고 테스트 안에서 정의한다.
	 */
	static final class TextFormat implements BoardSerializer
	{
		public void write(BoardSnapshot s, OutputStream out) throws IOException
		{
			PrintWriter w = new PrintWriter(new OutputStreamWriter(out));
			w.println(s.size());
			for (Coord c : s.liveCells()) w.println(c.row() + "," + c.col());
			w.flush();
		}
		public BoardSnapshot read(InputStream in) throws IOException
		{
			BufferedReader r = new BufferedReader(new InputStreamReader(in));
			int size = Integer.parseInt(r.readLine().trim());
			Set<Coord> live = new HashSet<>();
			for (String line; (line = r.readLine()) != null; )
			{
				String[] parts = line.split(",");
				live.add(new Coord(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])));
			}
			return new BoardSnapshot(size, live);
		}
		public String extension() { return ".txt"; }
	}

	@TempDir Path dir;

	private final BoardSnapshot snapshot = new BoardSnapshot(64, Set.of(new Coord(1, 2), new Coord(5, 5)));

	/** 목적: 형식 추가(Strategy)와 확장자 선택이 동작하고, BoardFileIO를
	 *  고치지 않고 새 형식을 붙일 수 있음(OCP)을 보인다.
	 *  방법: 원본 형식과 테스트용 텍스트 형식을 등록하고 .life/.txt로 각각 저장·불러오기.
	 *  기대: 둘 다 같은 보드가 돌아오고, .txt 파일은 텍스트 형식으로 저장되어 있다.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] 확장자로 형식을 골라 저장·불러오기 한다 (새 형식은 등록만으로 추가)")
	void picksTheFormatByExtension() throws IOException
	{
		BoardFileIO io = new BoardFileIO(List.of(new JavaSerializationFormat(64), new TextFormat()));
		File life = dir.resolve("board.life").toFile();
		File text = dir.resolve("board.txt").toFile();

		io.write(life, snapshot);
		io.write(text, snapshot);

		assertEquals(snapshot, io.read(life));
		assertEquals(snapshot, io.read(text));
		assertTrue(Files.readString(text.toPath()).startsWith("64"), "the text format was used for .txt");
	}

	/** 목적: 등록되지 않은 확장자를 조용히 잘못 처리하지 않는지 확인.
	 *  방법: .png 파일로 저장한다.
	 *  기대: IOException.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] 등록되지 않은 확장자는 오류다")
	void unknownExtensionIsAnError()
	{
		BoardFileIO io = new BoardFileIO(List.of(new JavaSerializationFormat(64)));
		assertThrows(IOException.class, () -> io.write(dir.resolve("board.png").toFile(), snapshot));
	}

	/** 목적: 한 확장자에 형식이 둘 등록되는 설정 실수를 막는지 확인.
	 *  방법: .life 형식을 두 번 등록한다.
	 *  기대: 생성자에서 예외.
	 */
	@Test @Tag("new-feature")
	@DisplayName("[새 기능] 같은 확장자에 형식을 두 번 등록하면 거부된다")
	void twoFormatsForOneExtensionAreRejected()
	{
		assertThrows(IllegalArgumentException.class,
					 () -> new BoardFileIO(List.of(new JavaSerializationFormat(64), new JavaSerializationFormat(32))));
	}
}
