package com.wordarena.server.tcp;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ReconnectSessionManager {

    public static final long DEFAULT_RECONNECT_WINDOW_MILLIS =
            120_000L;

    private static final int TOKEN_BYTE_LENGTH = 32;

    private final long reconnectWindowMillis;
    private final Clock clock;
    private final SecureRandom secureRandom;

    private final Map<String, ReconnectSession>
            sessionsByToken;

    private final Map<String, String>
            tokenByPlayerId;

    public ReconnectSessionManager() {
        this(
                DEFAULT_RECONNECT_WINDOW_MILLIS,
                Clock.systemUTC(),
                new SecureRandom()
        );
    }

    ReconnectSessionManager(
            long reconnectWindowMillis,
            Clock clock,
            SecureRandom secureRandom
    ) {
        if (reconnectWindowMillis <= 0) {
            throw new IllegalArgumentException(
                    "Reconnect window must be positive."
            );
        }

        this.reconnectWindowMillis =
                reconnectWindowMillis;

        this.clock = Objects.requireNonNull(
                clock,
                "Clock is required."
        );

        this.secureRandom =
                Objects.requireNonNull(
                        secureRandom,
                        "Secure random generator is required."
                );

        this.sessionsByToken =
                new ConcurrentHashMap<>();

        this.tokenByPlayerId =
                new ConcurrentHashMap<>();
    }

    public ReconnectSession createSession(
            String playerId,
            String username
    ) {
        String validatedPlayerId =
                requireText(
                        playerId,
                        "Player ID"
                );

        String validatedUsername =
                requireText(
                        username,
                        "Username"
                );

        invalidatePlayerSession(
                validatedPlayerId
        );

        long currentTimestamp =
                clock.millis();

        String reconnectToken =
                generateUniqueToken();

        ReconnectSession session =
                new ReconnectSession(
                        reconnectToken,
                        validatedPlayerId,
                        validatedUsername,
                        "",
                        currentTimestamp,
                        0,
                        currentTimestamp
                                + reconnectWindowMillis
                );

        sessionsByToken.put(
                reconnectToken,
                session
        );

        tokenByPlayerId.put(
                validatedPlayerId,
                reconnectToken
        );

        return session;
    }

    public Optional<ReconnectSession>
    findByToken(
            String reconnectToken
    ) {

        if (reconnectToken == null
                || reconnectToken.isBlank()) {

            return Optional.empty();
        }

        ReconnectSession session =
                sessionsByToken.get(
                        reconnectToken.trim()
                );

        if (session == null) {
            return Optional.empty();
        }

        if (session.isExpired(
                clock.millis()
        )) {
            invalidateToken(
                    session.reconnectToken()
            );

            return Optional.empty();
        }

        return Optional.of(session);
    }

    public Optional<ReconnectSession>
    findByPlayerId(
            String playerId
    ) {

        if (playerId == null
                || playerId.isBlank()) {

            return Optional.empty();
        }

        String reconnectToken =
                tokenByPlayerId.get(
                        playerId.trim()
                );

        if (reconnectToken == null) {
            return Optional.empty();
        }

        return findByToken(
                reconnectToken
        );
    }

    public ReconnectSession updateRoom(
            String playerId,
            String roomId
    ) {
        ReconnectSession session =
                requirePlayerSession(
                        playerId
                );

        ReconnectSession updatedSession =
                session.withRoomId(roomId);

        sessionsByToken.put(
                session.reconnectToken(),
                updatedSession
        );

        return updatedSession;
    }

    public ReconnectSession clearRoom(
            String playerId
    ) {
        return updateRoom(
                playerId,
                ""
        );
    }

    public ReconnectSession markDisconnected(
            String playerId
    ) {
        ReconnectSession session =
                requirePlayerSession(
                        playerId
                );

        long currentTimestamp =
                clock.millis();

        ReconnectSession disconnectedSession =
                session.markDisconnected(
                        currentTimestamp,
                        currentTimestamp
                                + reconnectWindowMillis
                );

        sessionsByToken.put(
                session.reconnectToken(),
                disconnectedSession
        );

        return disconnectedSession;
    }

    public ReconnectSession restoreSession(
            String reconnectToken
    ) {
        ReconnectSession session =
                findByToken(reconnectToken)
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "Reconnect token is invalid "
                                                + "or has expired."
                                )
                        );

        if (!session.isDisconnected()) {
            throw new IllegalStateException(
                    "The player session is already connected."
            );
        }

        long currentTimestamp =
                clock.millis();

        ReconnectSession restoredSession =
                session.markConnected(
                        currentTimestamp
                                + reconnectWindowMillis
                );

        sessionsByToken.put(
                restoredSession.reconnectToken(),
                restoredSession
        );

        return restoredSession;
    }

    public List<ReconnectSession>
    removeExpiredSessions() {

        long currentTimestamp =
                clock.millis();

        List<ReconnectSession> expiredSessions =
                new ArrayList<>();

        for (ReconnectSession session :
                sessionsByToken.values()) {

            if (session.isExpired(
                    currentTimestamp
            )) {
                boolean removed =
                        sessionsByToken.remove(
                                session.reconnectToken(),
                                session
                        );

                if (removed) {
                    tokenByPlayerId.remove(
                            session.playerId(),
                            session.reconnectToken()
                    );

                    expiredSessions.add(
                            session
                    );
                }
            }
        }

        return List.copyOf(
                expiredSessions
        );
    }

    public boolean invalidatePlayerSession(
            String playerId
    ) {
        if (playerId == null
                || playerId.isBlank()) {

            return false;
        }

        String reconnectToken =
                tokenByPlayerId.remove(
                        playerId.trim()
                );

        if (reconnectToken == null) {
            return false;
        }

        return sessionsByToken.remove(
                reconnectToken
        ) != null;
    }

    public boolean invalidateToken(
            String reconnectToken
    ) {
        if (reconnectToken == null
                || reconnectToken.isBlank()) {

            return false;
        }

        ReconnectSession removedSession =
                sessionsByToken.remove(
                        reconnectToken.trim()
                );

        if (removedSession == null) {
            return false;
        }

        tokenByPlayerId.remove(
                removedSession.playerId(),
                removedSession.reconnectToken()
        );

        return true;
    }

    public int getSessionCount() {
        return sessionsByToken.size();
    }

    public long getReconnectWindowMillis() {
        return reconnectWindowMillis;
    }

    private ReconnectSession requirePlayerSession(
            String playerId
    ) {
        return findByPlayerId(playerId)
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Reconnect session not found."
                        )
                );
    }

    private String generateUniqueToken() {
        String reconnectToken;

        do {
            byte[] tokenBytes =
                    new byte[TOKEN_BYTE_LENGTH];

            secureRandom.nextBytes(
                    tokenBytes
            );

            reconnectToken =
                    Base64.getUrlEncoder()
                            .withoutPadding()
                            .encodeToString(
                                    tokenBytes
                            );
        } while (sessionsByToken.containsKey(
                reconnectToken
        ));

        return reconnectToken;
    }

    private static String requireText(
            String value,
            String fieldName
    ) {
        Objects.requireNonNull(
                value,
                fieldName + " is required."
        );

        String trimmedValue =
                value.trim();

        if (trimmedValue.isEmpty()) {
            throw new IllegalArgumentException(
                    fieldName + " is required."
            );
        }

        return trimmedValue;
    }
}