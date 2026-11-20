package com.holub.life.view;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import com.holub.life.model.*;

/** 읽기 전용 Proxy(ReadOnlyBoardView)가 화면에 보드를 "읽기만" 하게 만드는지 확인한다.
 *  화면을 붙인 보드를 움직이는 테스트가 있어 EDT에서 실행한다.
 */
@DisplayName("읽기 전용 보드 ReadOnlyBoardView (Proxy)")
@ExtendWith(OnEventDispatchThread.class)
class ReadOnlyBoardViewTest
{
	private static final Set<Coord> GLIDER = Set.of(new Coord(1, 2), new Coord(2, 3),
													new Coord(3, 1), new Coord(3, 2), new Coord(3, 3));

	/** 목적: core 구조에서는 패널이 받은 BoardView가 실은 LifeModel이라
	 *  (LifeModel)로 캐스팅하면 보드를 바꿀 수 있었다. Proxy로 감싸면 막히는지 확인.
	 *  방법: LifeModel을 감싼 뒤 LifeModel인지 검사하고 캐스팅해 본다.
	 *  기대: LifeModel이 아니고, 캐스팅하면 ClassCastException이 난다.
	 */
	@Test @Tag("design")
	@DisplayName("[구조 D1] 감싼 보드는 LifeModel로 캐스팅해 바꿀 수 없다")
	void cannotBeCastBackToTheModel()
	{
		BoardView view = ReadOnlyBoardView.of(LifeModel.builder().build());
		assertFalse(view instanceof LifeModel);
		assertThrows(ClassCastException.class, () -> ((LifeModel) view).clear());
	}

	/** 목적: Proxy가 읽기를 모두 실제 보드에 그대로 전달하는지 확인.
	 *  방법: 실제 보드에 글라이더를 놓고 세대를 진행하며, 크기·세대·셀·스냅샷을 비교한다.
	 *  기대: 매번 실제 보드와 같은 값을 돌려준다.
	 */
	@Test @Tag("design")
	@DisplayName("[구조 D1] 읽기는 실제 보드와 항상 같다 (크기, 세대, 셀, 스냅샷)")
	void readsThroughToTheRealBoard()
	{
		LifeModel model = LifeModel.builder().size(16).build();
		BoardView view = ReadOnlyBoardView.of(model);
		model.apply(BoardEdit.set(GLIDER, true));

		for (int generation = 0; generation < 5; ++generation)
		{
			assertEquals(model.size(), view.size());
			assertEquals(model.generation(), view.generation());
			assertEquals(model.snapshot(), view.snapshot());
			for (int r = 0; r < 16; ++r)
				for (int c = 0; c < 16; ++c)
					assertEquals(model.isAlive(r, c), view.isAlive(r, c), "cell (" + r + ", " + c + ")");
			model.step();
		}
	}

	/** 목적: 화면이 Proxy를 통해 등록한 리스너가 실제 보드의 알림을 받고,
	 *  Proxy를 통해 해제할 수 있는지 확인(Observer 연결이 끊기지 않음).
	 *  방법: Proxy에 리스너를 달고 편집·진행·초기화한 뒤, 해제하고 한 번 더 진행한다.
	 *  기대: 해제 전 알림 세 가지가 모두 오고, 해제 뒤에는 오지 않는다.
	 */
	@Test @Tag("design")
	@DisplayName("[구조 D1] 리스너는 Proxy로 등록·해제해도 실제 보드의 알림을 받는다")
	void forwardsListeners()
	{
		LifeModel model = LifeModel.builder().build();
		BoardView view = ReadOnlyBoardView.of(model);
		List<String> heard = new ArrayList<>();
		BoardListener listener = new BoardListener()
		{
			@Override public void stepped(long generation, Set<Coord> changed) { heard.add("stepped " + generation); }
			@Override public void edited(BoardEdit applied) { heard.add("edited " + applied.cells().size()); }
			@Override public void reset() { heard.add("reset"); }
		};

		view.addListener(listener);
		model.apply(BoardEdit.set(GLIDER, true));
		model.step();
		model.clear();
		view.removeListener(listener);
		model.step();

		assertEquals(List.of("edited 5", "stepped 1", "reset"), heard);
	}

	/** 목적: 이미 감싼 보드를 또 감싸 Proxy가 겹겹이 쌓이지 않는지 확인.
	 *  기대: 감싼 보드를 다시 감싸면 같은 객체가 돌아온다.
	 */
	@Test @Tag("design")
	@DisplayName("[구조 D1] 이미 감싼 보드는 다시 감싸지 않는다")
	void doesNotWrapTwice()
	{
		BoardView once = ReadOnlyBoardView.of(LifeModel.builder().build());
		assertSame(once, ReadOnlyBoardView.of(once));
	}

	/** 목적: 패널에 실제 보드 대신 Proxy를 줘도 화면이 똑같은지 확인
	 *  (기존 화면 기능 유지, 바뀐 곳만 그리기 포함).
	 *  방법: 같은 보드에 패널 두 개를 단다. 하나는 보드를 직접, 하나는 Proxy로 받는다.
	 *  무작위 보드를 20세대 진행하며 중간에 편집도 섞고, 매번 두 화면을 픽셀 단위로 비교한다.
	 *  기대: 모든 픽셀이 같다.
	 */
	@Test @Tag("existing")
	@DisplayName("[기존 기능] 패널에 Proxy를 줘도 직접 준 것과 화면이 픽셀 단위로 같다")
	void panelDrawsTheSameThroughTheProxy()
	{
		LifeModel model = LifeModel.builder().build();
		Random random = new Random(11);
		Set<Coord> live = new HashSet<>();
		for (int r = 0; r < 64; ++r)
			for (int c = 0; c < 64; ++c)
				if (random.nextDouble() < 0.25) live.add(new Coord(r, c));
		model.restore(new BoardSnapshot(64, live));

		UniversePanel direct  = panel(model);
		UniversePanel proxied = panel(ReadOnlyBoardView.of(model));
		for (int generation = 0; generation < 20; ++generation)
		{
			if (generation % 7 == 3)
				model.toggle(random.nextInt(64), random.nextInt(64));
			model.step();
			assertSamePixels(render(direct), render(proxied), "generation " + generation);
		}
	}

	private static UniversePanel panel(BoardView board)
	{
		UniversePanel panel = new UniversePanel(board, 8);
		panel.setSize(board.size() * 8, board.size() * 8);
		render(panel);                  // first full paint, as when the window opens
		return panel;
	}

	private static BufferedImage render(UniversePanel panel)
	{
		BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics g = image.getGraphics();
		panel.paint(g);
		g.dispose();
		return image;
	}

	private static void assertSamePixels(BufferedImage expected, BufferedImage actual, String where)
	{
		for (int y = 0; y < expected.getHeight(); ++y)
			for (int x = 0; x < expected.getWidth(); ++x)
				assertEquals(expected.getRGB(x, y), actual.getRGB(x, y), where + " pixel (" + x + ", " + y + ")");
	}
}
