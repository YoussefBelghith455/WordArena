package com.wordarena.server.jms;

import com.wordarena.common.EventType;

import java.util.Objects;

public class GameEventNotifier {

    private final ServerEventPublisher eventPublisher;

    public GameEventNotifier(
            ServerEventPublisher eventPublisher
    ) {
        this.eventPublisher = Objects.requireNonNull(
                eventPublisher,
                "Server event publisher is required."
        );
    }

    public void playerJoined(
            String roomId,
            String playerId,
            String username
    ) {
        eventPublisher.publish(
                EventType.PLAYER_JOINED,
                roomId,
                playerId,
                safeUsername(username)
                        + " joined the room."
        );
    }

    public void playerLeft(
            String roomId,
            String playerId,
            String username
    ) {
        eventPublisher.publish(
                EventType.PLAYER_LEFT,
                roomId,
                playerId,
                safeUsername(username)
                        + " left the room."
        );
    }

    public void roundStarted(
            String roomId,
            String playerId,
            String username
    ) {
        eventPublisher.publish(
                EventType.ROUND_STARTED,
                roomId,
                playerId,
                "The round was started by "
                        + safeUsername(username)
                        + "."
        );
    }

    public void playerSolved(
            String roomId,
            String playerId,
            String username
    ) {
        eventPublisher.publish(
                EventType.PLAYER_SOLVED,
                roomId,
                playerId,
                safeUsername(username)
                        + " solved the word."
        );
    }

    public void scoreUpdated(
            String roomId,
            String playerId,
            String username,
            int score
    ) {
        if (score < 0) {
            throw new IllegalArgumentException(
                    "Score cannot be negative."
            );
        }

        eventPublisher.publish(
                EventType.SCORE_UPDATED,
                roomId,
                playerId,
                safeUsername(username)
                        + " now has "
                        + score
                        + " points."
        );
    }

    public void roundEnded(
            String roomId,
            String winnerPlayerId,
            String winnerUsername
    ) {
        String safeWinnerPlayerId =
                winnerPlayerId == null
                        ? ""
                        : winnerPlayerId.trim();

        String message;

        if (safeWinnerPlayerId.isBlank()) {
            message =
                    "The round ended without a winner.";
        } else {
            message =
                    "The round was won by "
                            + safeUsername(winnerUsername)
                            + ".";
        }

        eventPublisher.publish(
                EventType.ROUND_ENDED,
                roomId,
                safeWinnerPlayerId,
                message
        );
    }

    public boolean isEnabled() {
        return eventPublisher.isEnabled();
    }

    private String safeUsername(String username) {
        if (username == null || username.isBlank()) {
            return "A player";
        }

        return username.trim();
    }
}