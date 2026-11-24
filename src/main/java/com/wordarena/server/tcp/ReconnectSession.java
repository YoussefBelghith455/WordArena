package com.wordarena.server.tcp;

import java.util.Objects;
import java.util.Optional;

public record ReconnectSession(
        String reconnectToken,
        String playerId,
        String username,
        String roomId,
        long issuedAtTimestamp,
        long disconnectedAtTimestamp,
        long expiresAtTimestamp
) {

    public ReconnectSession {
        reconnectToken = requireText(
                reconnectToken,
                "Reconnect token"
        );

        playerId = requireText(
                playerId,
                "Player ID"
        );

        username = requireText(
                username,
                "Username"
        );

        roomId = roomId == null
                ? ""
                : roomId.trim();

        if (issuedAtTimestamp <= 0) {
            throw new IllegalArgumentException(
                    "Issue timestamp must be positive."
            );
        }

        if (disconnectedAtTimestamp < 0) {
            throw new IllegalArgumentException(
                    "Disconnect timestamp cannot be negative."
            );
        }

        if (expiresAtTimestamp <= issuedAtTimestamp) {
            throw new IllegalArgumentException(
                    "Expiration timestamp must be after "
                            + "the issue timestamp."
            );
        }
    }

    public boolean isDisconnected() {
        return disconnectedAtTimestamp > 0;
    }

    public boolean isExpired(
            long currentTimestamp
    ) {
        /*
         * A connected player's token remains valid for the
         * duration of the active connection. The short-lived
         * expiration window begins after disconnection.
         */
        return isDisconnected()
                && currentTimestamp
                >= expiresAtTimestamp;
    }

    public Optional<String> getRoomId() {
        if (roomId.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(roomId);
    }

    public ReconnectSession withRoomId(
            String updatedRoomId
    ) {
        return new ReconnectSession(
                reconnectToken,
                playerId,
                username,
                updatedRoomId,
                issuedAtTimestamp,
                disconnectedAtTimestamp,
                expiresAtTimestamp
        );
    }

    public ReconnectSession markDisconnected(
            long disconnectedTimestamp,
            long updatedExpirationTimestamp
    ) {
        if (disconnectedTimestamp <= 0) {
            throw new IllegalArgumentException(
                    "Disconnect timestamp must be positive."
            );
        }

        if (updatedExpirationTimestamp
                <= disconnectedTimestamp) {

            throw new IllegalArgumentException(
                    "Expiration timestamp must be after "
                            + "the disconnect timestamp."
            );
        }

        return new ReconnectSession(
                reconnectToken,
                playerId,
                username,
                roomId,
                issuedAtTimestamp,
                disconnectedTimestamp,
                updatedExpirationTimestamp
        );
    }

    public ReconnectSession markConnected(
            long updatedExpirationTimestamp
    ) {
        return new ReconnectSession(
                reconnectToken,
                playerId,
                username,
                roomId,
                issuedAtTimestamp,
                0,
                updatedExpirationTimestamp
        );
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