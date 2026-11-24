package com.wordarena.server.tcp;

import com.wordarena.server.game.CooperativeRound;
import com.wordarena.server.game.GamePlayer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CooperativeProtocolTest {

    @Test
    void shouldParseValidGuess() {
        assertEquals(
                "TRAIN",
                CooperativeProtocol.parseGuess(
                        " train "
                )
        );
    }

    @Test
    void shouldRejectInvalidGuess() {
        assertThrows(
                RoomProtocol.ProtocolException.class,
                () -> CooperativeProtocol.parseGuess(
                        "CAT"
                )
        );
    }

    @Test
    void shouldEncodeStartedRound() {
        CooperativeRound round =
                createRound();

        String content =
                CooperativeProtocol.encodeRoundStarted(
                        round
                );

        assertTrue(
                content.startsWith(
                        "COOPERATIVE|ACTIVE|6|"
                )
        );
    }

    @Test
    void shouldEncodeSharedGuessAndProgress() {
        CooperativeRound round =
                createRound();

        CooperativeRound.GuessOutcome outcome =
                round.submitGuess(
                        "player-one",
                        "RAINY"
                );

        String result =
                CooperativeProtocol.encodeGuessResult(
                        outcome
                );

        assertTrue(
                result.startsWith(
                        "COOPERATIVE|RAINY|"
                )
        );

        assertTrue(
                result.contains(
                        "|1|1|5|player-one|Hedi|false|0"
                )
        );

        assertEquals(
                "COOPERATIVE|ACTIVE|1|5|",
                CooperativeProtocol
                        .encodeRoundProgress(round)
        );
    }

    @Test
    void shouldEncodeTeamVictory() {
        CooperativeRound round =
                createRound();

        round.submitGuess(
                "player-two",
                "TRAIN"
        );

        assertEquals(
                "COOPERATIVE|TEAM_WIN|player-two|TRAIN|1",
                CooperativeProtocol.encodeGameOver(
                        round
                )
        );
    }

    @Test
    void shouldRejectEarlyGameOverEncoding() {
        CooperativeRound round =
                createRound();

        assertThrows(
                IllegalStateException.class,
                () -> CooperativeProtocol
                        .encodeGameOver(round)
        );
    }

    private CooperativeRound createRound() {
        return new CooperativeRound(
                "TRAIN",
                List.of(
                        new GamePlayer(
                                "player-one",
                                "Hedi"
                        ),
                        new GamePlayer(
                                "player-two",
                                "Youssef"
                        )
                )
        );
    }
}