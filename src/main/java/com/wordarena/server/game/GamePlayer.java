package com.wordarena.server.game;

import com.wordarena.common.PlayerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class GamePlayer {

    private final String playerId;
    private final String username;
    private final List<String> guessHistory;

    private boolean ready;
    private int attemptsUsed;
    private int score;
    private boolean solved;
    private long solvedAtTimestamp;

    public GamePlayer(
            String playerId,
            String username
    ) {
        this.playerId = requireText(
                playerId,
                "Player ID"
        );

        this.username = requireText(
                username,
                "Username"
        );

        this.guessHistory = new ArrayList<>();
    }

    public synchronized void setReady(boolean ready) {
        this.ready = ready;
    }

    public synchronized void recordGuess(String guessedWord) {
        if (guessedWord == null || guessedWord.isBlank()) {
            throw new IllegalArgumentException(
                    "Guessed word is required."
            );
        }

        guessHistory.add(guessedWord);
        attemptsUsed++;
    }

    public synchronized void markSolved() {
        if (solved) {
            return;
        }

        solved = true;
        solvedAtTimestamp = System.currentTimeMillis();
    }

    public synchronized void addScore(int points) {
        if (points < 0) {
            throw new IllegalArgumentException(
                    "Score points cannot be negative."
            );
        }

        score += points;
    }

    public synchronized void resetForNewRound() {
        ready = false;
        attemptsUsed = 0;
        score = 0;
        solved = false;
        solvedAtTimestamp = 0L;
        guessHistory.clear();
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getUsername() {
        return username;
    }

    public synchronized boolean isReady() {
        return ready;
    }

    public synchronized int getAttemptsUsed() {
        return attemptsUsed;
    }

    public synchronized int getScore() {
        return score;
    }

    public synchronized boolean isSolved() {
        return solved;
    }

    public synchronized long getSolvedAtTimestamp() {
        return solvedAtTimestamp;
    }

    public synchronized List<String> getGuessHistory() {
        return Collections.unmodifiableList(
                new ArrayList<>(guessHistory)
        );
    }

    public synchronized PlayerView toPlayerView() {
        return new PlayerView(
                playerId,
                username,
                ready,
                attemptsUsed,
                score,
                solved
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

        String trimmedValue = value.trim();

        if (trimmedValue.isEmpty()) {
            throw new IllegalArgumentException(
                    fieldName + " is required."
            );
        }

        return trimmedValue;
    }
}