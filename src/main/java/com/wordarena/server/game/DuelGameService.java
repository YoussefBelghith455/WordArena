package com.wordarena.server.game;

import com.wordarena.common.GameMode;
import com.wordarena.common.PlayerView;
import com.wordarena.server.rmi.GameStatisticsService;

import java.util.Objects;

public class DuelGameService {

    private final RoomManager roomManager;
    private final WordBank wordBank;
    private final GameStatisticsService statisticsService;

    public DuelGameService(
            RoomManager roomManager,
            WordBank wordBank,
            GameStatisticsService statisticsService
    ) {
        this.roomManager = Objects.requireNonNull(
                roomManager,
                "Room manager is required."
        );

        this.wordBank = Objects.requireNonNull(
                wordBank,
                "Word bank is required."
        );

        this.statisticsService = Objects.requireNonNull(
                statisticsService,
                "Statistics service is required."
        );
    }

    public DuelRound startRound(
            String roomId,
            String requestingPlayerId
    ) {
        GameRoom room =
                roomManager.requireRoom(roomId);

        if (room.getGameMode() != GameMode.DUEL) {
            throw new IllegalStateException(
                    "This room is not configured for DUEL mode."
            );
        }

        registerRoomPlayers(room);

        String secretWord =
                wordBank.selectRandomWord();

        DuelRound duelRound =
                room.startDuel(
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
        GameRoom room =
                roomManager.requireRoom(roomId);

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

        DuelRound.GuessOutcome outcome =
                room.submitDuelGuess(
                        playerId,
                        guessedWord
                );

        if (outcome.roundFinished()) {
            DuelRound duelRound =
                    requireActiveRound(roomId);

            statisticsService.recordCompletedDuel(
                    room,
                    duelRound
            );
        }

        return outcome;
    }

    public DuelRound requireActiveRound(
            String roomId
    ) {
        GameRoom room =
                roomManager.requireRoom(roomId);

        return room.getDuelRound()
                .orElseThrow(
                        () -> new IllegalStateException(
                                "The Duel round has not started."
                        )
                );
    }

    private void registerRoomPlayers(
            GameRoom room
    ) {
        for (PlayerView player :
                room.getPlayerViews()) {

            statisticsService.registerPlayer(
                    player.getPlayerId(),
                    player.getUsername()
            );
        }
    }
}