package com.wordarena.server.game;

import com.wordarena.common.GameMode;
import com.wordarena.common.PlayerView;
import com.wordarena.server.rmi.GameStatisticsService;

import java.util.Objects;

public class CooperativeGameService {

    private final RoomManager roomManager;
    private final WordBank wordBank;
    private final GameStatisticsService statisticsService;

    public CooperativeGameService(
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

    public CooperativeRound startRound(
            String roomId,
            String requestingPlayerId
    ) {
        GameRoom room =
                roomManager.requireRoom(roomId);

        if (room.getGameMode()
                != GameMode.COOPERATIVE) {

            throw new IllegalStateException(
                    "This room is not configured "
                            + "for COOPERATIVE mode."
            );
        }

        registerRoomPlayers(room);

        CooperativeRound cooperativeRound =
                room.startCooperative(
                        requestingPlayerId,
                        wordBank.selectRandomWord()
                );

        System.out.println(
                "Cooperative round started in room "
                        + room.getRoomId()
                        + "."
        );

        return cooperativeRound;
    }

    public CooperativeRound.GuessOutcome submitGuess(
            String roomId,
            String playerId,
            String guessedWord
    ) {
        GameRoom room =
                roomManager.requireRoom(roomId);

        if (room.getGameMode()
                != GameMode.COOPERATIVE) {

            throw new IllegalStateException(
                    "This room is not configured "
                            + "for COOPERATIVE mode."
            );
        }

        if (!room.containsPlayer(playerId)) {
            throw new IllegalStateException(
                    "The player is not in this room."
            );
        }

        CooperativeRound.GuessOutcome outcome =
                room.submitCooperativeGuess(
                        playerId,
                        guessedWord
                );

        if (outcome.roundFinished()) {
            statisticsService.recordCompletedCooperative(
                    room,
                    requireActiveRound(roomId)
            );
        }

        return outcome;
    }

    public CooperativeRound requireActiveRound(
            String roomId
    ) {
        return roomManager.requireRoom(roomId)
                .getCooperativeRound()
                .orElseThrow(
                        () -> new IllegalStateException(
                                "The Cooperative round "
                                        + "has not started."
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