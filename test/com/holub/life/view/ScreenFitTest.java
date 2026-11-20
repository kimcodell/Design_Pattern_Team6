package com.holub.life.view;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import com.holub.life.model.*;

/** 원본 주석의 결함 "보드가 화면보다 크면 실패"(가이드 D의 남은 결함)를 고쳤는지 확인한다.
 *  실제 화면 대신 "쓸 수 있는 공간"을 숫자로 주어 창 없이 검사한다.
 */
@DisplayName("큰 보드 화면 맞춤 ScreenFit")
@ExtendWith(OnEventDispatchThread.class)
class ScreenFitTest
{
	/** 노트북 화면 정도의 공간 (1440×900 화면에서 창 장식을 뺀 크기). */
	private static final Dimension LAPTOP = new Dimension(1400, 800);

	/** 목적: 기본 보드(64×64, 셀 8픽셀)는 지금까지와 똑같이 보이는지 확인.
	 *  방법: 노트북 크기 공간에 맞춘다.
	 *  기대: 셀 크기는 8 그대로, 스크롤 창 없이 패널 자체를 창에 넣는다.
	 */
	@Test @Tag("existing")
	@DisplayName("[기존 기능] 기본 보드(64×64)는 셀 8픽셀 그대로, 스크롤 없이 보인다")
	void defaultBoardIsUnchanged()
	{
		int cell = ScreenFit.cellSize(64, 8, Math.min(LAPTOP.width, LAPTOP.height));
		assertEquals(8, cell);
		UniversePanel panel = new UniversePanel(ReadOnlyBoardView.of(LifeModel.builder().build()), cell);
		assertSame(panel, ScreenFit.fit(panel, cell, LAPTOP));
	}

	/** 목적: 원본처럼 셀 8픽셀로 고정하면 화면을 넘는 보드가, 셀을 줄여 화면 안에 들어오는지 확인.
	 *  방법: 256×256 보드(8픽셀이면 2,048픽셀)를 노트북 크기 공간에 맞춘다.
	 *  기대: 셀이 3픽셀로 줄고(768픽셀), 스크롤 없이 전체가 보인다.
	 */
	@Test @Tag("defect-fix")
	@DisplayName("[결함 수정 D3] 화면보다 큰 보드는 셀을 줄여 한 화면에 다 보인다")
	void shrinksCellsToFit()
	{
		int cell = ScreenFit.cellSize(256, 8, Math.min(LAPTOP.width, LAPTOP.height));
		assertEquals(3, cell);
		UniversePanel panel = new UniversePanel(ReadOnlyBoardView.of(LifeModel.builder().size(256).build()), cell);
		assertTrue(panel.getPreferredSize().height <= LAPTOP.height);
		assertSame(panel, ScreenFit.fit(panel, cell, LAPTOP));
	}

	/** 목적: 셀을 1픽셀로 줄여도 안 들어가는 아주 큰 보드는 스크롤로 전부 볼 수 있는지 확인.
	 *  방법: 1024×1024 보드를 노트북 크기 공간에 맞춘다.
	 *  기대: 셀은 1픽셀, 창에 넣는 것은 공간 크기를 넘지 않는 스크롤 창이고,
	 *  그 안의 패널은 보드 전체 크기(1024픽셀)라 스크롤하면 모든 셀에 닿는다.
	 */
	@Test @Tag("defect-fix")
	@DisplayName("[결함 수정 D3] 1픽셀로도 안 들어가는 보드는 화면 크기의 스크롤 창에 넣는다")
	void scrollsWhenEvenOnePixelIsTooBig()
	{
		int cell = ScreenFit.cellSize(1024, 8, Math.min(LAPTOP.width, LAPTOP.height));
		assertEquals(1, cell);
		UniversePanel panel = new UniversePanel(ReadOnlyBoardView.of(LifeModel.builder().size(1024).build()), cell);

		JComponent shown = ScreenFit.fit(panel, cell, LAPTOP);
		JScrollPane scroll = assertInstanceOf(JScrollPane.class, shown);
		assertSame(panel, scroll.getViewport().getView());
		assertEquals(new Dimension(1024, 800), scroll.getPreferredSize());
		assertEquals(new Dimension(1024, 1024), panel.getPreferredSize());
	}

	/** 목적: 셀을 줄인 큰 보드에서도 클릭이 정확한 셀에 닿는지 확인(예전에는 화면 밖 셀은
	 *  누를 수조차 없었다).
	 *  방법: 셀을 3픽셀로 줄인 256×256 보드의 네 모서리 셀 가운데를 누른다.
	 *  기대: 패널이 정확히 그 네 셀을 알린다.
	 */
	@Test @Tag("defect-fix")
	@DisplayName("[결함 수정 D3] 셀을 줄인 큰 보드에서도 모서리 셀까지 정확히 클릭된다")
	void clicksReachEveryCornerOfAShrunkBoard()
	{
		int cell = ScreenFit.cellSize(256, 8, Math.min(LAPTOP.width, LAPTOP.height));
		UniversePanel panel = new UniversePanel(ReadOnlyBoardView.of(LifeModel.builder().size(256).build()), cell);
		panel.setSize(panel.getPreferredSize());
		List<Coord> clicked = new ArrayList<>();
		panel.setCellClickListener((row, col) -> clicked.add(new Coord(row, col)));

		List<Coord> corners = List.of(new Coord(0, 0), new Coord(0, 255), new Coord(255, 0), new Coord(255, 255));
		for (Coord corner : corners)
			panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_PRESSED, 0, 0,
											   corner.col() * cell + cell / 2, corner.row() * cell + cell / 2,
											   1, false, MouseEvent.BUTTON1));
		assertEquals(corners, clicked);
	}

	/** 목적: 셀을 줄인 큰 보드가 화면에 맞게 그려지는지 확인.
	 *  방법: 256×256 보드의 네 모서리 셀을 살리고, 셀 3픽셀로 그린다.
	 *  기대: 각 모서리 셀 자리가 빨갛다.
	 */
	@Test @Tag("defect-fix")
	@DisplayName("[결함 수정 D3] 셀을 줄인 큰 보드도 모서리 셀까지 맞게 그린다")
	void drawsAShrunkBoard()
	{
		int cell = ScreenFit.cellSize(256, 8, Math.min(LAPTOP.width, LAPTOP.height));
		LifeModel model = LifeModel.builder().size(256).build();
		List<Coord> corners = List.of(new Coord(0, 0), new Coord(0, 255), new Coord(255, 0), new Coord(255, 255));
		model.apply(BoardEdit.set(corners, true));
		UniversePanel panel = new UniversePanel(ReadOnlyBoardView.of(model), cell);
		panel.setSize(panel.getPreferredSize());

		BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics g = image.getGraphics();
		panel.paint(g);
		g.dispose();
		for (Coord corner : corners)
			assertEquals(0xff0000, image.getRGB(corner.col() * cell + cell / 2, corner.row() * cell + cell / 2) & 0xffffff,
						 "corner " + corner);
	}

	/** 목적: 잘못된 입력을 조용히 넘기지 않는지 확인.
	 *  기대: 보드 크기나 셀 크기가 0 이하면 IllegalArgumentException.
	 */
	@Test @Tag("defect-fix")
	@DisplayName("[결함 수정 D3] 크기가 0 이하면 셀 크기 계산이 오류를 낸다")
	void rejectsNonPositiveSizes()
	{
		assertThrows(IllegalArgumentException.class, () -> ScreenFit.cellSize(0, 8, 800));
		assertThrows(IllegalArgumentException.class, () -> ScreenFit.cellSize(64, 0, 800));
	}
}
