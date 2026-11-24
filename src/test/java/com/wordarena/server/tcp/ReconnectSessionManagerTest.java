package com.wordarena.server.tcp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReconnectSessionManagerTest {

    private MutableClock clock;
    private ReconnectSessionManager manager;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(
                1_000_000L
        );

        manager =
                new ReconnectSessionManager(
                        120_000L,
                        clock,
                        new SecureRandom()
                );
    }

    @Test
    void shouldCreateSecureReconnectSession() {
        ReconnectSession session =
                manager.createSession(
                        "player-one",
                        "Hedi"
                );

        assertEquals(
                "player-one",
                session.playerId()
        );

        assertEquals(
                "Hedi",
                session.username()
        );

        assertFalse(
                session.reconnectToken()
                        .isBlank()
        );

        assertEquals(
                1,
                manager.getSessionCount()
        );
    }

    @Test
    void shouldCreateDifferentTokens() {
        ReconnectSession first =
                manager.createSession(
                        "player-one",
                        "Hedi"
                );

        ReconnectSession second =
                manager.createSession(
                        "player-two",
                        "Youssef"
                );

        assertNotEquals(
                first.reconnectToken(),
                second.reconnectToken()
        );
    }

    @Test
    void shouldStoreRoomMembership() {
        manager.createSession(
                "player-one",
                "Hedi"
        );

        ReconnectSession updated =
                manager.updateRoom(
                        "player-one",
                        "ROOM1234"
                );

        assertEquals(
                "ROOM1234",
                updated.getRoomId()
                        .orElseThrow()
        );
    }

    @Test
    void shouldMarkSessionDisconnected() {
        manager.createSession(
                "player-one",
                "Hedi"
        );

        ReconnectSession disconnected =
                manager.markDisconnected(
                        "player-one"
                );

        assertTrue(
                disconnected.isDisconnected()
        );

        assertEquals(
                clock.millis(),
                disconnected
                        .disconnectedAtTimestamp()
        );
    }

    @Test
    void shouldRestoreDisconnectedSession() {
        ReconnectSession created =
                manager.createSession(
                        "player-one",
                        "Hedi"
                );

        manager.updateRoom(
                "player-one",
                "ROOM1234"
        );

        manager.markDisconnected(
                "player-one"
        );

        clock.advanceMillis(
                30_000L
        );

        ReconnectSession restored =
                manager.restoreSession(
                        created.reconnectToken()
                );

        assertFalse(
                restored.isDisconnected()
        );

        assertEquals(
                "player-one",
                restored.playerId()
        );

        assertEquals(
                "ROOM1234",
                restored.getRoomId()
                        .orElseThrow()
        );
    }

    @Test
    void shouldRejectAlreadyConnectedSession() {
        ReconnectSession session =
                manager.createSession(
                        "player-one",
                        "Hedi"
                );

        assertThrows(
                IllegalStateException.class,
                () -> manager.restoreSession(
                        session.reconnectToken()
                )
        );
    }

    @Test
    void shouldRejectExpiredToken() {
        ReconnectSession session =
                manager.createSession(
                        "player-one",
                        "Hedi"
                );

        manager.markDisconnected(
                "player-one"
        );

        clock.advanceMillis(
                120_001L
        );

        assertThrows(
                IllegalStateException.class,
                () -> manager.restoreSession(
                        session.reconnectToken()
                )
        );

        assertEquals(
                0,
                manager.getSessionCount()
        );
    }

    @Test
    void shouldRemoveExpiredSessions() {
        manager.createSession(
                "player-one",
                "Hedi"
        );

        manager.markDisconnected(
                "player-one"
        );

        clock.advanceMillis(
                120_001L
        );

        assertEquals(
                1,
                manager.removeExpiredSessions()
                        .size()
        );

        assertTrue(
                manager.findByPlayerId(
                        "player-one"
                ).isEmpty()
        );
    }

    @Test
    void shouldInvalidatePlayerSession() {
        manager.createSession(
                "player-one",
                "Hedi"
        );

        assertTrue(
                manager.invalidatePlayerSession(
                        "player-one"
                )
        );

        assertFalse(
                manager.invalidatePlayerSession(
                        "player-one"
                )
        );
    }

    private static final class MutableClock
            extends Clock {

        private long currentTimestamp;

        private MutableClock(
                long currentTimestamp
        ) {
            this.currentTimestamp =
                    currentTimestamp;
        }

        private void advanceMillis(
                long milliseconds
        ) {
            currentTimestamp +=
                    milliseconds;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(
                java.time.ZoneId zone
        ) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(
                    currentTimestamp
            );
        }

        @Override
        public long millis() {
            return currentTimestamp;
        }
    }
}