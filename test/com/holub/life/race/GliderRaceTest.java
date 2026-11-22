package com.holub.life.race;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("new-feature")
@DisplayName("글라이더 경주")
class GliderRaceTest
{
	@Test
	@DisplayName("베팅 금액을 한 번 차감하고 모든 글라이더가 같은 위치와 속도로 출발한다")
	void chargesStakeAndStartsTogether()
	{
		GliderRace race = new GliderRace(new Random(22));
		List<GliderRace.Frame> frames = new ArrayList<>();
		race.addObserver(new RaceObserver()
		{
			@Override public void raceAdvanced(GliderRace.Frame frame)
			{
				frames.add(frame);
			}
		});

		race.start(6, 4, 125);
		assertTrue(race.isRunning());
		assertEquals(875, race.balance());
		assertEquals(Collections.nCopies(6, 0.0), frames.get(0).progress());

		race.advance();
		double firstProgress = frames.get(1).progress().get(0);
		assertTrue(firstProgress > 0);
		assertEquals(Collections.nCopies(6, firstProgress), frames.get(1).progress());
		assertEquals(Collections.nCopies(6, 0.0), frames.get(0).progress());
	}

	@Test
	@DisplayName("잘못된 베팅이나 경주 중 추가 베팅은 칩을 차감하지 않는다")
	void rejectsInvalidBets()
	{
		GliderRace race = new GliderRace(new Random(1));
		assertAll(
				() -> assertThrows(IllegalArgumentException.class, () -> race.start(1, 1, 10)),
				() -> assertThrows(IllegalArgumentException.class, () -> race.start(7, 1, 10)),
				() -> assertThrows(IllegalArgumentException.class, () -> race.start(4, 0, 10)),
				() -> assertThrows(IllegalArgumentException.class, () -> race.start(4, 5, 10)),
				() -> assertThrows(IllegalArgumentException.class, () -> race.start(4, 1, 0)),
				() -> assertThrows(IllegalArgumentException.class, () -> race.start(4, 1, 1001)));
		assertEquals(1000, race.balance());
		assertFalse(race.isRunning());

		race.start(4, 1, 75);
		assertThrows(IllegalStateException.class, () -> race.start(4, 2, 25));
		assertEquals(925, race.balance());
	}

	@Test
	@DisplayName("같은 업데이트 안에서 결승선을 더 일찍 넘은 글라이더가 우승한다")
	void comparesFinishTimesWithinOneTick()
	{
		assertNull(GliderRace.findFinish(new double[] {140.0, 141.0}, new double[] {143.5, 143.8}));
		GliderRace.Finish finish = GliderRace.findFinish(
				new double[] {141.0, 143.5}, new double[] {146.0, 144.5});
		assertNotNull(finish);
		assertEquals(2, finish.winner());
		assertEquals(0.5, finish.fraction(), 1e-9);
	}

	@Test
	@DisplayName("적중하면 참가 수 배율로 지급하고 결과 알림은 정산 후 한 번만 전달된다")
	void paysWinningBetOnce()
	{
		int winner = winnerForSeed(47);
		GliderRace race = new GliderRace(new Random(47));
		List<GliderRace.Result> results = new ArrayList<>();
		race.addObserver(new RaceObserver()
		{
			@Override public void raceFinished(GliderRace.Result result)
			{
				assertFalse(race.isRunning());
				assertEquals(1400, race.balance());
				results.add(result);
			}
		});

		race.start(5, winner, 100);
		finishRace(race);
		race.advance();
		race.advance();
		assertEquals(List.of(new GliderRace.Result(winner, winner, 100, 500, 1400)), results);
		assertEquals(1400, race.balance());
	}

	@Test
	@DisplayName("틀리면 걸었던 칩만 잃고 다음 경주로 잔액이 이어진다")
	void retainsBalanceAfterLosingBet()
	{
		int winner = winnerForSeed(47);
		int pick = winner % 5 + 1;
		GliderRace race = new GliderRace(new Random(47));
		List<GliderRace.Result> results = collectResults(race);

		race.start(5, pick, 100);
		finishRace(race);
		assertEquals(List.of(new GliderRace.Result(winner, pick, 100, 0, 900)), results);
		race.start(5, pick, 50);
		assertEquals(850, race.balance());
	}

	@Test
	@DisplayName("동시 도착 시 무승부로 처리하고 걸었던 칩을 돌려준다")
	void refundsDeadHeat()
	{
		GliderRace race = new GliderRace(() -> 99L);
		List<GliderRace.Result> results = collectResults(race);
		race.start(2, 2, 200);
		assertEquals(800, race.balance());

		finishRace(race);
		assertEquals(List.of(new GliderRace.Result(0, 2, 200, 200, 1000)), results);
		assertEquals(1000, race.balance());
	}

	private static int winnerForSeed(long seed)
	{
		GliderRace race = new GliderRace(new Random(seed));
		List<GliderRace.Result> results = collectResults(race);
		race.start(5, 1, 100);
		finishRace(race);
		int winner = results.get(0).winner();
		assertTrue(winner > 0);
		return winner;
	}

	private static List<GliderRace.Result> collectResults(GliderRace race)
	{
		List<GliderRace.Result> results = new ArrayList<>();
		race.addObserver(new RaceObserver()
		{
			@Override public void raceFinished(GliderRace.Result result)
			{
				results.add(result);
			}
		});
		return results;
	}

	private static void finishRace(GliderRace race)
	{
		for (int tick = 0; tick < 200 && race.isRunning(); tick++)
			race.advance();
		assertFalse(race.isRunning(), "the gliders should reach the finish");
	}
}
