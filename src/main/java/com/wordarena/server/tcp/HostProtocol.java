package com.wordarena.server.tcp;

import com.wordarena.server.game.HostRound;
import com.wordarena.server.game.WordEvaluator;

import java.util.Objects;
import java.util.stream.Collectors;

public final class HostProtocol {

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

    private HostProtocol() {
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

    public static String encodeWordLocked() {
        return "HOST|LOCKED";
    }

    public static String encodeRoundStarted(
            HostRound hostRound
    ) {
        Objects.requireNonNull(
                hostRound,
                "Host round is required."
        );

        return String.join(
                FIELD_SEPARATOR,
                "HOST",
                "ACTIVE",
                Integer.toString(
                        HostRound.MAX_SHARED_ATTEMPTS
                ),
                hostRound.getHostPlayerId(),
                Long.toString(
                        hostRound.getStartedAtTimestamp()
                )
        );
    }

    public static String encodeGuessResult(
            HostRound.GuessOutcome outcome
    ) {
        Objects.requireNonNull(
                outcome,
                "Host guess outcome is required."
        );

        HostRound.HostedGuess hostedGuess =
                outcome.hostedGuess();

        String feedback =
                hostedGuess.evaluation()
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
                "HOST",
                hostedGuess.evaluation().guessedWord(),
                feedback,
                Integer.toString(
                        hostedGuess.attemptNumber()
                ),
                Integer.toString(
                        outcome.attemptsUsed()
                ),
                Integer.toString(
                        outcome.attemptsRemaining()
                ),
                hostedGuess.playerId(),
                hostedGuess.username(),
                Boolean.toString(
                        outcome.solved()
                )
        );
    }

    public static String encodeRoundProgress(
            HostRound hostRound
    ) {
        Objects.requireNonNull(
                hostRound,
                "Host round is required."
        );

        return String.join(
                FIELD_SEPARATOR,
                "HOST",
                hostRound.isFinished()
                        ? "FINISHED"
                        : "ACTIVE",
                Integer.toString(
                        hostRound.getAttemptsUsed()
                ),
                Integer.toString(
                        hostRound.getAttemptsRemaining()
                ),
                hostRound.getSolvingPlayerId()
                        .orElse("")
        );
    }

    public static String encodeGameOver(
            HostRound hostRound
    ) {
        Objects.requireNonNull(
                hostRound,
                "Host round is required."
        );

        if (!hostRound.isFinished()) {
            throw new IllegalStateException(
                    "GAME_OVER cannot be encoded "
                            + "before the Host round finishes."
            );
        }

        return String.join(
                FIELD_SEPARATOR,
                "HOST",
                hostRound.isSolved()
                        ? "GUESSERS_WIN"
                        : "HOST_WINS",
                hostRound.getSolvingPlayerId()
                        .orElse(""),
                hostRound.getHostPlayerId(),
                hostRound.revealSecretWord(),
                Integer.toString(
                        hostRound.getAttemptsUsed()
                )
        );
    }

    private static String validateWord(
            String content,
            String errorMessage
    ) {
        if (content == null || content.isBlank()) {
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