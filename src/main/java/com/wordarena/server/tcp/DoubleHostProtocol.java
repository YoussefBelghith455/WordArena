package com.wordarena.server.tcp;

import com.wordarena.server.game.DoubleHostRound;
import com.wordarena.server.game.WordEvaluator;

import java.util.Objects;
import java.util.stream.Collectors;

public final class DoubleHostProtocol {

    public static final String LOCK_WORD =
            "LOCK_WORD";

    public static final String WORD_LOCKED =
            "WORD_LOCKED";

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

    private DoubleHostProtocol() {
    }

    public static String parseSecretWord(
            String content
    ) {
        return validateWord(
                content,
                "LOCK_WORD requires exactly "
                        + WordEvaluator.WORD_LENGTH
                        + " English letters."
        );
    }

    public static String parseGuess(
            String content
    ) {
        return validateWord(
                content,
                "GUESS requires exactly "
                        + WordEvaluator.WORD_LENGTH
                        + " English letters."
        );
    }

    public static String encodeWordLocked(
            boolean bothWordsLocked
    ) {
        return String.join(
                FIELD_SEPARATOR,
                "DOUBLE_HOST",
                "LOCKED",
                Boolean.toString(
                        bothWordsLocked
                )
        );
    }

    public static String encodeRoundStarted(
            DoubleHostRound round
    ) {
        Objects.requireNonNull(
                round,
                "Double Host round is required."
        );

        return String.join(
                FIELD_SEPARATOR,
                "DOUBLE_HOST",
                "ACTIVE",
                Integer.toString(
                        DoubleHostRound.MAX_ATTEMPTS
                ),
                Long.toString(
                        round.getStartedAtTimestamp()
                )
        );
    }

    public static String encodeGuessResult(
            DoubleHostRound.GuessOutcome outcome
    ) {
        Objects.requireNonNull(
                outcome,
                "Double Host guess outcome is required."
        );

        String feedback =
                outcome.evaluation()
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
                "DOUBLE_HOST",
                outcome.evaluation().guessedWord(),
                feedback,
                Integer.toString(
                        outcome.attemptsUsed()
                ),
                Integer.toString(
                        outcome.attemptsRemaining()
                ),
                outcome.playerId(),
                Integer.toString(
                        outcome.score()
                ),
                Boolean.toString(
                        outcome.evaluation().solved()
                )
        );
    }

    public static String encodeRoundProgress(
            DoubleHostRound round
    ) {
        Objects.requireNonNull(
                round,
                "Double Host round is required."
        );

        return String.join(
                FIELD_SEPARATOR,
                "DOUBLE_HOST",
                round.isFinished()
                        ? "FINISHED"
                        : "ACTIVE",
                round.getWinnerPlayerId()
                        .orElse("")
        );
    }

    public static String encodeGameOver(
            DoubleHostRound round
    ) {
        Objects.requireNonNull(
                round,
                "Double Host round is required."
        );

        if (!round.isFinished()) {
            throw new IllegalStateException(
                    "GAME_OVER cannot be encoded before "
                            + "the Double Host round finishes."
            );
        }

        String winnerId =
                round.getWinnerPlayerId()
                        .orElse("");

        String result =
                winnerId.isBlank()
                        ? "NO_WINNER"
                        : "WINNER";

        String firstPlayerId =
                round.getPlayerIds().get(0);

        String secondPlayerId =
                round.getPlayerIds().get(1);

        return String.join(
                FIELD_SEPARATOR,
                "DOUBLE_HOST",
                result,
                winnerId,
                firstPlayerId,
                round.revealTargetWord(
                        firstPlayerId
                ),
                secondPlayerId,
                round.revealTargetWord(
                        secondPlayerId
                )
        );
    }

    private static String validateWord(
            String content,
            String errorMessage
    ) {
        if (content == null
                || content.isBlank()) {

            throw RoomProtocol.protocolError(
                    RoomProtocol.ErrorCode
                            .INVALID_REQUEST_FORMAT,
                    errorMessage
            );
        }

        String normalizedWord =
                content.trim().toUpperCase();

        if (normalizedWord.length()
                != WordEvaluator.WORD_LENGTH
                || !normalizedWord.matches("[A-Z]+")) {

            throw RoomProtocol.protocolError(
                    RoomProtocol.ErrorCode
                            .INVALID_REQUEST_FORMAT,
                    errorMessage
            );
        }

        return normalizedWord;
    }
}