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

class CooperativeRoundTest {

    private GamePlayer firstPlayer;
    private GamePlayer secondPlayer;
    private CooperativeRound cooperativeRound;

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

        cooperativeRound = new CooperativeRound(
                "TRAIN",
                List.of(
                        firstPlayer,
                        secondPlayer
                )
        );
    }

    @Test
    void shouldRequireAtLeastTwoPlayers() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new CooperativeRound(
                                "TRAIN",
                                List.of(firstPlayer)
                        )
                );

        assertEquals(
                "COOPERATIVE mode requires at least 2 players.",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectDuplicatePlayerIds() {
        GamePlayer duplicatePlayer =
                new GamePlayer(
                        "player-one",
                        "AnotherPlayer"
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new CooperativeRound(
                                "TRAIN",
                                List.of(
                                        firstPlayer,
                                        duplicatePlayer
                                )
                        )
                );

        assertEquals(
                "Cooperative players must have unique IDs.",
                exception.getMessage()
        );
    }

    @Test
    void shouldUseOneSharedAttemptPool() {
        CooperativeRound.GuessOutcome firstOutcome =
                cooperativeRound.submitGuess(
                        firstPlayer.getPlayerId(),
                        "CLOUD"
                );

        CooperativeRound.GuessOutcome secondOutcome =
                cooperativeRound.submitGuess(
                        secondPlayer.getPlayerId(),
                        "APPLE"
                );

        assertEquals(
                1,
                firstOutcome.attemptsUsed()
        );

        assertEquals(
                5,
                firstOutcome.attemptsRemaining()
        );

        assertEquals(
                2,
                secondOutcome.attemptsUsed()
        );

        assertEquals(
                4,
                secondOutcome.attemptsRemaining()
        );

        assertEquals(
                2,
                cooperativeRound.getAttemptsUsed()
        );
    }

    @Test
    void shouldStoreSharedGuessHistoryInOrder() {
        cooperativeRound.submitGuess(
                firstPlayer.getPlayerId(),
                "CLOUD"
        );

        cooperativeRound.submitGuess(
                secondPlayer.getPlayerId(),
                "APPLE"
        );

        List<CooperativeRound.SharedGuess> history =
                cooperativeRound.getGuessHistory();

        assertEquals(
                2,
                history.size()
        );

        assertEquals(
                1,
                history.getFirst().attemptNumber()
        );

        assertEquals(
                firstPlayer.getPlayerId(),
                history.getFirst().playerId()
        );

        assertEquals(
                "CLOUD",
                history.getFirst()
                        .evaluation()
                        .guessedWord()
        );

        assertEquals(
                2,
                history.get(1).attemptNumber()
        );

        assertEquals(
                secondPlayer.getPlayerId(),
                history.get(1).playerId()
        );

        assertEquals(
                "APPLE",
                history.get(1)
                        .evaluation()
                        .guessedWord()
        );
    }

    @Test
    void shouldReturnWordleFeedbackForSharedGuess() {
        CooperativeRound.GuessOutcome outcome =
                cooperativeRound.submitGuess(
                        firstPlayer.getPlayerId(),
                        "RAINY"
                );

        assertEquals(
                List.of(
                        PRESENT,
                        PRESENT,
                        PRESENT,
                        PRESENT,
                        ABSENT
                ),
                outcome.sharedGuess()
                        .evaluation()
                        .feedback()
                        .stream()
                        .map(
                                WordEvaluator.LetterFeedback::state
                        )
                        .toList()
        );

        assertFalse(outcome.solved());
        assertFalse(outcome.roundFinished());
    }

    @Test
    void shouldFinishWhenAnyPlayerSolvesWord() {
        cooperativeRound.submitGuess(
                firstPlayer.getPlayerId(),
                "CLOUD"
        );

        CooperativeRound.GuessOutcome outcome =
                cooperativeRound.submitGuess(
                        secondPlayer.getPlayerId(),
                        "TRAIN"
                );

        assertTrue(outcome.solved());
        assertTrue(outcome.roundFinished());

        assertEquals(
                secondPlayer.getPlayerId(),
                outcome.solvingPlayerId()
        );

        assertEquals(
                2,
                outcome.attemptsUsed()
        );

        assertEquals(
                4,
                outcome.attemptsRemaining()
        );

        assertEquals(
                600,
                outcome.teamScore()
        );

        assertEquals(
                "TRAIN",
                outcome.revealedSecretWord()
        );
    }

    @Test
    void shouldAwardTeamScoreToEveryPlayer() {
        cooperativeRound.submitGuess(
                secondPlayer.getPlayerId(),
                "TRAIN"
        );

        assertEquals(
                600,
                firstPlayer.getScore()
        );

        assertEquals(
                600,
                secondPlayer.getScore()
        );

        assertTrue(firstPlayer.isSolved());
        assertTrue(secondPlayer.isSolved());
    }

    @Test
    void shouldExposeCorrectFeedbackForSolvedWord() {
        CooperativeRound.GuessOutcome outcome =
                cooperativeRound.submitGuess(
                        firstPlayer.getPlayerId(),
                        "TRAIN"
                );

        assertEquals(
                List.of(
                        CORRECT,
                        CORRECT,
                        CORRECT,
                        CORRECT,
                        CORRECT
                ),
                outcome.sharedGuess()
                        .evaluation()
                        .feedback()
                        .stream()
                        .map(
                                WordEvaluator.LetterFeedback::state
                        )
                        .toList()
        );
    }

    @Test
    void shouldFinishAfterSixSharedFailedAttempts() {
        List<String> failedGuesses = List.of(
                "APPLE",
                "BEACH",
                "CLOUD",
                "DREAM",
                "HOUSE",
                "MOUSE"
        );

        CooperativeRound.GuessOutcome finalOutcome = null;

        for (int index = 0;
             index < failedGuesses.size();
             index++) {

            String playerId =
                    index % 2 == 0
                            ? firstPlayer.getPlayerId()
                            : secondPlayer.getPlayerId();

            finalOutcome =
                    cooperativeRound.submitGuess(
                            playerId,
                            failedGuesses.get(index)
                    );
        }

        assertTrue(finalOutcome.roundFinished());
        assertFalse(finalOutcome.solved());

        assertEquals(
                6,
                finalOutcome.attemptsUsed()
        );

        assertEquals(
                0,
                finalOutcome.attemptsRemaining()
        );

        assertEquals(
                "",
                finalOutcome.solvingPlayerId()
        );

        assertEquals(
                0,
                finalOutcome.teamScore()
        );

        assertEquals(
                "TRAIN",
                finalOutcome.revealedSecretWord()
        );

        assertTrue(cooperativeRound.isFinished());
        assertFalse(cooperativeRound.isSolved());
    }

    @Test
    void shouldRejectGuessAfterRoundFinishes() {
        cooperativeRound.submitGuess(
                firstPlayer.getPlayerId(),
                "TRAIN"
        );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> cooperativeRound.submitGuess(
                                secondPlayer.getPlayerId(),
                                "CLOUD"
                        )
                );

        assertEquals(
                "The Cooperative round has already finished.",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectUnknownPlayer() {
        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> cooperativeRound.submitGuess(
                                "unknown-player",
                                "TRAIN"
                        )
                );

        assertEquals(
                "The player does not belong "
                        + "to this Cooperative round.",
                exception.getMessage()
        );
    }

    @Test
    void shouldHideSecretBeforeRoundFinishes() {
        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        cooperativeRound::revealSecretWord
                );

        assertEquals(
                "The secret word cannot be revealed "
                        + "before the round finishes.",
                exception.getMessage()
        );
    }

    @Test
    void shouldNormalizeLowercaseSecretAndGuess() {
        CooperativeRound lowercaseRound =
                new CooperativeRound(
                        "train",
                        List.of(
                                firstPlayer,
                                secondPlayer
                        )
                );

        CooperativeRound.GuessOutcome outcome =
                lowercaseRound.submitGuess(
                        firstPlayer.getPlayerId(),
                        "train"
                );

        assertTrue(outcome.solved());

        assertEquals(
                "TRAIN",
                outcome.sharedGuess()
                        .evaluation()
                        .guessedWord()
        );
    }
}