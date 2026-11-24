package com.wordarena.server.game;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class DoubleHostRound {

    public static final int MAX_ATTEMPTS = 6;

    private static final int MAX_SCORE = 1000;

    private static final int SCORE_PENALTY_PER_EXTRA_ATTEMPT =
            100;

    private final Map<String, GamePlayer> playersById;
    private final Map<String, String> targetWordByPlayerId;
    private final long startedAtTimestamp;

    private boolean finished;
    private String winnerPlayerId;
    private long finishedAtTimestamp;

    public DoubleHostRound(
            List<GamePlayer> players,
            Map<String, String> lockedWordByPlayerId
    ) {
        Objects.requireNonNull(
                players,
                "Double Host player list is required."
        );

        Objects.requireNonNull(
                lockedWordByPlayerId,
                "Double Host locked words are required."
        );

        if (players.size() != 2) {
            throw new IllegalArgumentException(
                    "DOUBLE_HOST mode requires exactly 2 players."
            );
        }

        this.playersById =
                validateAndCopyPlayers(players);

        if (lockedWordByPlayerId.size() != 2) {
            throw new IllegalArgumentException(
                    "Both Double Host players must lock a secret word."
            );
        }

        this.targetWordByPlayerId =
                createTargetWordMapping(
                        lockedWordByPlayerId
                );

        this.startedAtTimestamp =
                System.currentTimeMillis();
    }

    public synchronized GuessOutcome submitGuess(
            String playerId,
            String guessedWord
    ) {
        if (finished) {
            throw new IllegalStateException(
                    "The Double Host round has already finished."
            );
        }

        GamePlayer player =
                requirePlayer(playerId);

        if (player.isSolved()) {
            throw new IllegalStateException(
                    "This player has already solved the opponent's word."
            );
        }

        if (player.getAttemptsUsed()
                >= MAX_ATTEMPTS) {

            throw new IllegalStateException(
                    "This player has no attempts remaining."
            );
        }

        String targetWord =
                targetWordByPlayerId.get(
                        player.getPlayerId()
                );

        WordEvaluator.EvaluationResult evaluation =
                WordEvaluator.evaluate(
                        targetWord,
                        guessedWord
                );

        player.recordGuess(
                evaluation.guessedWord()
        );

        if (evaluation.solved()) {
            player.markSolved();

            int awardedScore =
                    calculateScore(
                            player.getAttemptsUsed()
                    );

            player.addScore(awardedScore);

            winnerPlayerId =
                    player.getPlayerId();

            finished = true;

            finishedAtTimestamp =
                    System.currentTimeMillis();
        } else if (everyPlayerExhaustedAttempts()) {
            finished = true;

            finishedAtTimestamp =
                    System.currentTimeMillis();
        }

        return new GuessOutcome(
                player.getPlayerId(),
                evaluation,
                player.getAttemptsUsed(),
                MAX_ATTEMPTS
                        - player.getAttemptsUsed(),
                player.getScore(),
                finished,
                safeWinnerPlayerId(),
                finished
                        ? targetWord
                        : ""
        );
    }

    public synchronized boolean isFinished() {
        return finished;
    }

    public synchronized Optional<String>
    getWinnerPlayerId() {

        return Optional.ofNullable(
                winnerPlayerId
        );
    }

    public synchronized int getAttemptsRemaining(
            String playerId
    ) {
        GamePlayer player =
                requirePlayer(playerId);

        return MAX_ATTEMPTS
                - player.getAttemptsUsed();
    }

    public synchronized List<String>
    getPlayerIds() {

        return List.copyOf(
                playersById.keySet()
        );
    }

    public synchronized String revealTargetWord(
            String playerId
    ) {
        if (!finished) {
            throw new IllegalStateException(
                    "Double Host secret words cannot be revealed "
                            + "before the round finishes."
            );
        }

        requirePlayer(playerId);

        return targetWordByPlayerId.get(
                playerId
        );
    }

    public long getStartedAtTimestamp() {
        return startedAtTimestamp;
    }

    public synchronized long getFinishedAtTimestamp() {
        return finishedAtTimestamp;
    }

    private Map<String, GamePlayer>
    validateAndCopyPlayers(
            List<GamePlayer> players
    ) {

        Map<String, GamePlayer> validatedPlayers =
                new LinkedHashMap<>();

        for (GamePlayer player : players) {
            GamePlayer validatedPlayer =
                    Objects.requireNonNull(
                            player,
                            "Double Host players cannot be null."
                    );

            GamePlayer existingPlayer =
                    validatedPlayers.putIfAbsent(
                            validatedPlayer.getPlayerId(),
                            validatedPlayer
                    );

            if (existingPlayer != null) {
                throw new IllegalArgumentException(
                        "Double Host players must have unique IDs."
                );
            }
        }

        return validatedPlayers;
    }

    private Map<String, String>
    createTargetWordMapping(
            Map<String, String> lockedWordByPlayerId
    ) {

        for (String playerId :
                playersById.keySet()) {

            if (!lockedWordByPlayerId.containsKey(
                    playerId
            )) {
                throw new IllegalArgumentException(
                        "Both Double Host players must lock a secret word."
                );
            }
        }

        List<String> playerIds =
                List.copyOf(
                        playersById.keySet()
                );

        String firstPlayerId =
                playerIds.get(0);

        String secondPlayerId =
                playerIds.get(1);

        String firstPlayerWord =
                validateSecretWord(
                        lockedWordByPlayerId.get(
                                firstPlayerId
                        )
                );

        String secondPlayerWord =
                validateSecretWord(
                        lockedWordByPlayerId.get(
                                secondPlayerId
                        )
                );

        Map<String, String> targetWords =
                new LinkedHashMap<>();

        /*
         * Each player guesses the word selected
         * by the other player.
         */
        targetWords.put(
                firstPlayerId,
                secondPlayerWord
        );

        targetWords.put(
                secondPlayerId,
                firstPlayerWord
        );

        return targetWords;
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

        GamePlayer player =
                playersById.get(
                        playerId.trim()
                );

        if (player == null) {
            throw new IllegalStateException(
                    "The player does not belong "
                            + "to this Double Host round."
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

    private int calculateScore(
            int attemptsUsed
    ) {
        int score =
                MAX_SCORE
                        - (
                        attemptsUsed - 1
                ) * SCORE_PENALTY_PER_EXTRA_ATTEMPT;

        return Math.max(
                score,
                0
        );
    }

    private String safeWinnerPlayerId() {
        return winnerPlayerId == null
                ? ""
                : winnerPlayerId;
    }

    private static String validateSecretWord(
            String secretWord
    ) {
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
            String revealedTargetWord
    ) {
    }
}