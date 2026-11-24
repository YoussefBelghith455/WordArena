package com.wordarena.server.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoubleHostRoundTest {

    private GamePlayer firstPlayer;
    private GamePlayer secondPlayer;
    private DoubleHostRound round;

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

        round = new DoubleHostRound(
                List.of(
                        firstPlayer,
                        secondPlayer
                ),
                Map.of(
                        firstPlayer.getPlayerId(),
                        "TRAIN",
                        secondPlayer.getPlayerId(),
                        "CLOUD"
                )
        );
    }

    @Test
    void shouldRequireExactlyTwoPlayers() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DoubleHostRound(
                        List.of(firstPlayer),
                        Map.of(
                                firstPlayer.getPlayerId(),
                                "TRAIN"
                        )
                )
        );
    }

    @Test
    void shouldRequireBothLockedWords() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DoubleHostRound(
                        List.of(
                                firstPlayer,
                                secondPlayer
                        ),
                        Map.of(
                                firstPlayer.getPlayerId(),
                                "TRAIN"
                        )
                )
        );
    }

    @Test
    void shouldAssignOpponentWordAsTarget() {
        DoubleHostRound.GuessOutcome outcome =
                round.submitGuess(
                        firstPlayer.getPlayerId(),
                        "CLOUD"
                );

        assertTrue(outcome.evaluation().solved());

        assertEquals(
                firstPlayer.getPlayerId(),
                outcome.winnerPlayerId()
        );
    }

    @Test
    void shouldGiveEachPlayerIndependentAttempts() {
        DoubleHostRound.GuessOutcome firstOutcome =
                round.submitGuess(
                        firstPlayer.getPlayerId(),
                        "APPLE"
                );

        DoubleHostRound.GuessOutcome secondOutcome =
                round.submitGuess(
                        secondPlayer.getPlayerId(),
                        "BEACH"
                );

        assertEquals(
                1,
                firstOutcome.attemptsUsed()
        );

        assertEquals(
                1,
                secondOutcome.attemptsUsed()
        );

        assertEquals(
                5,
                round.getAttemptsRemaining(
                        firstPlayer.getPlayerId()
                )
        );

        assertEquals(
                5,
                round.getAttemptsRemaining(
                        secondPlayer.getPlayerId()
                )
        );
    }

    @Test
    void shouldAwardWinnerOnFirstAttempt() {
        DoubleHostRound.GuessOutcome outcome =
                round.submitGuess(
                        secondPlayer.getPlayerId(),
                        "TRAIN"
                );

        assertTrue(outcome.roundFinished());

        assertEquals(
                secondPlayer.getPlayerId(),
                outcome.winnerPlayerId()
        );

        assertEquals(
                1000,
                outcome.score()
        );

        assertEquals(
                1000,
                secondPlayer.getScore()
        );
    }

    @Test
    void shouldReduceScoreAfterFailedAttempt() {
        round.submitGuess(
                firstPlayer.getPlayerId(),
                "APPLE"
        );

        DoubleHostRound.GuessOutcome outcome =
                round.submitGuess(
                        firstPlayer.getPlayerId(),
                        "CLOUD"
                );

        assertEquals(
                900,
                outcome.score()
        );
    }

    @Test
    void shouldHideTargetBeforeFinish() {
        assertThrows(
                IllegalStateException.class,
                () -> round.revealTargetWord(
                        firstPlayer.getPlayerId()
                )
        );
    }

    @Test
    void shouldRevealBothTargetsAfterFinish() {
        round.submitGuess(
                firstPlayer.getPlayerId(),
                "CLOUD"
        );

        assertEquals(
                "CLOUD",
                round.revealTargetWord(
                        firstPlayer.getPlayerId()
                )
        );

        assertEquals(
                "TRAIN",
                round.revealTargetWord(
                        secondPlayer.getPlayerId()
                )
        );
    }

    @Test
    void shouldRejectGuessAfterWinner() {
        round.submitGuess(
                firstPlayer.getPlayerId(),
                "CLOUD"
        );

        assertThrows(
                IllegalStateException.class,
                () -> round.submitGuess(
                        secondPlayer.getPlayerId(),
                        "TRAIN"
                )
        );
    }

    @Test
    void shouldFinishWithoutWinnerAfterBothExhaustAttempts() {
        List<String> failedGuesses =
                List.of(
                        "APPLE",
                        "BEACH",
                        "DREAM",
                        "HOUSE",
                        "MOUSE",
                        "STONE"
                );

        for (String guess : failedGuesses) {
            round.submitGuess(
                    firstPlayer.getPlayerId(),
                    guess
            );
        }

        assertFalse(round.isFinished());

        for (String guess : failedGuesses) {
            round.submitGuess(
                    secondPlayer.getPlayerId(),
                    guess
            );
        }

        assertTrue(round.isFinished());

        assertTrue(
                round.getWinnerPlayerId().isEmpty()
        );
    }
}