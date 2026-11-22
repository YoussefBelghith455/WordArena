package com.wordarena.server.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.wordarena.server.game.WordEvaluator.LetterState.ABSENT;
import static com.wordarena.server.game.WordEvaluator.LetterState.CORRECT;
import static com.wordarena.server.game.WordEvaluator.LetterState.PRESENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuelRoundTest {

    private GamePlayer firstPlayer;
    private GamePlayer secondPlayer;
    private DuelRound duelRound;

    @BeforeEach
    void setUp() {
        firstPlayer = new GamePlayer(
                "player-one",
                "Hedi"
        );

        secondPlayer = new GamePlayer(
                "player-two",
                "Youssef"
        );

        duelRound = new DuelRound(
                "TRAIN",
                List.of(
                        firstPlayer,
                        secondPlayer
                )
        );
    }

    @Test
    void shouldRequireExactlyTwoPlayers() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new DuelRound(
                                "TRAIN",
                                List.of(firstPlayer)
                        )
                );

        assertEquals(
                "DUEL mode requires exactly 2 players.",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectDuplicatePlayerIds() {
        GamePlayer duplicatePlayer =
                new GamePlayer(
                        "player-one",
                        "AnotherUsername"
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new DuelRound(
                                "TRAIN",
                                List.of(
                                        firstPlayer,
                                        duplicatePlayer
                                )
                        )
                );

        assertEquals(
                "Duel players must have unique IDs.",
                exception.getMessage()
        );
    }

    @Test
    void shouldRecordValidGuessAndFeedback() {
        DuelRound.GuessOutcome outcome =
                duelRound.submitGuess(
                        firstPlayer.getPlayerId(),
                        "RAINY"
                );

        assertFalse(outcome.roundFinished());
        assertEquals(1, outcome.attemptsUsed());
        assertEquals(5, outcome.attemptsRemaining());
        assertEquals(0, outcome.score());
        assertEquals("", outcome.revealedSecretWord());

        assertEquals(
                List.of(
                        PRESENT,
                        PRESENT,
                        PRESENT,
                        PRESENT,
                        ABSENT
                ),
                outcome.evaluation()
                        .feedback()
                        .stream()
                        .map(
                                WordEvaluator.LetterFeedback::state
                        )
                        .toList()
        );
    }

    @Test
    void shouldAwardMaximumScoreForFirstAttemptWin() {
        DuelRound.GuessOutcome outcome =
                duelRound.submitGuess(
                        firstPlayer.getPlayerId(),
                        "TRAIN"
                );

        assertTrue(outcome.roundFinished());
        assertTrue(outcome.evaluation().solved());
        assertEquals(1, outcome.attemptsUsed());
        assertEquals(5, outcome.attemptsRemaining());
        assertEquals(1000, outcome.score());

        assertEquals(
                firstPlayer.getPlayerId(),
                outcome.winnerPlayerId()
        );

        assertEquals(
                "TRAIN",
                outcome.revealedSecretWord()
        );

        assertTrue(duelRound.isFinished());

        assertEquals(
                firstPlayer.getPlayerId(),
                duelRound.getWinnerPlayerId()
                        .orElseThrow()
        );
    }

    @Test
    void shouldReduceScoreAfterIncorrectGuess() {
        duelRound.submitGuess(
                firstPlayer.getPlayerId(),
                "CLOUD"
        );

        DuelRound.GuessOutcome outcome =
                duelRound.submitGuess(
                        firstPlayer.getPlayerId(),
                        "TRAIN"
                );

        assertTrue(outcome.roundFinished());
        assertEquals(2, outcome.attemptsUsed());
        assertEquals(900, outcome.score());
    }

    @Test
    void shouldRejectGuessFromUnknownPlayer() {
        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> duelRound.submitGuess(
                                "unknown-player",
                                "TRAIN"
                        )
                );

        assertEquals(
                "The player does not belong to this Duel round.",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectGuessAfterRoundFinishes() {
        duelRound.submitGuess(
                firstPlayer.getPlayerId(),
                "TRAIN"
        );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> duelRound.submitGuess(
                                secondPlayer.getPlayerId(),
                                "TRAIN"
                        )
                );

        assertEquals(
                "The Duel round has already finished.",
                exception.getMessage()
        );
    }

    @Test
    void shouldHideSecretBeforeRoundFinishes() {
        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        duelRound::revealSecretWord
                );

        assertEquals(
                "The secret word cannot be revealed "
                        + "before the round finishes.",
                exception.getMessage()
        );
    }

    @Test
    void shouldFinishWithoutWinnerAfterBothPlayersUseSixAttempts() {
        List<String> incorrectGuesses = List.of(
                "APPLE",
                "BEACH",
                "CLOUD",
                "DREAM",
                "HOUSE",
                "MOUSE"
        );

        DuelRound.GuessOutcome lastOutcome = null;

        for (String guess : incorrectGuesses) {
            lastOutcome = duelRound.submitGuess(
                    firstPlayer.getPlayerId(),
                    guess
            );
        }

        assertFalse(lastOutcome.roundFinished());

        for (String guess : incorrectGuesses) {
            lastOutcome = duelRound.submitGuess(
                    secondPlayer.getPlayerId(),
                    guess
            );
        }

        assertTrue(lastOutcome.roundFinished());
        assertTrue(
                duelRound.getWinnerPlayerId().isEmpty()
        );

        assertEquals(
                "TRAIN",
                lastOutcome.revealedSecretWord()
        );

        assertEquals(
                0,
                duelRound.getAttemptsRemaining(
                        firstPlayer.getPlayerId()
                )
        );

        assertEquals(
                0,
                duelRound.getAttemptsRemaining(
                        secondPlayer.getPlayerId()
                )
        );
    }

    @Test
    void shouldRejectSeventhAttemptFromOnePlayer() {
        List<String> incorrectGuesses = List.of(
                "APPLE",
                "BEACH",
                "CLOUD",
                "DREAM",
                "HOUSE",
                "MOUSE"
        );

        for (String guess : incorrectGuesses) {
            duelRound.submitGuess(
                    firstPlayer.getPlayerId(),
                    guess
            );
        }

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> duelRound.submitGuess(
                                firstPlayer.getPlayerId(),
                                "STONE"
                        )
                );

        assertEquals(
                "This player has no attempts remaining.",
                exception.getMessage()
        );
    }

    @Test
    void shouldNormalizeLowercaseSecretAndGuess() {
        DuelRound lowercaseRound = new DuelRound(
                "train",
                List.of(
                        firstPlayer,
                        secondPlayer
                )
        );

        DuelRound.GuessOutcome outcome =
                lowercaseRound.submitGuess(
                        firstPlayer.getPlayerId(),
                        "train"
                );

        assertTrue(outcome.evaluation().solved());
        assertEquals(
                List.of(
                        CORRECT,
                        CORRECT,
                        CORRECT,
                        CORRECT,
                        CORRECT
                ),
                outcome.evaluation()
                        .feedback()
                        .stream()
                        .map(
                                WordEvaluator.LetterFeedback::state
                        )
                        .toList()
        );
    }
}