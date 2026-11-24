package com.wordarena.server.tcp;

import com.wordarena.server.game.CooperativeRound;
import com.wordarena.server.game.WordEvaluator;

import java.util.Objects;
import java.util.stream.Collectors;

public final class CooperativeProtocol {

    public static final String ROUND_STATE =
            "ROUND_STATE";

    public static final String GUESS_RESULT =
            "GUESS_RESULT";

    public static final String GAME_OVER =
            "GAME_OVER";

    private static final String FIELD_SEPARATOR =
            "|";

    private static final String FEEDBACK_SEPARATOR =
            ",";

    private CooperativeProtocol() {
    }

    public static String parseGuess(String content) {
        if (content == null || content.isBlank()) {
            throw RoomProtocol.protocolError(
                    RoomProtocol.ErrorCode
                            .INVALID_REQUEST_FORMAT,
                    "GUESS requires a five-letter word."
            );
        }

        String normalizedGuess =
                content.trim().toUpperCase();

        if (normalizedGuess.length()
                != WordEvaluator.WORD_LENGTH) {

            throw RoomProtocol.protocolError(
                    RoomProtocol.ErrorCode
                            .INVALID_REQUEST_FORMAT,
                    "GUESS requires exactly "
                            + WordEvaluator.WORD_LENGTH
                            + " letters."
            );
        }

        if (!normalizedGuess.matches("[A-Z]+")) {
            throw RoomProtocol.protocolError(
                    RoomProtocol.ErrorCode
                            .INVALID_REQUEST_FORMAT,
                    "GUESS may contain only English letters."
            );
        }

        return normalizedGuess;
    }

    public static String encodeRoundStarted(
            CooperativeRound cooperativeRound
    ) {
        Objects.requireNonNull(
                cooperativeRound,
                "Cooperative round is required."
        );

        return String.join(
                FIELD_SEPARATOR,
                "COOPERATIVE",
                "ACTIVE",
                Integer.toString(
                        CooperativeRound.MAX_SHARED_ATTEMPTS
                ),
                Long.toString(
                        cooperativeRound.getStartedAtTimestamp()
                )
        );
    }

    public static String encodeGuessResult(
            CooperativeRound.GuessOutcome outcome
    ) {
        Objects.requireNonNull(
                outcome,
                "Cooperative guess outcome is required."
        );

        CooperativeRound.SharedGuess sharedGuess =
                outcome.sharedGuess();

        String feedback =
                sharedGuess.evaluation()
                        .feedback()
                        .stream()
                        .map(letterFeedback ->
                                letterFeedback.state().name()
                        )
                        .collect(
                                Collectors.joining(
                                        FEEDBACK_SEPARATOR
                                )
                        );

        return String.join(
                FIELD_SEPARATOR,
                "COOPERATIVE",
                sharedGuess.evaluation().guessedWord(),
                feedback,
                Integer.toString(
                        sharedGuess.attemptNumber()
                ),
                Integer.toString(
                        outcome.attemptsUsed()
                ),
                Integer.toString(
                        outcome.attemptsRemaining()
                ),
                sharedGuess.playerId(),
                sharedGuess.username(),
                Boolean.toString(
                        outcome.solved()
                ),
                Integer.toString(
                        outcome.teamScore()
                )
        );
    }

    public static String encodeRoundProgress(
            CooperativeRound cooperativeRound
    ) {
        Objects.requireNonNull(
                cooperativeRound,
                "Cooperative round is required."
        );

        return String.join(
                FIELD_SEPARATOR,
                "COOPERATIVE",
                cooperativeRound.isFinished()
                        ? "FINISHED"
                        : "ACTIVE",
                Integer.toString(
                        cooperativeRound.getAttemptsUsed()
                ),
                Integer.toString(
                        cooperativeRound.getAttemptsRemaining()
                ),
                cooperativeRound.getSolvingPlayerId()
                        .orElse("")
        );
    }

    public static String encodeGameOver(
            CooperativeRound cooperativeRound
    ) {
        Objects.requireNonNull(
                cooperativeRound,
                "Cooperative round is required."
        );

        if (!cooperativeRound.isFinished()) {
            throw new IllegalStateException(
                    "GAME_OVER cannot be encoded "
                            + "before the Cooperative round finishes."
            );
        }

        String result =
                cooperativeRound.isSolved()
                        ? "TEAM_WIN"
                        : "TEAM_LOSS";

        return String.join(
                FIELD_SEPARATOR,
                "COOPERATIVE",
                result,
                cooperativeRound.getSolvingPlayerId()
                        .orElse(""),
                cooperativeRound.revealSecretWord(),
                Integer.toString(
                        cooperativeRound.getAttemptsUsed()
                )
        );
    }
}