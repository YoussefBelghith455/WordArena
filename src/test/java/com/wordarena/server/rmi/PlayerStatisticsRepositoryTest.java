package com.wordarena.server.rmi;

import com.wordarena.common.PlayerStats;
import com.wordarena.common.ScoreEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerStatisticsRepositoryTest {

    private PlayerStatisticsRepository repository;

    @BeforeEach
    void setUp() {
        repository =
                new PlayerStatisticsRepository();
    }

    @Test
    void shouldRegisterPlayerWithEmptyStatistics() {
        repository.registerPlayer(
                "player-one",
                "Hedi"
        );

        PlayerStats statistics =
                repository.findPlayerStats(
                        "player-one"
                ).orElseThrow();

        assertEquals(
                "player-one",
                statistics.getPlayerId()
        );

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
                statistics.getGamesWon()
        );

        assertEquals(
                0,
                statistics.getGamesLost()
        );

        assertEquals(
                0,
                statistics.getTotalScore()
        );
    }

    @Test
    void shouldNotDuplicateRegisteredPlayer() {
        repository.registerPlayer(
                "player-one",
                "Hedi"
        );

        repository.registerPlayer(
                "player-one",
                "Hedi"
        );

        assertEquals(
                1,
                repository.getRegisteredPlayerCount()
        );
    }

    @Test
    void shouldUpdateUsernameForExistingPlayer() {
        repository.registerPlayer(
                "player-one",
                "OldUsername"
        );

        repository.registerPlayer(
                "player-one",
                "Hedi"
        );

        PlayerStats statistics =
                repository.findPlayerStats(
                        "player-one"
                ).orElseThrow();

        assertEquals(
                "Hedi",
                statistics.getUsername()
        );
    }

    @Test
    void shouldRecordWinningGameAndScore() {
        repository.recordGameResult(
                "player-one",
                "Hedi",
                true,
                900
        );

        PlayerStats statistics =
                repository.findPlayerStats(
                        "player-one"
                ).orElseThrow();

        assertEquals(
                1,
                statistics.getGamesPlayed()
        );

        assertEquals(
                1,
                statistics.getGamesWon()
        );

        assertEquals(
                0,
                statistics.getGamesLost()
        );

        assertEquals(
                900,
                statistics.getTotalScore()
        );
    }

    @Test
    void shouldRecordLosingGame() {
        repository.recordGameResult(
                "player-two",
                "Youssef",
                false,
                0
        );

        PlayerStats statistics =
                repository.findPlayerStats(
                        "player-two"
                ).orElseThrow();

        assertEquals(
                1,
                statistics.getGamesPlayed()
        );

        assertEquals(
                0,
                statistics.getGamesWon()
        );

        assertEquals(
                1,
                statistics.getGamesLost()
        );

        assertEquals(
                0,
                statistics.getTotalScore()
        );
    }

    @Test
    void shouldAccumulateSeveralGameResults() {
        repository.recordGameResult(
                "player-one",
                "Hedi",
                true,
                900
        );

        repository.recordGameResult(
                "player-one",
                "Hedi",
                false,
                0
        );

        repository.recordGameResult(
                "player-one",
                "Hedi",
                true,
                700
        );

        PlayerStats statistics =
                repository.findPlayerStats(
                        "player-one"
                ).orElseThrow();

        assertEquals(
                3,
                statistics.getGamesPlayed()
        );

        assertEquals(
                2,
                statistics.getGamesWon()
        );

        assertEquals(
                1,
                statistics.getGamesLost()
        );

        assertEquals(
                1600,
                statistics.getTotalScore()
        );
    }

    @Test
    void shouldBuildLeaderboardByDescendingScore() {
        repository.recordGameResult(
                "player-one",
                "Hedi",
                true,
                700
        );

        repository.recordGameResult(
                "player-two",
                "Youssef",
                true,
                900
        );

        repository.recordGameResult(
                "player-three",
                "Amine",
                false,
                0
        );

        List<ScoreEntry> leaderboard =
                repository.getLeaderboard();

        assertEquals(
                3,
                leaderboard.size()
        );

        assertEquals(
                "Youssef",
                leaderboard.get(0).getUsername()
        );

        assertEquals(
                900,
                leaderboard.get(0).getScore()
        );

        assertEquals(
                1,
                leaderboard.get(0).getRank()
        );

        assertEquals(
                "Hedi",
                leaderboard.get(1).getUsername()
        );

        assertEquals(
                2,
                leaderboard.get(1).getRank()
        );

        assertEquals(
                "Amine",
                leaderboard.get(2).getUsername()
        );

        assertEquals(
                3,
                leaderboard.get(2).getRank()
        );
    }

    @Test
    void shouldUseUsernameToOrderEqualScores() {
        repository.recordGameResult(
                "player-one",
                "Youssef",
                true,
                800
        );

        repository.recordGameResult(
                "player-two",
                "Hedi",
                true,
                800
        );

        List<ScoreEntry> leaderboard =
                repository.getLeaderboard();

        assertEquals(
                "Hedi",
                leaderboard.get(0).getUsername()
        );

        assertEquals(
                "Youssef",
                leaderboard.get(1).getUsername()
        );
    }

    @Test
    void shouldReturnEmptyResultForUnknownPlayer() {
        assertTrue(
                repository.findPlayerStats(
                        "unknown-player"
                ).isEmpty()
        );
    }

    @Test
    void shouldRejectNegativeScore() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> repository.recordGameResult(
                                "player-one",
                                "Hedi",
                                true,
                                -100
                        )
                );

        assertEquals(
                "Earned score cannot be negative.",
                exception.getMessage()
        );
    }
}