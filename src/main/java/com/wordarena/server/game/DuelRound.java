package com.wordarena.server.game;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class DuelRound {

    public static final int MAX_ATTEMPTS = 6;
    private static final int MAX_SCORE = 1000;
    private static final int SCORE_PENALTY_PER_EXTRA_ATTEMPT = 100;

    private final String secretWord;
    private final Map<String, GamePlayer> playersById;
    private final long startedAtTimestamp;

    private boolean finished;
    private String winnerPlayerId;
    private long finishedAtTimestamp;

    public DuelRound(
            String secretWord,
            List<GamePlayer> players
    ) {
        this.secretWord = validateSecretWord(secretWord);

        Objects.requireNonNull(
                players,
                "Duel player list is required."
        );

        if (players.size() != 2) {
            throw new IllegalArgumentException(
                    "DUEL mode requires exactly 2 players."
            );
        }

        this.playersById = new LinkedHashMap<>();

        for (GamePlayer player : players) {
            GamePlayer validatedPlayer =
                    Objects.requireNonNull(
                            player,
                            "Duel players cannot be null."
                    );

            GamePlayer existingPlayer =
                    playersById.putIfAbsent(
                            validatedPlayer.getPlayerId(),
                            validatedPlayer
                    );

            if (existingPlayer != null) {
                throw new IllegalArgumentException(
                        "Duel players must have unique IDs."
                );
            }
        }

        this.startedAtTimestamp =
                System.currentTimeMillis();
    }

    public synchronized GuessOutcome submitGuess(
            String playerId,
            String guessedWord
    ) {
        if (finished) {
            throw new IllegalStateException(
                    "The Duel round has already finished."
            );
        }

        GamePlayer player = requirePlayer(playerId);

        if (player.isSolved()) {
            throw new IllegalStateException(
                    "This player has already solved the word."
            );
        }

        if (player.getAttemptsUsed() >= MAX_ATTEMPTS) {
            throw new IllegalStateException(
                    "This player has no attempts remaining."
            );
        }

        WordEvaluator.EvaluationResult evaluation =
                WordEvaluator.evaluate(
                        secretWord,
                        guessedWord
                );

        player.recordGuess(
                evaluation.guessedWord()
        );

        if (evaluation.solved()) {
            player.markSolved();

            int awardedScore = calculateScore(
                    player.getAttemptsUsed()
            );

            player.addScore(awardedScore);

            winnerPlayerId = player.getPlayerId();
            finished = true;
            finishedAtTimestamp =
                    System.currentTimeMillis();
        } else if (everyPlayerExhaustedAttempts()) {
            finished = true;
            finishedAtTimestamp =
                    System.currentTimeMillis();
        }

        int attemptsRemaining =
                MAX_ATTEMPTS - player.getAttemptsUsed();

        return new GuessOutcome(
                player.getPlayerId(),
                evaluation,
                player.getAttemptsUsed(),
                attemptsRemaining,
                player.getScore(),
                finished,
                winnerPlayerId,
                finished ? secretWord : ""
        );
    }

    public synchronized boolean isFinished() {
        return finished;
    }

    public synchronized Optional<String> getWinnerPlayerId() {
        return Optional.ofNullable(winnerPlayerId);
    }

    public synchronized int getAttemptsRemaining(
            String playerId
    ) {
        GamePlayer player = requirePlayer(playerId);

        return MAX_ATTEMPTS
                - player.getAttemptsUsed();
    }

    public synchronized List<String> getPlayerIds() {
        return List.copyOf(playersById.keySet());
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

    private GamePlayer requirePlayer(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            throw new IllegalArgumentException(
                    "Player ID is required."
            );
        }

        GamePlayer player = playersById.get(
                playerId.trim()
        );

        if (player == null) {
            throw new IllegalStateException(
                    "The player does not belong to this Duel round."
            );
        }

        return player;
    }

    private boolean everyPlayerExhaustedAttempts() {
        return playersById.values()
                .stream()
                .allMatch(player ->
                        player.getAttemptsUsed()
                                >= MAX_ATTEMPTS
                );
    }

    private int calculateScore(int attemptsUsed) {
        int score = MAX_SCORE
                - (
                attemptsUsed - 1
        ) * SCORE_PENALTY_PER_EXTRA_ATTEMPT;

        return Math.max(score, 0);
    }

    private static String validateSecretWord(
            String secretWord
    ) {
        /*
         * Calling the evaluator validates and normalizes
         * the secret using the same five-letter rules.
         */
        return WordEvaluator.evaluate(
                secretWord,
                secretWord
        ).guessedWord();
    }

    public record GuessOutcome(
            String playerId,
            WordEvaluator.EvaluationResult evaluation,
            int attemptsUsed,
            int attemptsRemaining,
            int score,
            boolean roundFinished,
            String winnerPlayerId,
            String revealedSecretWord
    ) {
    }
}