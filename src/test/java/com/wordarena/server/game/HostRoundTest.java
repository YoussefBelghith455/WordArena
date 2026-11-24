package com.wordarena.server.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HostRoundTest {

    private GamePlayer host;
    private GamePlayer firstGuesser;
    private GamePlayer secondGuesser;
    private HostRound hostRound;

    @BeforeEach
    void setUp() {
        host = new GamePlayer(
                "host-player",
                "Hedi"
        );

        firstGuesser = new GamePlayer(
                "guesser-one",
                "Youssef"
        );

        secondGuesser = new GamePlayer(
                "guesser-two",
                "Amine"
        );

        hostRound = new HostRound(
                "TRAIN",
                host,
                List.of(
                        host,
                        firstGuesser,
                        secondGuesser
                )
        );
    }

    @Test
    void shouldRejectHostGuess() {
        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> hostRound.submitGuess(
                                host.getPlayerId(),
                                "TRAIN"
                        )
                );

        assertEquals(
                "The host cannot submit guesses.",
                exception.getMessage()
        );
    }

    @Test
    void shouldUseSharedGuesserAttempts() {
        HostRound.GuessOutcome firstOutcome =
                hostRound.submitGuess(
                        firstGuesser.getPlayerId(),
                        "CLOUD"
                );

        HostRound.GuessOutcome secondOutcome =
                hostRound.submitGuess(
                        secondGuesser.getPlayerId(),
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
    }

    @Test
    void shouldAwardGuessersWhenWordIsSolved() {
        HostRound.GuessOutcome outcome =
                hostRound.submitGuess(
                        firstGuesser.getPlayerId(),
                        "TRAIN"
                );

        assertTrue(outcome.solved());
        assertTrue(outcome.roundFinished());

        assertEquals(
                "GUESSERS",
                outcome.winningSide()
        );

        assertEquals(
                firstGuesser.getPlayerId(),
                outcome.solvingPlayerId()
        );

        assertEquals(
                600,
                firstGuesser.getScore()
        );

        assertEquals(
                600,
                secondGuesser.getScore()
        );

        assertEquals(
                0,
                host.getScore()
        );

        assertTrue(firstGuesser.isSolved());
        assertTrue(secondGuesser.isSolved());
        assertFalse(host.isSolved());
    }

    @Test
    void shouldAwardHostWhenGuessersFail() {
        List<String> guesses = List.of(
                "APPLE",
                "BEACH",
                "CLOUD",
                "DREAM",
                "HOUSE",
                "MOUSE"
        );

        HostRound.GuessOutcome finalOutcome =
                null;

        for (int index = 0;
             index < guesses.size();
             index++) {

            finalOutcome =
                    hostRound.submitGuess(
                            index % 2 == 0
                                    ? firstGuesser.getPlayerId()
                                    : secondGuesser.getPlayerId(),
                            guesses.get(index)
                    );
        }

        assertTrue(finalOutcome.roundFinished());
        assertFalse(finalOutcome.solved());

        assertEquals(
                "HOST",
                finalOutcome.winningSide()
        );

        assertEquals(
                600,
                host.getScore()
        );

        assertEquals(
                0,
                firstGuesser.getScore()
        );

        assertEquals(
                0,
                secondGuesser.getScore()
        );

        assertEquals(
                "TRAIN",
                finalOutcome.revealedSecretWord()
        );
    }

    @Test
    void shouldStoreSharedGuessHistory() {
        hostRound.submitGuess(
                firstGuesser.getPlayerId(),
                "CLOUD"
        );

        hostRound.submitGuess(
                secondGuesser.getPlayerId(),
                "APPLE"
        );

        assertEquals(
                2,
                hostRound.getGuessHistory().size()
        );

        assertEquals(
                firstGuesser.getPlayerId(),
                hostRound.getGuessHistory()
                        .getFirst()
                        .playerId()
        );

        assertEquals(
                secondGuesser.getPlayerId(),
                hostRound.getGuessHistory()
                        .get(1)
                        .playerId()
        );
    }

    @Test
    void shouldHideSecretBeforeFinish() {
        assertThrows(
                IllegalStateException.class,
                hostRound::revealSecretWord
        );
    }

    @Test
    void shouldRejectUnknownGuesser() {
        assertThrows(
                IllegalStateException.class,
                () -> hostRound.submitGuess(
                        "unknown-player",
                        "TRAIN"
                )
        );
    }

    @Test
    void shouldRejectGuessAfterFinish() {
        hostRound.submitGuess(
                firstGuesser.getPlayerId(),
                "TRAIN"
        );

        assertThrows(
                IllegalStateException.class,
                () -> hostRound.submitGuess(
                        secondGuesser.getPlayerId(),
                        "CLOUD"
                )
        );
    }
}