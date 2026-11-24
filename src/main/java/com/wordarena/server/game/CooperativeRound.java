package com.wordarena.server.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class CooperativeRound {

    public static final int MAX_SHARED_ATTEMPTS = 6;

    private static final int WIN_SCORE = 600;

    private final String secretWord;
    private final List<GamePlayer> players;
    private final List<SharedGuess> guessHistory;
    private final long startedAtTimestamp;

    private int attemptsUsed;
    private boolean finished;
    private boolean solved;
    private String solvingPlayerId;
    private long finishedAtTimestamp;

    public CooperativeRound(
            String secretWord,
            List<GamePlayer> players
    ) {
        this.secretWord =
                validateSecretWord(secretWord);

        Objects.requireNonNull(
                players,
                "Cooperative player list is required."
        );

        if (players.size() < 2) {
            throw new IllegalArgumentException(
                    "COOPERATIVE mode requires at least 2 players."
            );
        }

        this.players =
                validateAndCopyPlayers(players);

        this.guessHistory =
                new ArrayList<>();

        this.startedAtTimestamp =
                System.currentTimeMillis();
    }

    public synchronized GuessOutcome submitGuess(
            String playerId,
            String guessedWord
    ) {
        if (finished) {
            throw new IllegalStateException(
                    "The Cooperative round has already finished."
            );
        }

        GamePlayer player =
                requirePlayer(playerId);

        if (attemptsUsed >= MAX_SHARED_ATTEMPTS) {
            throw new IllegalStateException(
                    "The team has no attempts remaining."
            );
        }

        WordEvaluator.EvaluationResult evaluation =
                WordEvaluator.evaluate(
                        secretWord,
                        guessedWord
                );

        attemptsUsed++;

        player.recordGuess(
                evaluation.guessedWord()
        );

        SharedGuess sharedGuess =
                new SharedGuess(
                        attemptsUsed,
                        player.getPlayerId(),
                        player.getUsername(),
                        evaluation
                );

        guessHistory.add(sharedGuess);

        if (evaluation.solved()) {
            solved = true;
            finished = true;
            solvingPlayerId =
                    player.getPlayerId();

            awardTeamScore();
            markEveryPlayerSolved();

            finishedAtTimestamp =
                    System.currentTimeMillis();
        } else if (attemptsUsed
                >= MAX_SHARED_ATTEMPTS) {

            finished = true;
            finishedAtTimestamp =
                    System.currentTimeMillis();
        }

        return new GuessOutcome(
                sharedGuess,
                attemptsUsed,
                getAttemptsRemaining(),
                solved,
                finished,
                safeSolvingPlayerId(),
                solved ? WIN_SCORE : 0,
                finished ? secretWord : ""
        );
    }

    public synchronized boolean isFinished() {
        return finished;
    }

    public synchronized boolean isSolved() {
        return solved;
    }

    public synchronized int getAttemptsUsed() {
        return attemptsUsed;
    }

    public synchronized int getAttemptsRemaining() {
        return MAX_SHARED_ATTEMPTS
                - attemptsUsed;
    }

    public synchronized Optional<String>
    getSolvingPlayerId() {

        return Optional.ofNullable(
                solvingPlayerId
        );
    }

    public synchronized List<SharedGuess>
    getGuessHistory() {

        return Collections.unmodifiableList(
                new ArrayList<>(guessHistory)
        );
    }

    public List<String> getPlayerIds() {
        return players.stream()
                .map(GamePlayer::getPlayerId)
                .toList();
    }

    public long getStartedAtTimestamp() {
        return startedAtTimestamp;
    }

    public synchronized long getFinishedAtTimestamp() {
        return finishedAtTimestamp;
    }

    public synchronized String revealSecretWord() {
        if (!finished) {
            throw new IllegalStateException(
                    "The secret word cannot be revealed "
                            + "before the round finishes."
            );
        }

        return secretWord;
    }

    private GamePlayer requirePlayer(
            String playerId
    ) {
        if (playerId == null
                || playerId.isBlank()) {

            throw new IllegalArgumentException(
                    "Player ID is required."
            );
        }

        return players.stream()
                .filter(player ->
                        player.getPlayerId()
                                .equals(playerId.trim())
                )
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException(
                                "The player does not belong "
                                        + "to this Cooperative round."
                        )
                );
    }

    private void awardTeamScore() {
        for (GamePlayer player : players) {
            player.addScore(WIN_SCORE);
        }
    }

    private void markEveryPlayerSolved() {
        for (GamePlayer player : players) {
            player.markSolved();
        }
    }

    private String safeSolvingPlayerId() {
        return solvingPlayerId == null
                ? ""
                : solvingPlayerId;
    }

    private static List<GamePlayer>
    validateAndCopyPlayers(
            List<GamePlayer> players
    ) {

        List<GamePlayer> validatedPlayers =
                new ArrayList<>();

        for (GamePlayer player : players) {
            GamePlayer validatedPlayer =
                    Objects.requireNonNull(
                            player,
                            "Cooperative players cannot be null."
                    );

            boolean duplicateId =
                    validatedPlayers.stream()
                            .anyMatch(existingPlayer ->
                                    existingPlayer
                                            .getPlayerId()
                                            .equals(
                                                    validatedPlayer
                                                            .getPlayerId()
                                            )
                            );

            if (duplicateId) {
                throw new IllegalArgumentException(
                        "Cooperative players must have unique IDs."
                );
            }

            validatedPlayers.add(
                    validatedPlayer
            );
        }

        return List.copyOf(validatedPlayers);
    }

    private static String validateSecretWord(
            String secretWord
    ) {
        return WordEvaluator.evaluate(
                secretWord,
                secretWord
        ).guessedWord();
    }

    public record SharedGuess(
            int attemptNumber,
            String playerId,
            String username,
            WordEvaluator.EvaluationResult evaluation
    ) {
    }

    public record GuessOutcome(
            SharedGuess sharedGuess,
            int attemptsUsed,
            int attemptsRemaining,
            boolean solved,
            boolean roundFinished,
            String solvingPlayerId,
            int teamScore,
            String revealedSecretWord
    ) {
    }
}