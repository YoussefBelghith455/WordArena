package com.wordarena.server.tcp;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerSessionRegistry {

    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 20;

    private final Map<String, PlayerSession> sessionsByPlayerId;
    private final Map<String, String> playerIdsByNormalizedUsername;

    public PlayerSessionRegistry() {
        this.sessionsByPlayerId = new ConcurrentHashMap<>();
        this.playerIdsByNormalizedUsername = new ConcurrentHashMap<>();
    }

    public RegistrationResult register(String username) {
        String validationError = validateUsername(username);

        if (validationError != null) {
            return RegistrationResult.failure(validationError);
        }

        String trimmedUsername = username.trim();
        String normalizedUsername = normalizeUsername(trimmedUsername);
        String playerId = UUID.randomUUID().toString();

        String existingPlayerId =
                playerIdsByNormalizedUsername.putIfAbsent(
                        normalizedUsername,
                        playerId
                );

        if (existingPlayerId != null) {
            return RegistrationResult.failure(
                    "This username is already connected."
            );
        }

        PlayerSession session =
                new PlayerSession(playerId, trimmedUsername);

        sessionsByPlayerId.put(playerId, session);

        return RegistrationResult.success(session);
    }

    public Optional<PlayerSession> findByPlayerId(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                sessionsByPlayerId.get(playerId)
        );
    }

    public boolean remove(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return false;
        }

        PlayerSession removedSession =
                sessionsByPlayerId.remove(playerId);

        if (removedSession == null) {
            return false;
        }

        playerIdsByNormalizedUsername.remove(
                normalizeUsername(removedSession.username()),
                playerId
        );

        return true;
    }

    public int getConnectedPlayerCount() {
        return sessionsByPlayerId.size();
    }

    private String validateUsername(String username) {
        if (username == null || username.isBlank()) {
            return "Username is required.";
        }

        String trimmedUsername = username.trim();

        if (trimmedUsername.length() < MIN_USERNAME_LENGTH) {
            return "Username must contain at least "
                    + MIN_USERNAME_LENGTH
                    + " characters.";
        }

        if (trimmedUsername.length() > MAX_USERNAME_LENGTH) {
            return "Username must contain at most "
                    + MAX_USERNAME_LENGTH
                    + " characters.";
        }

        if (!trimmedUsername.matches("[A-Za-z0-9_]+")) {
            return "Username may contain only letters, numbers, "
                    + "and underscores.";
        }

        return null;
    }

    private String normalizeUsername(String username) {
        return username.toLowerCase();
    }

    public record PlayerSession(
            String playerId,
            String username
    ) {
    }

    public record RegistrationResult(
            boolean successful,
            PlayerSession session,
            String errorMessage
    ) {

        public static RegistrationResult success(
                PlayerSession session
        ) {
            return new RegistrationResult(
                    true,
                    session,
                    null
            );
        }

        public static RegistrationResult failure(
                String errorMessage
        ) {
            return new RegistrationResult(
                    false,
                    null,
                    errorMessage
            );
        }
    }
}