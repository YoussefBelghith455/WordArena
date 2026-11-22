package com.wordarena.server.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class WordEvaluator {

    public static final int WORD_LENGTH = 5;

    private WordEvaluator() {
    }

    public static EvaluationResult evaluate(
            String secretWord,
            String guessedWord
    ) {
        String normalizedSecret = normalizeWord(
                secretWord,
                "Secret word"
        );

        String normalizedGuess = normalizeWord(
                guessedWord,
                "Guessed word"
        );

        LetterState[] states = new LetterState[WORD_LENGTH];
        boolean[] consumedSecretLetters =
                new boolean[WORD_LENGTH];

        // First pass: letters in the correct position.
        for (int index = 0; index < WORD_LENGTH; index++) {
            if (normalizedGuess.charAt(index)
                    == normalizedSecret.charAt(index)) {

                states[index] = LetterState.CORRECT;
                consumedSecretLetters[index] = true;
            }
        }

        // Second pass: letters present in another position.
        for (int guessIndex = 0;
             guessIndex < WORD_LENGTH;
             guessIndex++) {

            if (states[guessIndex] == LetterState.CORRECT) {
                continue;
            }

            char guessedLetter =
                    normalizedGuess.charAt(guessIndex);

            int matchingSecretIndex = findUnusedMatch(
                    normalizedSecret,
                    guessedLetter,
                    consumedSecretLetters
            );

            if (matchingSecretIndex >= 0) {
                states[guessIndex] = LetterState.PRESENT;
                consumedSecretLetters[matchingSecretIndex] = true;
            } else {
                states[guessIndex] = LetterState.ABSENT;
            }
        }

        List<LetterFeedback> feedback = new ArrayList<>();

        for (int index = 0; index < WORD_LENGTH; index++) {
            feedback.add(
                    new LetterFeedback(
                            normalizedGuess.charAt(index),
                            states[index]
                    )
            );
        }

        return new EvaluationResult(
                normalizedGuess,
                Collections.unmodifiableList(feedback),
                normalizedGuess.equals(normalizedSecret)
        );
    }

    private static int findUnusedMatch(
            String secretWord,
            char guessedLetter,
            boolean[] consumedSecretLetters
    ) {
        for (int index = 0;
             index < WORD_LENGTH;
             index++) {

            if (!consumedSecretLetters[index]
                    && secretWord.charAt(index)
                    == guessedLetter) {

                return index;
            }
        }

        return -1;
    }

    private static String normalizeWord(
            String word,
            String fieldName
    ) {
        if (word == null || word.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " is required."
            );
        }

        String normalizedWord =
                word.trim().toUpperCase(Locale.ROOT);

        if (normalizedWord.length() != WORD_LENGTH) {
            throw new IllegalArgumentException(
                    fieldName + " must contain exactly "
                            + WORD_LENGTH
                            + " letters."
            );
        }

        if (!normalizedWord.matches("[A-Z]+")) {
            throw new IllegalArgumentException(
                    fieldName
                            + " may contain only English letters."
            );
        }

        return normalizedWord;
    }

    public enum LetterState {
        CORRECT,
        PRESENT,
        ABSENT
    }

    public record LetterFeedback(
            char letter,
            LetterState state
    ) {
    }

    public record EvaluationResult(
            String guessedWord,
            List<LetterFeedback> feedback,
            boolean solved
    ) {
    }
}