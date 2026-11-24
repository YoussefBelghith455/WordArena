package com.wordarena.server.game;

import com.wordarena.common.GameMode;
import com.wordarena.server.rmi.GameStatisticsService;
import com.wordarena.server.rmi.PlayerStatisticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HostGameServiceTest {

    private RoomManager roomManager;
    private HostGameService hostGameService;
    private GameRoom room;
    private GamePlayer host;
    private GamePlayer guesser;

    @BeforeEach
    void setUp() {
        roomManager = new RoomManager();

        hostGameService =
                new HostGameService(
                        roomManager,
                        new GameStatisticsService(
                                new PlayerStatisticsRepository()
                        )
                );

        host = new GamePlayer(
                "host-player",
                "Hedi"
        );

        guesser = new GamePlayer(
                "guesser-player",
                "Youssef"
        );

        room = roomManager.createRoom(
                "Host Room",
                GameMode.HOST,
                2,
                host
        );

        roomManager.joinRoom(
                room.getRoomId(),
                guesser
        );
    }

    @Test
    void shouldAllowHostToLockWord() {
        hostGameService.lockSecretWord(
                room.getRoomId(),
                host.getPlayerId(),
                "train"
        );

        assertTrue(
                hostGameService.isSecretWordLocked(
                        room.getRoomId()
                )
        );
    }

    @Test
    void shouldRejectNonHostWordLock() {
        assertThrows(
                IllegalStateException.class,
                () -> hostGameService.lockSecretWord(
                        room.getRoomId(),
                        guesser.getPlayerId(),
                        "TRAIN"
                )
        );
    }

    @Test
    void shouldRequireLockedWordBeforeStart() {
        setPlayersReady();

        assertThrows(
                IllegalStateException.class,
                () -> hostGameService.startRound(
                        room.getRoomId(),
                        host.getPlayerId()
                )
        );
    }

    @Test
    void shouldStartAndProcessHostRound() {
        setPlayersReady();

        hostGameService.lockSecretWord(
                room.getRoomId(),
                host.getPlayerId(),
                "TRAIN"
        );

        HostRound round =
                hostGameService.startRound(
                        room.getRoomId(),
                        host.getPlayerId()
                );

        HostRound.GuessOutcome outcome =
                hostGameService.submitGuess(
                        room.getRoomId(),
                        guesser.getPlayerId(),
                        "TRAIN"
                );

        assertTrue(room.isStarted());
        assertTrue(round.isFinished());
        assertTrue(outcome.solved());

        assertEquals(
                "GUESSERS",
                outcome.winningSide()
        );
    }

    @Test
    void shouldRejectHostGuessThroughService() {
        setPlayersReady();

        hostGameService.lockSecretWord(
                room.getRoomId(),
                host.getPlayerId(),
                "TRAIN"
        );

        hostGameService.startRound(
                room.getRoomId(),
                host.getPlayerId()
        );

        assertThrows(
                IllegalStateException.class,
                () -> hostGameService.submitGuess(
                        room.getRoomId(),
                        host.getPlayerId(),
                        "TRAIN"
                )
        );
    }

    @Test
    void shouldReportUnlockedWord() {
        assertFalse(
                hostGameService.isSecretWordLocked(
                        room.getRoomId()
                )
        );
    }

    private void setPlayersReady() {
        room.setPlayerReady(
                host.getPlayerId(),
                true
        );

        room.setPlayerReady(
                guesser.getPlayerId(),
                true
        );
    }
}