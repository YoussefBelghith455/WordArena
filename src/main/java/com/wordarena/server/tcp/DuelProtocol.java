package com.wordarena.server.tcp;

import com.wordarena.server.game.DuelRound;
import com.wordarena.server.game.WordEvaluator;

import java.util.Objects;
import java.util.stream.Collectors;

public final class DuelProtocol {

    public static final String START_ROUND = "START_ROUND";
    public static final String GUESS = "GUESS";

    public static final String ROUND_STATE = "ROUND_STATE";
    public static final String GUESS_RESULT = "GUESS_RESULT";
    public static final String GAME_OVER = "GAME_OVER";

    private static final String FIELD_SEPARATOR = "|";
    private static final String FEEDBACK_SEPARATOR = ",";

    private DuelProtocol() {
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
            DuelRound duelRound
    ) {
        Objects.requireNonNull(
                duelRound,
                "Duel round is required."
        );

        return String.join(
                FIELD_SEPARATOR,
                "ACTIVE",
                Integer.toString(
                        DuelRound.MAX_ATTEMPTS
                ),
                Long.toString(
                        duelRound.getStartedAtTimestamp()
                )
        );
    }

    public static String encodeGuessResult(
            DuelRound.GuessOutcome outcome
    ) {
        Objects.requireNonNull(
                outcome,
                "Guess outcome is required."
        );

        String feedback = outcome.evaluation()
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
                outcome.evaluation().guessedWord(),
                feedback,
                Integer.toString(
                        outcome.attemptsUsed()
                ),
                Integer.toString(
                        outcome.attemptsRemaining()
                ),
                Integer.toString(
                        outcome.score()
                ),
                Boolean.toString(
                        outcome.evaluation().solved()
                )
        );
    }

    public static String encodeRoundProgress(
            DuelRound duelRound
    ) {
        Objects.requireNonNull(
                duelRound,
                "Duel round is required."
        );

        String winnerPlayerId =
                duelRound.getWinnerPlayerId()
                        .orElse("");

        return String.join(
                FIELD_SEPARATOR,
                duelRound.isFinished()
                        ? "FINISHED"
                        : "ACTIVE",
                winnerPlayerId
        );
    }

    public static String encodeGameOver(
            DuelRound duelRound
    ) {
        Objects.requireNonNull(
                duelRound,
                "Duel round is required."
        );

        if (!duelRound.isFinished()) {
            throw new IllegalStateException(
                    "GAME_OVER cannot be encoded "
                            + "before the round finishes."
            );
        }

        String winnerPlayerId =
                duelRound.getWinnerPlayerId()
                        .orElse("");

        String result = winnerPlayerId.isBlank()
                ? "NO_WINNER"
                : "WINNER";

        return String.join(
                FIELD_SEPARATOR,
                result,
                winnerPlayerId,
                duelRound.revealSecretWord()
        );
    }
}