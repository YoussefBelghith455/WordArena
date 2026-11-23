package com.wordarena.server.rmi;

import com.wordarena.common.GameMode;
import com.wordarena.common.PlayerStats;
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
    private GameRoom room;

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

        room = new GameRoom(
                "ROOM1234",
                "Statistics Duel",
                GameMode.DUEL,
                2,
                host
        );

        room.addPlayer(secondPlayer);
    }

    @Test
    void shouldRegisterPlayerWithoutCompletedGame() {
        statisticsService.registerPlayer(
                host.getPlayerId(),
                host.getUsername()
        );

        PlayerStats statistics =
                repository.findPlayerStats(
                        host.getPlayerId()
                ).orElseThrow();

        assertEquals(
                "Hedi",
                statistics.getUsername()
        );

        assertEquals(
                0,
                statistics.getGamesPlayed()
        );

        assertEquals(
                0,
                statistics.getTotalScore()
        );
    }

    @Test
    void shouldRecordWinnerAndLoser() {
        DuelRound duelRound = new DuelRound(
                "TRAIN",
                List.of(
                        host,
                        secondPlayer
                )
        );

        duelRound.submitGuess(
                host.getPlayerId(),
                "TRAIN"
        );

        boolean recorded =
                statisticsService.recordCompletedDuel(
                        room,
                        duelRound
                );

        assertTrue(recorded);

        PlayerStats winnerStatistics =
                repository.findPlayerStats(
                        host.getPlayerId()
                ).orElseThrow();

        assertEquals(
                1,
                winnerStatistics.getGamesPlayed()
        );

        assertEquals(
                1,
                winnerStatistics.getGamesWon()
        );

        assertEquals(
                0,
                winnerStatistics.getGamesLost()
        );

        assertEquals(
                1000,
                winnerStatistics.getTotalScore()
        );

        PlayerStats loserStatistics =
                repository.findPlayerStats(
                        secondPlayer.getPlayerId()
                ).orElseThrow();

        assertEquals(
                1,
                loserStatistics.getGamesPlayed()
        );

        assertEquals(
                0,
                loserStatistics.getGamesWon()
        );

        assertEquals(
                1,
                loserStatistics.getGamesLost()
        );

        assertEquals(
                0,
                loserStatistics.getTotalScore()
        );
    }

    @Test
    void shouldRecordNoWinnerAsLossForBothPlayers() {
        DuelRound duelRound = new DuelRound(
                "TRAIN",
                List.of(
                        host,
                        secondPlayer
                )
        );

        List<String> failedGuesses = List.of(
                "APPLE",
                "BEACH",
                "CLOUD",
                "DREAM",
                "HOUSE",
                "MOUSE"
        );

        for (String guess : failedGuesses) {
            duelRound.submitGuess(
                    host.getPlayerId(),
                    guess
            );
        }

        for (String guess : failedGuesses) {
            duelRound.submitGuess(
                    secondPlayer.getPlayerId(),
                    guess
            );
        }

        statisticsService.recordCompletedDuel(
                room,
                duelRound
        );

        PlayerStats hostStatistics =
                repository.findPlayerStats(
                        host.getPlayerId()
                ).orElseThrow();

        PlayerStats secondPlayerStatistics =
                repository.findPlayerStats(
                        secondPlayer.getPlayerId()
                ).orElseThrow();

        assertEquals(
                1,
                hostStatistics.getGamesLost()
        );

        assertEquals(
                1,
                secondPlayerStatistics.getGamesLost()
        );

        assertEquals(
                0,
                hostStatistics.getGamesWon()
        );

        assertEquals(
                0,
                secondPlayerStatistics.getGamesWon()
        );
    }

    @Test
    void shouldNotRecordSameRoundTwice() {
        DuelRound duelRound = new DuelRound(
                "TRAIN",
                List.of(
                        host,
                        secondPlayer
                )
        );

        duelRound.submitGuess(
                host.getPlayerId(),
                "TRAIN"
        );

        boolean firstRecording =
                statisticsService.recordCompletedDuel(
                        room,
                        duelRound
                );

        boolean secondRecording =
                statisticsService.recordCompletedDuel(
                        room,
                        duelRound
                );

        assertTrue(firstRecording);
        assertFalse(secondRecording);

        PlayerStats statistics =
                repository.findPlayerStats(
                        host.getPlayerId()
                ).orElseThrow();

        assertEquals(
                1,
                statistics.getGamesPlayed()
        );

        assertEquals(
                1,
                statisticsService.getRecordedRoundCount()
        );
    }

    @Test
    void shouldRejectUnfinishedRound() {
        DuelRound duelRound = new DuelRound(
                "TRAIN",
                List.of(
                        host,
                        secondPlayer
                )
        );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> statisticsService
                                .recordCompletedDuel(
                                        room,
                                        duelRound
                                )
                );

        assertEquals(
                "Duel statistics cannot be recorded "
                        + "before the round finishes.",
                exception.getMessage()
        );

        assertEquals(
                0,
                repository.getRegisteredPlayerCount()
        );
    }
}