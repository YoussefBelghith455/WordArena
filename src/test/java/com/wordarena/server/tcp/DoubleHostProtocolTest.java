package com.wordarena.server.tcp;

import com.wordarena.server.game.DoubleHostRound;
import com.wordarena.server.game.GamePlayer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoubleHostProtocolTest {

    @Test
    void shouldParseWords() {
        assertEquals(
                "TRAIN",
                DoubleHostProtocol.parseSecretWord(
                        " train "
                )
        );

        assertEquals(
                "CLOUD",
                DoubleHostProtocol.parseGuess(
                        "cloud"
                )
        );
    }

    @Test
    void shouldEncodeLockState() {
        assertEquals(
                "DOUBLE_HOST|LOCKED|false",
                DoubleHostProtocol.encodeWordLocked(
                        false
                )
        );

        assertEquals(
                "DOUBLE_HOST|LOCKED|true",
                DoubleHostProtocol.encodeWordLocked(
                        true
                )
        );
    }

    @Test
    void shouldEncodeRoundStart() {
        assertTrue(
                DoubleHostProtocol.encodeRoundStarted(
                        createRound()
                ).startsWith(
                        "DOUBLE_HOST|ACTIVE|6|"
                )
        );
    }

    @Test
    void shouldEncodeGuessResult() {
        DoubleHostRound round =
                createRound();

        DoubleHostRound.GuessOutcome outcome =
                round.submitGuess(
                        "player-one",
                        "APPLE"
                );

        String encoded =
                DoubleHostProtocol.encodeGuessResult(
                        outcome
                );

        assertTrue(
                encoded.startsWith(
                        "DOUBLE_HOST|APPLE|"
                )
        );

        assertTrue(
                encoded.contains(
                        "|1|5|player-one|0|false"
                )
        );
    }

    @Test
    void shouldEncodeWinner() {
        DoubleHostRound round =
                createRound();

        round.submitGuess(
                "player-one",
                "CLOUD"
        );

        String encoded =
                DoubleHostProtocol.encodeGameOver(
                        round
                );

        assertTrue(
                encoded.startsWith(
                        "DOUBLE_HOST|WINNER|player-one|"
                )
        );

        assertTrue(
                encoded.contains(
                        "|player-one|CLOUD|"
                )
        );

        assertTrue(
                encoded.endsWith(
                        "|player-two|TRAIN"
                )
        );
    }

    @Test
    void shouldRejectEarlyGameOver() {
        assertThrows(
                IllegalStateException.class,
                () -> DoubleHostProtocol
                        .encodeGameOver(
                                createRound()
                        )
        );
    }

    private DoubleHostRound createRound() {
        GamePlayer first =
                new GamePlayer(
                        "player-one",
                        "Hedi"
                );

        GamePlayer second =
                new GamePlayer(
                        "player-two",
                        "Youssef"
                );

        return new DoubleHostRound(
                List.of(
                        first,
                        second
                ),
                Map.of(
                        first.getPlayerId(),
                        "TRAIN",
                        second.getPlayerId(),
                        "CLOUD"
                )
        );
    }
}