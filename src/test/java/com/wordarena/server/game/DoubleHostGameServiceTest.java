package com.wordarena.server.game;

import com.wordarena.common.GameMode;
import com.wordarena.server.rmi.GameStatisticsService;
import com.wordarena.server.rmi.PlayerStatisticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoubleHostGameServiceTest {

    private DoubleHostGameService service;
    private GameRoom room;
    private GamePlayer host;
    private GamePlayer secondPlayer;

    @BeforeEach
    void setUp() {
        RoomManager roomManager =
                new RoomManager();

        service =
                new DoubleHostGameService(
                        roomManager,
                        new GameStatisticsService(
                                new PlayerStatisticsRepository()
                        )
                );

        host = new GamePlayer(
                "player-one",
                "Hedi"
        );

        secondPlayer = new GamePlayer(
                "player-two",
                "Youssef"
        );

        room = roomManager.createRoom(
                "Double Host Room",
                GameMode.DOUBLE_HOST,
                2,
                host
        );

        roomManager.joinRoom(
                room.getRoomId(),
                secondPlayer
        );
    }

    @Test
    void shouldAllowBothPlayersToLockWords() {
        service.lockSecretWord(
                room.getRoomId(),
                host.getPlayerId(),
                "TRAIN"
        );

        service.lockSecretWord(
                room.getRoomId(),
                secondPlayer.getPlayerId(),
                "CLOUD"
        );

        assertTrue(
                service.hasPlayerLockedWord(
                        room.getRoomId(),
                        host.getPlayerId()
                )
        );

        assertTrue(
                service.areBothWordsLocked(
                        room.getRoomId()
                )
        );
    }

    @Test
    void shouldRequireBothWordsBeforeStart() {
        setPlayersReady();

        service.lockSecretWord(
                room.getRoomId(),
                host.getPlayerId(),
                "TRAIN"
        );

        assertThrows(
                IllegalStateException.class,
                () -> service.startRound(
                        room.getRoomId(),
                        host.getPlayerId()
                )
        );
    }

    @Test
    void shouldStartAndFindWinner() {
        setPlayersReady();
        lockBothWords();

        DoubleHostRound round =
                service.startRound(
                        room.getRoomId(),
                        host.getPlayerId()
                );

        DoubleHostRound.GuessOutcome outcome =
                service.submitGuess(
                        room.getRoomId(),
                        host.getPlayerId(),
                        "CLOUD"
                );

        assertTrue(round.isFinished());
        assertTrue(outcome.evaluation().solved());
    }

    @Test
    void shouldRejectNonHostStart() {
        setPlayersReady();
        lockBothWords();

        assertThrows(
                IllegalStateException.class,
                () -> service.startRound(
                        room.getRoomId(),
                        secondPlayer.getPlayerId()
                )
        );
    }

    @Test
    void shouldReportUnlockedState() {
        assertFalse(
                service.areBothWordsLocked(
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
                secondPlayer.getPlayerId(),
                true
        );
    }

    private void lockBothWords() {
        service.lockSecretWord(
                room.getRoomId(),
                host.getPlayerId(),
                "TRAIN"
        );

        service.lockSecretWord(
                room.getRoomId(),
                secondPlayer.getPlayerId(),
                "CLOUD"
        );
    }
}