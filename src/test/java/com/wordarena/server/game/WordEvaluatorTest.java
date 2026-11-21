package com.wordarena.server.game;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.wordarena.server.game.WordEvaluator.LetterState.ABSENT;
import static com.wordarena.server.game.WordEvaluator.LetterState.CORRECT;
import static com.wordarena.server.game.WordEvaluator.LetterState.PRESENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WordEvaluatorTest {

    @Test
    void shouldMarkEveryLetterCorrectWhenWordIsSolved() {
        WordEvaluator.EvaluationResult result =
                WordEvaluator.evaluate("TRAIN", "TRAIN");

        assertTrue(result.solved());

        assertEquals(
                List.of(
                        CORRECT,
                        CORRECT,
                        CORRECT,
                        CORRECT,
                        CORRECT
                ),
                statesOf(result)
        );
    }

    @Test
    void shouldMarkLettersPresentInOtherPositions() {
        WordEvaluator.EvaluationResult result =
                WordEvaluator.evaluate("TRAIN", "RAINY");

        assertFalse(result.solved());

        assertEquals(
                List.of(
                        PRESENT,
                        PRESENT,
                        PRESENT,
                        PRESENT,
                        ABSENT
                ),
                statesOf(result)
        );
    }

    @Test
    void shouldMarkCompletelyAbsentLetters() {
        WordEvaluator.EvaluationResult result =
                WordEvaluator.evaluate("TRAIN", "BLOCK");

        assertFalse(result.solved());

        assertEquals(
                List.of(
                        ABSENT,
                        ABSENT,
                        ABSENT,
                        ABSENT,
                        ABSENT
                ),
                statesOf(result)
        );
    }

    @Test
    void shouldNotReuseASecretLetterOccurrence() {
        WordEvaluator.EvaluationResult result =
                WordEvaluator.evaluate("APPLE", "ALLEY");

        assertEquals(
                List.of(
                        CORRECT,
                        PRESENT,
                        ABSENT,
                        PRESENT,
                        ABSENT
                ),
                statesOf(result)
        );
    }

    @Test
    void shouldNormalizeLowercaseWords() {
        WordEvaluator.EvaluationResult result =
                WordEvaluator.evaluate("train", "train");

        assertTrue(result.solved());
        assertEquals("TRAIN", result.guessedWord());
    }

    @Test
    void shouldRejectWordsWithIncorrectLength() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> WordEvaluator.evaluate(
                                "TRAIN",
                                "CAT"
                        )
                );

        assertEquals(
                "Guessed word must contain exactly 5 letters.",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNonAlphabeticCharacters() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> WordEvaluator.evaluate(
                                "TRAIN",
                                "TR4IN"
                        )
                );

        assertEquals(
                "Guessed word may contain only English letters.",
                exception.getMessage()
        );
    }

    private List<WordEvaluator.LetterState> statesOf(
            WordEvaluator.EvaluationResult result
    ) {
        return result.feedback()
                .stream()
                .map(WordEvaluator.LetterFeedback::state)
                .toList();
    }
}