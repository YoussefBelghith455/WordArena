package com.wordarena.server.game;

import com.wordarena.common.GameMode;

import java.util.Objects;

public class DuelGameService {

    private final RoomManager roomManager;
    private final WordBank wordBank;

    public DuelGameService(
            RoomManager roomManager,
            WordBank wordBank
    ) {
        this.roomManager = Objects.requireNonNull(
                roomManager,
                "Room manager is required."
        );

        this.wordBank = Objects.requireNonNull(
                wordBank,
                "Word bank is required."
        );
    }

    public DuelRound startRound(
            String roomId,
            String requestingPlayerId
    ) {
        GameRoom room = roomManager.requireRoom(roomId);

        if (room.getGameMode() != GameMode.DUEL) {
            throw new IllegalStateException(
                    "This room is not configured for DUEL mode."
            );
        }

        String secretWord = wordBank.selectRandomWord();

        DuelRound duelRound = room.startDuel(
                requestingPlayerId,
                secretWord
        );

        System.out.println(
                "Duel round started in room "
                        + room.getRoomId()
                        + "."
        );

        return duelRound;
    }

    public DuelRound.GuessOutcome submitGuess(
            String roomId,
            String playerId,
            String guessedWord
    ) {
        GameRoom room = roomManager.requireRoom(roomId);

        if (room.getGameMode() != GameMode.DUEL) {
            throw new IllegalStateException(
                    "This room is not configured for DUEL mode."
            );
        }

        if (!room.containsPlayer(playerId)) {
            throw new IllegalStateException(
                    "The player is not in this room."
            );
        }

        return room.submitDuelGuess(
                playerId,
                guessedWord
        );
    }

    public DuelRound requireActiveRound(
            String roomId
    ) {
        GameRoom room = roomManager.requireRoom(roomId);

        return room.getDuelRound()
                .orElseThrow(
                        () -> new IllegalStateException(
                                "The Duel round has not started."
                        )
                );
    }
}