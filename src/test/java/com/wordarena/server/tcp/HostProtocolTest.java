package com.wordarena.server.tcp;

import com.wordarena.server.game.GamePlayer;
import com.wordarena.server.game.HostRound;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HostProtocolTest {

    @Test
    void shouldParseSecretAndGuessWords() {
        assertEquals(
                "TRAIN",
                HostProtocol.parseSecretWord(
                        " train "
                )
        );

        assertEquals(
                "CLOUD",
                HostProtocol.parseGuess(
                        "cloud"
                )
        );
    }

    @Test
    void shouldRejectInvalidWords() {
        assertThrows(
                RoomProtocol.ProtocolException.class,
                () -> HostProtocol.parseSecretWord(
                        "CAT"
                )
        );

        assertThrows(
                RoomProtocol.ProtocolException.class,
                () -> HostProtocol.parseGuess(
                        "TR4IN"
                )
        );
    }

    @Test
    void shouldEncodeWordLocked() {
        assertEquals(
                "HOST|LOCKED",
                HostProtocol.encodeWordLocked()
        );
    }

    @Test
    void shouldEncodeStartedRound() {
        HostRound round =
                createRound();

        assertTrue(
                HostProtocol.encodeRoundStarted(
                        round
                ).startsWith(
                        "HOST|ACTIVE|6|host-player|"
                )
        );
    }

    @Test
    void shouldEncodeGuessResult() {
        HostRound round =
                createRound();

        HostRound.GuessOutcome outcome =
                round.submitGuess(
                        "guesser-player",
                        "CLOUD"
                );

        String encoded =
                HostProtocol.encodeGuessResult(
                        outcome
                );

        assertTrue(
                encoded.startsWith(
                        "HOST|CLOUD|"
                )
        );

        assertTrue(
                encoded.contains(
                        "|1|1|5|guesser-player|Youssef|false"
                )
        );
    }

    @Test
    void shouldEncodeGuesserVictory() {
        HostRound round =
                createRound();

        round.submitGuess(
                "guesser-player",
                "TRAIN"
        );

        assertEquals(
                "HOST|GUESSERS_WIN|guesser-player|"
                        + "host-player|TRAIN|1",
                HostProtocol.encodeGameOver(
                        round
                )
        );
    }

    @Test
    void shouldRejectEarlyGameOver() {
        assertThrows(
                IllegalStateException.class,
                () -> HostProtocol.encodeGameOver(
                        createRound()
                )
        );
    }

    private HostRound createRound() {
        GamePlayer host =
                new GamePlayer(
                        "host-player",
                        "Hedi"
                );

        GamePlayer guesser =
                new GamePlayer(
                        "guesser-player",
                        "Youssef"
                );

        return new HostRound(
                "TRAIN",
                host,
                List.of(
                        host,
                        guesser
                )
        );
    }
}