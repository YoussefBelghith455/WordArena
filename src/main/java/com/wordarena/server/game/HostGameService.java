package com.wordarena.server.game;

import com.wordarena.common.GameMode;
import com.wordarena.common.PlayerView;
import com.wordarena.server.rmi.GameStatisticsService;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class HostGameService {

    private final RoomManager roomManager;
    private final GameStatisticsService statisticsService;

    private final Map<String, String>
            lockedSecretWordsByRoomId;

    private final Map<String, HostRound>
            activeRoundsByRoomId;

    public HostGameService(
            RoomManager roomManager,
            GameStatisticsService statisticsService
    ) {
        this.roomManager = Objects.requireNonNull(
                roomManager,
                "Room manager is required."
        );

        this.statisticsService =
                Objects.requireNonNull(
                        statisticsService,
                        "Statistics service is required."
                );

        this.lockedSecretWordsByRoomId =
                new ConcurrentHashMap<>();

        this.activeRoundsByRoomId =
                new ConcurrentHashMap<>();
    }

    public void lockSecretWord(
            String roomId,
            String requestingPlayerId,
            String secretWord
    ) {
        GameRoom room =
                requireHostRoom(roomId);

        if (!room.isHost(requestingPlayerId)) {
            throw new IllegalStateException(
                    "Only the room host can lock "
                            + "the secret word."
            );
        }

        if (room.isStarted()) {
            throw new IllegalStateException(
                    "The secret word cannot be changed "
                            + "after the game starts."
            );
        }

        String normalizedWord =
                WordEvaluator.evaluate(
                        secretWord,
                        secretWord
                ).guessedWord();

        lockedSecretWordsByRoomId.put(
                room.getRoomId(),
                normalizedWord
        );
    }

    public boolean isSecretWordLocked(
            String roomId
    ) {
        return roomId != null
                && lockedSecretWordsByRoomId
                .containsKey(roomId.trim());
    }

    public HostRound startRound(
            String roomId,
            String requestingPlayerId
    ) {
        GameRoom room =
                requireHostRoom(roomId);

        if (!room.isHost(requestingPlayerId)) {
            throw new IllegalStateException(
                    "Only the room host can start the game."
            );
        }

        String secretWord =
                lockedSecretWordsByRoomId.get(
                        room.getRoomId()
                );

        if (secretWord == null) {
            throw new IllegalStateException(
                    "The host must lock a secret word "
                            + "before starting."
            );
        }

        registerRoomPlayers(room);

        room.start(requestingPlayerId);

        GamePlayer host =
                room.requirePlayer(
                        room.getHostPlayerId()
                );

        List<GamePlayer> players =
                room.getPlayerViews()
                        .stream()
                        .map(player ->
                                room.requirePlayer(
                                        player.getPlayerId()
                                )
                        )
                        .toList();

        HostRound hostRound =
                new HostRound(
                        secretWord,
                        host,
                        players
                );

        activeRoundsByRoomId.put(
                room.getRoomId(),
                hostRound
        );

        return hostRound;
    }

    public HostRound.GuessOutcome submitGuess(
            String roomId,
            String playerId,
            String guessedWord
    ) {
        GameRoom room =
                requireHostRoom(roomId);

        if (!room.containsPlayer(playerId)) {
            throw new IllegalStateException(
                    "The player is not in this room."
            );
        }

        HostRound hostRound =
                requireActiveRound(roomId);

        HostRound.GuessOutcome outcome =
                hostRound.submitGuess(
                        playerId,
                        guessedWord
                );

        if (outcome.roundFinished()) {
            statisticsService.recordCompletedHost(
                    room,
                    hostRound
            );
        }

        return outcome;
    }

    public HostRound requireActiveRound(
            String roomId
    ) {
        if (roomId == null || roomId.isBlank()) {
            throw new IllegalArgumentException(
                    "Room ID is required."
            );
        }

        HostRound hostRound =
                activeRoundsByRoomId.get(
                        roomId.trim()
                );

        if (hostRound == null) {
            throw new IllegalStateException(
                    "The Host round has not started."
            );
        }

        return hostRound;
    }

    private GameRoom requireHostRoom(
            String roomId
    ) {
        GameRoom room =
                roomManager.requireRoom(roomId);

        if (room.getGameMode() != GameMode.HOST) {
            throw new IllegalStateException(
                    "This room is not configured "
                            + "for HOST mode."
            );
        }

        return room;
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