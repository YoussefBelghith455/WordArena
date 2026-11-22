package com.wordarena.server.game;

import com.wordarena.common.GameMode;
import com.wordarena.common.RoomInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomManagerTest {

    private RoomManager roomManager;
    private GamePlayer host;
    private GamePlayer secondPlayer;

    @BeforeEach
    void setUp() {
        roomManager = new RoomManager();

        host = new GamePlayer(
                "player-host",
                "Hedi"
        );

        secondPlayer = new GamePlayer(
                "player-two",
                "Youssef"
        );
    }

    @Test
    void shouldCreateRoomAndAddHostAutomatically() {
        GameRoom room = roomManager.createRoom(
                "Word Masters",
                GameMode.DUEL,
                2,
                host
        );

        assertEquals(1, roomManager.getRoomCount());
        assertEquals(1, room.getCurrentPlayerCount());
        assertTrue(room.isHost(host.getPlayerId()));
        assertTrue(room.containsPlayer(host.getPlayerId()));

        assertEquals(
                room.getRoomId(),
                roomManager.findRoomIdForPlayer(
                        host.getPlayerId()
                ).orElseThrow()
        );
    }

    @Test
    void shouldAllowSecondPlayerToJoinRoom() {
        GameRoom room = roomManager.createRoom(
                "Duel Room",
                GameMode.DUEL,
                2,
                host
        );

        roomManager.joinRoom(
                room.getRoomId(),
                secondPlayer
        );

        assertEquals(2, room.getCurrentPlayerCount());
        assertTrue(
                room.containsPlayer(
                        secondPlayer.getPlayerId()
                )
        );
    }

    @Test
    void shouldRejectPlayerJoiningTwoRooms() {
        GameRoom firstRoom = roomManager.createRoom(
                "First Room",
                GameMode.DUEL,
                2,
                host
        );

        roomManager.joinRoom(
                firstRoom.getRoomId(),
                secondPlayer
        );

        GamePlayer anotherHost = new GamePlayer(
                "another-host",
                "AnotherHost"
        );

        GameRoom secondRoom = roomManager.createRoom(
                "Second Room",
                GameMode.COOPERATIVE,
                3,
                anotherHost
        );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> roomManager.joinRoom(
                                secondRoom.getRoomId(),
                                secondPlayer
                        )
                );

        assertEquals(
                "The player is already in another room.",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectJoiningFullRoom() {
        GameRoom room = roomManager.createRoom(
                "Full Room",
                GameMode.DUEL,
                2,
                host
        );

        roomManager.joinRoom(
                room.getRoomId(),
                secondPlayer
        );

        GamePlayer thirdPlayer = new GamePlayer(
                "player-three",
                "ThirdPlayer"
        );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> roomManager.joinRoom(
                                room.getRoomId(),
                                thirdPlayer
                        )
                );

        assertEquals(
                "The room is full.",
                exception.getMessage()
        );
    }

    @Test
    void shouldStartRoomWhenEveryPlayerIsReady() {
        GameRoom room = roomManager.createRoom(
                "Ready Room",
                GameMode.DUEL,
                2,
                host
        );

        roomManager.joinRoom(
                room.getRoomId(),
                secondPlayer
        );

        room.setPlayerReady(
                host.getPlayerId(),
                true
        );

        room.setPlayerReady(
                secondPlayer.getPlayerId(),
                true
        );

        room.start(host.getPlayerId());

        assertTrue(room.isStarted());
    }

    @Test
    void shouldRejectStartWhenPlayerIsNotReady() {
        GameRoom room = roomManager.createRoom(
                "Waiting Room",
                GameMode.DUEL,
                2,
                host
        );

        roomManager.joinRoom(
                room.getRoomId(),
                secondPlayer
        );

        room.setPlayerReady(
                host.getPlayerId(),
                true
        );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> room.start(
                                host.getPlayerId()
                        )
                );

        assertEquals(
                "Every player must be ready before starting.",
                exception.getMessage()
        );

        assertFalse(room.isStarted());
    }

    @Test
    void shouldRejectStartRequestedByNonHost() {
        GameRoom room = roomManager.createRoom(
                "Host Only Room",
                GameMode.DUEL,
                2,
                host
        );

        roomManager.joinRoom(
                room.getRoomId(),
                secondPlayer
        );

        room.setPlayerReady(
                host.getPlayerId(),
                true
        );

        room.setPlayerReady(
                secondPlayer.getPlayerId(),
                true
        );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> room.start(
                                secondPlayer.getPlayerId()
                        )
                );

        assertEquals(
                "Only the room host can start the game.",
                exception.getMessage()
        );
    }

    @Test
    void shouldRemoveNormalPlayerFromRoom() {
        GameRoom room = roomManager.createRoom(
                "Leave Room",
                GameMode.COOPERATIVE,
                3,
                host
        );

        roomManager.joinRoom(
                room.getRoomId(),
                secondPlayer
        );

        roomManager.leaveRoom(
                room.getRoomId(),
                secondPlayer.getPlayerId()
        );

        assertEquals(1, room.getCurrentPlayerCount());

        assertTrue(
                roomManager.findRoomIdForPlayer(
                        secondPlayer.getPlayerId()
                ).isEmpty()
        );
    }

    @Test
    void shouldDeleteRoomWhenHostLeaves() {
        GameRoom room = roomManager.createRoom(
                "Temporary Room",
                GameMode.HOST,
                2,
                host
        );

        roomManager.joinRoom(
                room.getRoomId(),
                secondPlayer
        );

        roomManager.leaveRoom(
                room.getRoomId(),
                host.getPlayerId()
        );

        assertEquals(0, roomManager.getRoomCount());
        assertTrue(
                roomManager.findRoom(
                        room.getRoomId()
                ).isEmpty()
        );

        assertTrue(
                roomManager.findRoomIdForPlayer(
                        secondPlayer.getPlayerId()
                ).isEmpty()
        );
    }

    @Test
    void shouldReturnOnlyRoomsThatHaveNotStarted() {
        GameRoom startedRoom = roomManager.createRoom(
                "Started Room",
                GameMode.DUEL,
                2,
                host
        );

        roomManager.joinRoom(
                startedRoom.getRoomId(),
                secondPlayer
        );

        startedRoom.setPlayerReady(
                host.getPlayerId(),
                true
        );

        startedRoom.setPlayerReady(
                secondPlayer.getPlayerId(),
                true
        );

        startedRoom.start(host.getPlayerId());

        GamePlayer anotherHost = new GamePlayer(
                "available-host",
                "AvailableHost"
        );

        GameRoom availableRoom = roomManager.createRoom(
                "Available Room",
                GameMode.COOPERATIVE,
                3,
                anotherHost
        );

        List<RoomInfo> availableRooms =
                roomManager.getAvailableRooms();

        assertEquals(1, availableRooms.size());

        assertEquals(
                availableRoom.getRoomId(),
                availableRooms.getFirst().getRoomId()
        );
    }
}