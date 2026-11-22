package com.wordarena.server.tcp;

import com.wordarena.common.GameMode;
import com.wordarena.server.game.GamePlayer;
import com.wordarena.server.game.GameRoom;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomProtocolTest {

    @Test
    void shouldParseValidCreateRoomRequest() {
        RoomProtocol.CreateRoomRequest request =
                RoomProtocol.parseCreateRoom(
                        "Word Masters|DUEL|2"
                );

        assertEquals(
                "Word Masters",
                request.roomName()
        );

        assertEquals(
                GameMode.DUEL,
                request.gameMode()
        );

        assertEquals(
                2,
                request.maxPlayers()
        );
    }

    @Test
    void shouldAcceptCooperativeRoomWithFourPlayers() {
        RoomProtocol.CreateRoomRequest request =
                RoomProtocol.parseCreateRoom(
                        "Team Arena|COOPERATIVE|4"
                );

        assertEquals(
                GameMode.COOPERATIVE,
                request.gameMode()
        );

        assertEquals(
                4,
                request.maxPlayers()
        );
    }

    @Test
    void shouldRejectMalformedCreateRoomRequest() {
        RoomProtocol.ProtocolException exception =
                assertThrows(
                        RoomProtocol.ProtocolException.class,
                        () -> RoomProtocol.parseCreateRoom(
                                "Word Masters|DUEL"
                        )
                );

        assertEquals(
                RoomProtocol.ErrorCode.INVALID_REQUEST_FORMAT,
                exception.getErrorCode()
        );

        assertEquals(
                "INVALID_REQUEST_FORMAT|"
                        + "CREATE_ROOM requires "
                        + "roomName|gameMode|maxPlayers.",
                exception.toProtocolContent()
        );
    }

    @Test
    void shouldRejectInvalidGameMode() {
        RoomProtocol.ProtocolException exception =
                assertThrows(
                        RoomProtocol.ProtocolException.class,
                        () -> RoomProtocol.parseCreateRoom(
                                "Unknown Mode|SOLO|2"
                        )
                );

        assertEquals(
                RoomProtocol.ErrorCode.INVALID_GAME_MODE,
                exception.getErrorCode()
        );
    }

    @Test
    void shouldRejectNonNumericMaximumPlayers() {
        RoomProtocol.ProtocolException exception =
                assertThrows(
                        RoomProtocol.ProtocolException.class,
                        () -> RoomProtocol.parseCreateRoom(
                                "Broken Room|DUEL|two"
                        )
                );

        assertEquals(
                RoomProtocol.ErrorCode.INVALID_MAX_PLAYERS,
                exception.getErrorCode()
        );

        assertEquals(
                "Maximum players must be a number.",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectDuelWithMoreThanTwoPlayers() {
        RoomProtocol.ProtocolException exception =
                assertThrows(
                        RoomProtocol.ProtocolException.class,
                        () -> RoomProtocol.parseCreateRoom(
                                "Large Duel|DUEL|4"
                        )
                );

        assertEquals(
                RoomProtocol.ErrorCode.INVALID_MAX_PLAYERS,
                exception.getErrorCode()
        );

        assertEquals(
                "DUEL mode requires exactly 2 players.",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectDoubleHostWithMoreThanTwoPlayers() {
        RoomProtocol.ProtocolException exception =
                assertThrows(
                        RoomProtocol.ProtocolException.class,
                        () -> RoomProtocol.parseCreateRoom(
                                "Double Host|DOUBLE_HOST|3"
                        )
                );

        assertEquals(
                RoomProtocol.ErrorCode.INVALID_MAX_PLAYERS,
                exception.getErrorCode()
        );

        assertEquals(
                "DOUBLE_HOST mode requires exactly 2 players.",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectReservedSeparatorInRoomName() {
        RoomProtocol.ProtocolException exception =
                assertThrows(
                        RoomProtocol.ProtocolException.class,
                        () -> RoomProtocol.parseCreateRoom(
                                "Bad;Room|DUEL|2"
                        )
                );

        assertEquals(
                RoomProtocol.ErrorCode.INVALID_REQUEST_FORMAT,
                exception.getErrorCode()
        );
    }

    @Test
    void shouldParseReadyState() {
        assertTrue(
                RoomProtocol.parseReadyState("true")
        );

        assertTrue(
                RoomProtocol.parseReadyState(" TRUE ")
        );

        assertFalse(
                RoomProtocol.parseReadyState("false")
        );
    }

    @Test
    void shouldRejectInvalidReadyState() {
        RoomProtocol.ProtocolException exception =
                assertThrows(
                        RoomProtocol.ProtocolException.class,
                        () -> RoomProtocol.parseReadyState(
                                "yes"
                        )
                );

        assertEquals(
                RoomProtocol.ErrorCode.INVALID_REQUEST_FORMAT,
                exception.getErrorCode()
        );
    }

    @Test
    void shouldEncodeCompleteRoomState() {
        GamePlayer host = new GamePlayer(
                "player-host",
                "Hedi"
        );

        GameRoom room = new GameRoom(
                "ROOM1234",
                "Word Masters",
                GameMode.DUEL,
                2,
                host
        );

        assertEquals(
                "Word Masters|DUEL|1|2|false|player-host",
                RoomProtocol.encodeRoomState(room)
        );
    }

    @Test
    void shouldEncodeCompletePlayerList() {
        GamePlayer host = new GamePlayer(
                "player-host",
                "Hedi"
        );

        host.setReady(true);

        GameRoom room = new GameRoom(
                "ROOM1234",
                "Word Masters",
                GameMode.DUEL,
                2,
                host
        );

        GamePlayer secondPlayer = new GamePlayer(
                "player-two",
                "Youssef"
        );

        room.addPlayer(secondPlayer);

        assertEquals(
                "player-host~Hedi~true~0~0~false;"
                        + "player-two~Youssef~false~0~0~false",
                RoomProtocol.encodeRoomPlayers(
                        room.getPlayerViews()
                )
        );
    }

    @Test
    void shouldEncodeStableErrorCodeAndMessage() {
        String encodedError = RoomProtocol.encodeError(
                RoomProtocol.ErrorCode.ROOM_NOT_FOUND,
                "Room not found."
        );

        assertEquals(
                "ROOM_NOT_FOUND|Room not found.",
                encodedError
        );
    }
}