package com.wordarena.server.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class HostRound {

    public static final int MAX_SHARED_ATTEMPTS = 6;

    private static final int WIN_SCORE = 600;

    private final String secretWord;
    private final GamePlayer host;
    private final List<GamePlayer> guessers;
    private final List<HostedGuess> guessHistory;
    private final long startedAtTimestamp;

    private int attemptsUsed;
    private boolean finished;
    private boolean solved;
    private String solvingPlayerId;
    private long finishedAtTimestamp;

    public HostRound(
            String secretWord,
            GamePlayer host,
            List<GamePlayer> players
    ) {
        this.secretWord =
                validateSecretWord(secretWord);

        this.host = Objects.requireNonNull(
                host,
                "Host player is required."
        );

        Objects.requireNonNull(
                players,
                "Host-mode player list is required."
        );

        this.guessers =
                validateAndCopyGuessers(
                        players,
                        host.getPlayerId()
                );

        if (guessers.isEmpty()) {
            throw new IllegalArgumentException(
                    "HOST mode requires at least one guesser."
            );
        }

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
                    "The Host round has already finished."
            );
        }

        if (host.getPlayerId().equals(playerId)) {
            throw new IllegalStateException(
                    "The host cannot submit guesses."
            );
        }

        GamePlayer guesser =
                requireGuesser(playerId);

        if (attemptsUsed >= MAX_SHARED_ATTEMPTS) {
            throw new IllegalStateException(
                    "The guessing team has no attempts remaining."
            );
        }

        WordEvaluator.EvaluationResult evaluation =
                WordEvaluator.evaluate(
                        secretWord,
                        guessedWord
                );

        attemptsUsed++;

        guesser.recordGuess(
                evaluation.guessedWord()
        );

        HostedGuess hostedGuess =
                new HostedGuess(
                        attemptsUsed,
                        guesser.getPlayerId(),
                        guesser.getUsername(),
                        evaluation
                );

        guessHistory.add(hostedGuess);

        if (evaluation.solved()) {
            solved = true;
            finished = true;
            solvingPlayerId =
                    guesser.getPlayerId();

            awardGuessingTeam();
            markGuessingTeamSolved();

            finishedAtTimestamp =
                    System.currentTimeMillis();
        } else if (attemptsUsed
                >= MAX_SHARED_ATTEMPTS) {

            finished = true;
            host.addScore(WIN_SCORE);

            finishedAtTimestamp =
                    System.currentTimeMillis();
        }

        return new GuessOutcome(
                hostedGuess,
                attemptsUsed,
                getAttemptsRemaining(),
                solved,
                finished,
                safeSolvingPlayerId(),
                solved ? "GUESSERS" : (
                        finished ? "HOST" : ""
                ),
                WIN_SCORE,
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

    public String getHostPlayerId() {
        return host.getPlayerId();
    }

    public String getHostUsername() {
        return host.getUsername();
    }

    public synchronized Optional<String>
    getSolvingPlayerId() {

        return Optional.ofNullable(
                solvingPlayerId
        );
    }

    public synchronized List<HostedGuess>
    getGuessHistory() {

        return Collections.unmodifiableList(
                new ArrayList<>(guessHistory)
        );
    }

    public List<String> getGuesserPlayerIds() {
        return guessers.stream()
                .map(GamePlayer::getPlayerId)
                .toList();
    }

    public List<String> getParticipantPlayerIds() {
        List<String> participantIds =
                new ArrayList<>();

        participantIds.add(
                host.getPlayerId()
        );

        participantIds.addAll(
                getGuesserPlayerIds()
        );

        return List.copyOf(participantIds);
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
                            + "before the Host round finishes."
            );
        }

        return secretWord;
    }

    private GamePlayer requireGuesser(
            String playerId
    ) {
        if (playerId == null
                || playerId.isBlank()) {

            throw new IllegalArgumentException(
                    "Player ID is required."
            );
        }

        return guessers.stream()
                .filter(player ->
                        player.getPlayerId()
                                .equals(playerId.trim())
                )
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException(
                                "The player does not belong "
                                        + "to the guessing team."
                        )
                );
    }

    private void awardGuessingTeam() {
        for (GamePlayer guesser : guessers) {
            guesser.addScore(WIN_SCORE);
        }
    }

    private void markGuessingTeamSolved() {
        for (GamePlayer guesser : guessers) {
            guesser.markSolved();
        }
    }

    private String safeSolvingPlayerId() {
        return solvingPlayerId == null
                ? ""
                : solvingPlayerId;
    }

    private static List<GamePlayer>
    validateAndCopyGuessers(
            List<GamePlayer> players,
            String hostPlayerId
    ) {

        List<GamePlayer> validatedPlayers =
                new ArrayList<>();

        boolean hostFound = false;

        for (GamePlayer player : players) {
            GamePlayer validatedPlayer =
                    Objects.requireNonNull(
                            player,
                            "Host-mode players cannot be null."
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
                        "Host-mode players must have unique IDs."
                );
            }

            validatedPlayers.add(
                    validatedPlayer
            );

            if (validatedPlayer.getPlayerId()
                    .equals(hostPlayerId)) {

                hostFound = true;
            }
        }

        if (!hostFound) {
            throw new IllegalArgumentException(
                    "The host must belong to the room."
            );
        }

        return validatedPlayers.stream()
                .filter(player ->
                        !player.getPlayerId()
                                .equals(hostPlayerId)
                )
                .toList();
    }

    private static String validateSecretWord(
            String secretWord
    ) {
        return WordEvaluator.evaluate(
                secretWord,
                secretWord
        ).guessedWord();
    }

    public record HostedGuess(
            int attemptNumber,
            String playerId,
            String username,
            WordEvaluator.EvaluationResult evaluation
    ) {
    }

    public record GuessOutcome(
            HostedGuess hostedGuess,
            int attemptsUsed,
            int attemptsRemaining,
            boolean solved,
            boolean roundFinished,
            String solvingPlayerId,
            String winningSide,
            int awardedScore,
            String revealedSecretWord
    ) {
    }
}