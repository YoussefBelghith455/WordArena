package com.wordarena.server.game;

import com.wordarena.common.GameMode;
import com.wordarena.common.PlayerView;
import com.wordarena.server.rmi.GameStatisticsService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class DoubleHostGameService {

    private final RoomManager roomManager;
    private final GameStatisticsService statisticsService;

    private final Map<String, Map<String, String>>
            lockedWordsByRoomId;

    private final Map<String, DoubleHostRound>
            activeRoundsByRoomId;

    public DoubleHostGameService(
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

        this.lockedWordsByRoomId =
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
                requireDoubleHostRoom(roomId);

        if (!room.containsPlayer(
                requestingPlayerId
        )) {
            throw new IllegalStateException(
                    "The player is not in this room."
            );
        }

        if (room.isStarted()) {
            throw new IllegalStateException(
                    "Secret words cannot be changed "
                            + "after the game starts."
            );
        }

        String normalizedWord =
                WordEvaluator.evaluate(
                        secretWord,
                        secretWord
                ).guessedWord();

        lockedWordsByRoomId.compute(
                room.getRoomId(),
                (ignoredRoomId, existingWords) -> {
                    Map<String, String> updatedWords =
                            existingWords == null
                                    ? new LinkedHashMap<>()
                                    : new LinkedHashMap<>(
                                    existingWords
                            );

                    updatedWords.put(
                            requestingPlayerId,
                            normalizedWord
                    );

                    return updatedWords;
                }
        );
    }

    public boolean hasPlayerLockedWord(
            String roomId,
            String playerId
    ) {
        if (roomId == null
                || roomId.isBlank()
                || playerId == null
                || playerId.isBlank()) {

            return false;
        }

        Map<String, String> lockedWords =
                lockedWordsByRoomId.get(
                        roomId.trim()
                );

        return lockedWords != null
                && lockedWords.containsKey(
                playerId.trim()
        );
    }

    public boolean areBothWordsLocked(
            String roomId
    ) {
        if (roomId == null
                || roomId.isBlank()) {

            return false;
        }

        Map<String, String> lockedWords =
                lockedWordsByRoomId.get(
                        roomId.trim()
                );

        return lockedWords != null
                && lockedWords.size() == 2;
    }

    public DoubleHostRound startRound(
            String roomId,
            String requestingPlayerId
    ) {
        GameRoom room =
                requireDoubleHostRoom(roomId);

        if (!room.isHost(requestingPlayerId)) {
            throw new IllegalStateException(
                    "Only the room host can start the game."
            );
        }

        Map<String, String> lockedWords =
                lockedWordsByRoomId.get(
                        room.getRoomId()
                );

        if (lockedWords == null
                || lockedWords.size() != 2) {

            throw new IllegalStateException(
                    "Both Double Host players must lock "
                            + "a secret word before starting."
            );
        }

        registerRoomPlayers(room);

        room.start(requestingPlayerId);

        List<GamePlayer> players =
                room.getPlayerViews()
                        .stream()
                        .map(player ->
                                room.requirePlayer(
                                        player.getPlayerId()
                                )
                        )
                        .toList();

        DoubleHostRound round =
                new DoubleHostRound(
                        players,
                        Map.copyOf(lockedWords)
                );

        activeRoundsByRoomId.put(
                room.getRoomId(),
                round
        );

        return round;
    }

    public DoubleHostRound.GuessOutcome submitGuess(
            String roomId,
            String playerId,
            String guessedWord
    ) {
        GameRoom room =
                requireDoubleHostRoom(roomId);

        if (!room.containsPlayer(playerId)) {
            throw new IllegalStateException(
                    "The player is not in this room."
            );
        }

        DoubleHostRound round =
                requireActiveRound(roomId);

        DoubleHostRound.GuessOutcome outcome =
                round.submitGuess(
                        playerId,
                        guessedWord
                );

        if (outcome.roundFinished()) {
            statisticsService
                    .recordCompletedDoubleHost(
                            room,
                            round
                    );
        }

        return outcome;
    }

    public DoubleHostRound requireActiveRound(
            String roomId
    ) {
        if (roomId == null || roomId.isBlank()) {
            throw new IllegalArgumentException(
                    "Room ID is required."
            );
        }

        DoubleHostRound round =
                activeRoundsByRoomId.get(
                        roomId.trim()
                );

        if (round == null) {
            throw new IllegalStateException(
                    "The Double Host round has not started."
            );
        }

        return round;
    }

    private GameRoom requireDoubleHostRoom(
            String roomId
    ) {
        GameRoom room =
                roomManager.requireRoom(roomId);

        if (room.getGameMode()
                != GameMode.DOUBLE_HOST) {

            throw new IllegalStateException(
                    "This room is not configured "
                            + "for DOUBLE_HOST mode."
            );
        }

        if (room.getMaxPlayers() != 2) {
            throw new IllegalStateException(
                    "DOUBLE_HOST mode requires exactly 2 players."
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