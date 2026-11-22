package com.wordarena.server.game;

import com.wordarena.common.GameMode;
import com.wordarena.common.PlayerView;
import com.wordarena.common.RoomInfo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class GameRoom {

    private static final int MIN_PLAYERS = 2;
    private static final int MAX_ALLOWED_PLAYERS = 4;

    private final String roomId;
    private final String roomName;
    private final GameMode gameMode;
    private final int maxPlayers;
    private final String hostPlayerId;
    private final Map<String, GamePlayer> players;

    private boolean started;
    private DuelRound duelRound;

    public GameRoom(
            String roomId,
            String roomName,
            GameMode gameMode,
            int maxPlayers,
            GamePlayer host
    ) {
        this.roomId = requireText(
                roomId,
                "Room ID"
        );

        this.roomName = requireText(
                roomName,
                "Room name"
        );

        this.gameMode = Objects.requireNonNull(
                gameMode,
                "Game mode is required."
        );

        validateMaximumPlayers(maxPlayers);

        GamePlayer validatedHost = Objects.requireNonNull(
                host,
                "Host player is required."
        );

        this.maxPlayers = maxPlayers;
        this.hostPlayerId = validatedHost.getPlayerId();
        this.players = new LinkedHashMap<>();

        players.put(
                validatedHost.getPlayerId(),
                validatedHost
        );
    }

    public synchronized void addPlayer(GamePlayer player) {
        Objects.requireNonNull(
                player,
                "Player is required."
        );

        if (started) {
            throw new IllegalStateException(
                    "Cannot join a room after the game has started."
            );
        }

        if (players.size() >= maxPlayers) {
            throw new IllegalStateException(
                    "The room is full."
            );
        }

        if (players.containsKey(player.getPlayerId())) {
            throw new IllegalStateException(
                    "The player is already in this room."
            );
        }

        boolean usernameAlreadyUsed = players.values()
                .stream()
                .anyMatch(existingPlayer ->
                        existingPlayer.getUsername()
                                .equalsIgnoreCase(
                                        player.getUsername()
                                )
                );

        if (usernameAlreadyUsed) {
            throw new IllegalStateException(
                    "A player with this username is already "
                            + "in the room."
            );
        }

        players.put(
                player.getPlayerId(),
                player
        );
    }

    public synchronized GamePlayer removePlayer(
            String playerId
    ) {
        validatePlayerId(playerId);

        if (hostPlayerId.equals(playerId)) {
            throw new IllegalStateException(
                    "The host cannot leave while the room exists."
            );
        }

        GamePlayer removedPlayer = players.remove(playerId);

        if (removedPlayer == null) {
            throw new IllegalStateException(
                    "The player is not in this room."
            );
        }

        return removedPlayer;
    }

    public synchronized void setPlayerReady(
            String playerId,
            boolean ready
    ) {
        if (started) {
            throw new IllegalStateException(
                    "Ready states cannot change after the game starts."
            );
        }

        requirePlayer(playerId).setReady(ready);
    }

    public synchronized void start(
            String requestingPlayerId
    ) {
        validateStartConditions(requestingPlayerId);
        started = true;
    }

    public synchronized DuelRound startDuel(
            String requestingPlayerId,
            String secretWord
    ) {
        if (gameMode != GameMode.DUEL) {
            throw new IllegalStateException(
                    "This room is not configured for DUEL mode."
            );
        }

        validateStartConditions(requestingPlayerId);

        duelRound = new DuelRound(
                secretWord,
                List.copyOf(players.values())
        );

        started = true;

        return duelRound;
    }

    public synchronized DuelRound.GuessOutcome submitDuelGuess(
            String playerId,
            String guessedWord
    ) {
        if (!started || duelRound == null) {
            throw new IllegalStateException(
                    "The Duel round has not started."
            );
        }

        return duelRound.submitGuess(
                playerId,
                guessedWord
        );
    }

    public synchronized Optional<DuelRound> getDuelRound() {
        return Optional.ofNullable(duelRound);
    }

    public synchronized Optional<GamePlayer> findPlayer(
            String playerId
    ) {
        if (playerId == null || playerId.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(players.get(playerId));
    }

    public synchronized GamePlayer requirePlayer(
            String playerId
    ) {
        validatePlayerId(playerId);

        GamePlayer player = players.get(playerId);

        if (player == null) {
            throw new IllegalStateException(
                    "The player is not in this room."
            );
        }

        return player;
    }

    public synchronized boolean containsPlayer(
            String playerId
    ) {
        return playerId != null
                && players.containsKey(playerId);
    }

    public synchronized boolean isHost(String playerId) {
        return hostPlayerId.equals(playerId);
    }

    public synchronized List<PlayerView> getPlayerViews() {
        return players.values()
                .stream()
                .map(GamePlayer::toPlayerView)
                .toList();
    }

    public synchronized RoomInfo toRoomInfo() {
        return new RoomInfo(
                roomId,
                roomName,
                gameMode,
                players.size(),
                maxPlayers,
                started
        );
    }

    public String getRoomId() {
        return roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public String getHostPlayerId() {
        return hostPlayerId;
    }

    public synchronized int getCurrentPlayerCount() {
        return players.size();
    }

    public synchronized boolean isStarted() {
        return started;
    }

    private void validateStartConditions(
            String requestingPlayerId
    ) {
        validatePlayerId(requestingPlayerId);

        if (!hostPlayerId.equals(requestingPlayerId)) {
            throw new IllegalStateException(
                    "Only the room host can start the game."
            );
        }

        if (started) {
            throw new IllegalStateException(
                    "The game has already started."
            );
        }

        if (players.size() < MIN_PLAYERS) {
            throw new IllegalStateException(
                    "At least two players are required."
            );
        }

        boolean everyPlayerReady = players.values()
                .stream()
                .allMatch(GamePlayer::isReady);

        if (!everyPlayerReady) {
            throw new IllegalStateException(
                    "Every player must be ready before starting."
            );
        }
    }

    private static void validateMaximumPlayers(
            int maxPlayers
    ) {
        if (maxPlayers < MIN_PLAYERS
                || maxPlayers > MAX_ALLOWED_PLAYERS) {

            throw new IllegalArgumentException(
                    "Maximum players must be between "
                            + MIN_PLAYERS
                            + " and "
                            + MAX_ALLOWED_PLAYERS
                            + "."
            );
        }
    }

    private static void validatePlayerId(String playerId) {
        requireText(playerId, "Player ID");
    }

    private static String requireText(
            String value,
            String fieldName
    ) {
        Objects.requireNonNull(
                value,
                fieldName + " is required."
        );

        String trimmedValue = value.trim();

        if (trimmedValue.isEmpty()) {
            throw new IllegalArgumentException(
                    fieldName + " is required."
            );
        }

        return trimmedValue;
    }
}