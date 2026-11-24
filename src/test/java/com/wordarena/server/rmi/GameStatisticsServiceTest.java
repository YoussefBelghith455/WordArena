package com.wordarena.server.rmi;

import com.wordarena.common.GameMode;
import com.wordarena.common.PlayerStats;
import com.wordarena.server.game.CooperativeRound;
import com.wordarena.server.game.DuelRound;
import com.wordarena.server.game.GamePlayer;
import com.wordarena.server.game.GameRoom;
import com.wordarena.server.game.HostRound;
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
    void shouldRegisterPlayer() {
        statisticsService.registerPlayer(
                host.getPlayerId(),
                host.getUsername()
        );

        assertEquals(
                0,
                repository.findPlayerStats(
                        host.getPlayerId()
                ).orElseThrow().getGamesPlayed()
        );
    }

    @Test
    void shouldRecordDuelOnce() {
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
    }

    @Test
    void shouldRecordCooperativeVictory() {
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

        statisticsService.recordCompletedCooperative(
                room,
                round
        );

        assertEquals(
                1,
                stats(host).getGamesWon()
        );

        assertEquals(
                1,
                stats(secondPlayer).getGamesWon()
        );
    }

    @Test
    void shouldRecordHostVictory() {
        GameRoom room =
                createRoom(GameMode.HOST);

        HostRound round =
                new HostRound(
                        "TRAIN",
                        host,
                        List.of(host, secondPlayer)
                );

        for (String guess : List.of(
                "APPLE",
                "BEACH",
                "CLOUD",
                "DREAM",
                "HOUSE",
                "MOUSE"
        )) {
            round.submitGuess(
                    secondPlayer.getPlayerId(),
                    guess
            );
        }

        assertTrue(
                statisticsService.recordCompletedHost(
                        room,
                        round
                )
        );

        assertEquals(
                1,
                stats(host).getGamesWon()
        );

        assertEquals(
                600,
                stats(host).getTotalScore()
        );

        assertEquals(
                1,
                stats(secondPlayer).getGamesLost()
        );
    }

    @Test
    void shouldRecordGuessingTeamVictory() {
        GameRoom room =
                createRoom(GameMode.HOST);

        HostRound round =
                new HostRound(
                        "TRAIN",
                        host,
                        List.of(host, secondPlayer)
                );

        round.submitGuess(
                secondPlayer.getPlayerId(),
                "TRAIN"
        );

        statisticsService.recordCompletedHost(
                room,
                round
        );

        assertEquals(
                1,
                stats(host).getGamesLost()
        );

        assertEquals(
                1,
                stats(secondPlayer).getGamesWon()
        );

        assertEquals(
                600,
                stats(secondPlayer).getTotalScore()
        );
    }

    @Test
    void shouldNotRecordHostRoundTwice() {
        GameRoom room =
                createRoom(GameMode.HOST);

        HostRound round =
                new HostRound(
                        "TRAIN",
                        host,
                        List.of(host, secondPlayer)
                );

        round.submitGuess(
                secondPlayer.getPlayerId(),
                "TRAIN"
        );

        assertTrue(
                statisticsService.recordCompletedHost(
                        room,
                        round
                )
        );

        assertFalse(
                statisticsService.recordCompletedHost(
                        room,
                        round
                )
        );
    }

    @Test
    void shouldRejectUnfinishedHostRound() {
        GameRoom room =
                createRoom(GameMode.HOST);

        HostRound round =
                new HostRound(
                        "TRAIN",
                        host,
                        List.of(host, secondPlayer)
                );

        assertThrows(
                IllegalStateException.class,
                () -> statisticsService
                        .recordCompletedHost(
                                room,
                                round
                        )
        );
    }

    private PlayerStats stats(
            GamePlayer player
    ) {
        return repository.findPlayerStats(
                player.getPlayerId()
        ).orElseThrow();
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