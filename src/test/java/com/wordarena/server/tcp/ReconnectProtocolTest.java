package com.wordarena.server.tcp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReconnectProtocolTest {

    @Test
    void shouldParseReconnectToken() {
        assertEquals(
                "token-value",
                ReconnectProtocol
                        .parseReconnectToken(
                                " token-value "
                        )
        );
    }

    @Test
    void shouldRejectMissingToken() {
        assertThrows(
                RoomProtocol.ProtocolException.class,
                () -> ReconnectProtocol
                        .parseReconnectToken(" ")
        );
    }

    @Test
    void shouldEncodeReconnectToken() {
        ReconnectSession session =
                createSession();

        assertEquals(
                "token-value|121000",
                ReconnectProtocol.encodeToken(
                        session
                )
        );
    }

    @Test
    void shouldEncodeReconnectSuccess() {
        ReconnectSession session =
                createSession()
                        .withRoomId(
                                "ROOM1234"
                        );

        assertEquals(
                "player-one|Hedi|ROOM1234|121000",
                ReconnectProtocol
                        .encodeReconnectSuccess(
                                session
                        )
        );
    }

    @Test
    void shouldNeverIncludeSecretData() {
        ReconnectSession session =
                createSession()
                        .withRoomId(
                                "ROOM1234"
                        );

        String encoded =
                ReconnectProtocol
                        .encodeReconnectSuccess(
                                session
                        );

        assertTrue(
                !encoded.contains("TRAIN")
                        && !encoded.contains("CLOUD")
        );
    }

    private ReconnectSession createSession() {
        return new ReconnectSession(
                "token-value",
                "player-one",
                "Hedi",
                "",
                1_000L,
                0,
                121_000L
        );
    }
}