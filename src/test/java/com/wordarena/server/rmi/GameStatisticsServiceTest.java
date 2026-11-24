package com.wordarena.server.rmi;

import com.wordarena.common.GameMode;
import com.wordarena.common.PlayerStats;
import com.wordarena.server.game.CooperativeRound;
import com.wordarena.server.game.DuelRound;
import com.wordarena.server.game.GamePlayer;
import com.wordarena.server.game.GameRoom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameStatisticsServiceTest {

    private PlayerStatisticsRepository repository;
    private GameStatisticsService statisticsService;
    private GamePlayer host;
    private GamePlayer secondPlayer;

    @BeforeEach
    void setUp() {
        repository =
                new PlayerStatisticsRepository();

        statisticsService =
                new GameStatisticsService(repository);

        host = new GamePlayer(
                "player-one",
                "Hedi"
        );

        secondPlayer = new GamePlayer(
                "player-two",
                "Youssef"
        );
    }

    @Test
    void shouldRegisterPlayerWithoutCompletedGame() {
        statisticsService.registerPlayer(
                host.getPlayerId(),
                host.getUsername()
        );

        PlayerStats stats =
                repository.findPlayerStats(
                        host.getPlayerId()
                ).orElseThrow();

        assertEquals(0, stats.getGamesPlayed());
    }

    @Test
    void shouldRecordCompletedDuelOnce() {
        GameRoom room =
                createRoom(GameMode.DUEL);

        DuelRound round =
                new DuelRound(
                        "TRAIN",
                        List.of(host, secondPlayer)
                );

        round.submitGuess(
                host.getPlayerId(),
                "TRAIN"
        );

        assertTrue(
                statisticsService.recordCompletedDuel(
                        room,
                        round
                )
        );

        assertFalse(
                statisticsService.recordCompletedDuel(
                        room,
                        round
                )
        );

        assertEquals(
                1,
                repository.findPlayerStats(
                        host.getPlayerId()
                ).orElseThrow().getGamesWon()
        );

        assertEquals(
                1,
                repository.findPlayerStats(
                        secondPlayer.getPlayerId()
                ).orElseThrow().getGamesLost()
        );
    }

    @Test
    void shouldRecordCooperativeVictoryForEveryPlayer() {
        GameRoom room =
                createRoom(GameMode.COOPERATIVE);

        CooperativeRound round =
                new CooperativeRound(
                        "TRAIN",
                        List.of(host, secondPlayer)
                );

        round.submitGuess(
                host.getPlayerId(),
                "TRAIN"
        );

        assertTrue(
                statisticsService
                        .recordCompletedCooperative(
                                room,
                                round
                        )
        );

        PlayerStats hostStats =
                repository.findPlayerStats(
                        host.getPlayerId()
                ).orElseThrow();

        PlayerStats secondStats =
                repository.findPlayerStats(
                        secondPlayer.getPlayerId()
                ).orElseThrow();

        assertEquals(1, hostStats.getGamesWon());
        assertEquals(1, secondStats.getGamesWon());
        assertEquals(600, hostStats.getTotalScore());
        assertEquals(600, secondStats.getTotalScore());
    }

    @Test
    void shouldRecordCooperativeLossForEveryPlayer() {
        GameRoom room =
                createRoom(GameMode.COOPERATIVE);

        CooperativeRound round =
                new CooperativeRound(
                        "TRAIN",
                        List.of(host, secondPlayer)
                );

        List<String> guesses = List.of(
                "APPLE",
                "BEACH",
                "CLOUD",
                "DREAM",
                "HOUSE",
                "MOUSE"
        );

        for (int index = 0;
             index < guesses.size();
             index++) {

            round.submitGuess(
                    index % 2 == 0
                            ? host.getPlayerId()
                            : secondPlayer.getPlayerId(),
                    guesses.get(index)
            );
        }

        statisticsService.recordCompletedCooperative(
                room,
                round
        );

        assertEquals(
                1,
                repository.findPlayerStats(
                        host.getPlayerId()
                ).orElseThrow().getGamesLost()
        );

        assertEquals(
                1,
                repository.findPlayerStats(
                        secondPlayer.getPlayerId()
                ).orElseThrow().getGamesLost()
        );
    }

    @Test
    void shouldNotRecordCooperativeRoundTwice() {
        GameRoom room =
                createRoom(GameMode.COOPERATIVE);

        CooperativeRound round =
                new CooperativeRound(
                        "TRAIN",
                        List.of(host, secondPlayer)
                );

        round.submitGuess(
                host.getPlayerId(),
                "TRAIN"
        );

        assertTrue(
                statisticsService
                        .recordCompletedCooperative(
                                room,
                                round
                        )
        );

        assertFalse(
                statisticsService
                        .recordCompletedCooperative(
                                room,
                                round
                        )
        );

        assertEquals(
                1,
                statisticsService.getRecordedRoundCount()
        );
    }

    @Test
    void shouldRejectUnfinishedDuel() {
        GameRoom room =
                createRoom(GameMode.DUEL);

        DuelRound round =
                new DuelRound(
                        "TRAIN",
                        List.of(host, secondPlayer)
                );

        assertThrows(
                IllegalStateException.class,
                () -> statisticsService
                        .recordCompletedDuel(
                                room,
                                round
                        )
        );
    }

    @Test
    void shouldRejectUnfinishedCooperativeRound() {
        GameRoom room =
                createRoom(GameMode.COOPERATIVE);

        CooperativeRound round =
                new CooperativeRound(
                        "TRAIN",
                        List.of(host, secondPlayer)
                );

        assertThrows(
                IllegalStateException.class,
                () -> statisticsService
                        .recordCompletedCooperative(
                                room,
                                round
                        )
        );
    }

    private GameRoom createRoom(
            GameMode gameMode
    ) {
        GameRoom room =
                new GameRoom(
                        "ROOM1234",
                        "Statistics Room",
                        gameMode,
                        gameMode == GameMode.DUEL
                                ? 2
                                : 4,
                        host
                );

        room.addPlayer(secondPlayer);
        return room;
    }
}