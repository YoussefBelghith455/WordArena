package com.wordarena.server.game;

import com.wordarena.common.GameMode;
import com.wordarena.common.RoomInfo;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RoomManager {

    private static final int MAX_ROOM_NAME_LENGTH = 30;

    private final Map<String, GameRoom> roomsById;
    private final Map<String, String> roomIdByPlayerId;

    public RoomManager() {
        this.roomsById = new ConcurrentHashMap<>();
        this.roomIdByPlayerId = new ConcurrentHashMap<>();
    }

    public GameRoom createRoom(
            String roomName,
            GameMode gameMode,
            int maxPlayers,
            GamePlayer host
    ) {
        Objects.requireNonNull(
                host,
                "Host player is required."
        );

        String validatedRoomName =
                validateRoomName(roomName);

        Objects.requireNonNull(
                gameMode,
                "Game mode is required."
        );

        ensurePlayerIsNotAlreadyInRoom(
                host.getPlayerId()
        );

        String roomId = generateRoomId();

        GameRoom room = new GameRoom(
                roomId,
                validatedRoomName,
                gameMode,
                maxPlayers,
                host
        );

        GameRoom existingRoom =
                roomsById.putIfAbsent(roomId, room);

        if (existingRoom != null) {
            throw new IllegalStateException(
                    "Unable to create a unique room."
            );
        }

        String previousRoomId =
                roomIdByPlayerId.putIfAbsent(
                        host.getPlayerId(),
                        roomId
                );

        if (previousRoomId != null) {
            roomsById.remove(roomId, room);

            throw new IllegalStateException(
                    "The player is already in another room."
            );
        }

        return room;
    }

    public GameRoom joinRoom(
            String roomId,
            GamePlayer player
    ) {
        Objects.requireNonNull(
                player,
                "Player is required."
        );

        ensurePlayerIsNotAlreadyInRoom(
                player.getPlayerId()
        );

        GameRoom room = requireRoom(roomId);

        /*
         * The synchronized block ensures that joining the room
         * and registering the player-room relationship happen
         * as one coordinated operation.
         */
        synchronized (room) {
            ensurePlayerIsNotAlreadyInRoom(
                    player.getPlayerId()
            );

            room.addPlayer(player);

            String existingRoomId =
                    roomIdByPlayerId.putIfAbsent(
                            player.getPlayerId(),
                            room.getRoomId()
                    );

            if (existingRoomId != null) {
                room.removePlayer(player.getPlayerId());

                throw new IllegalStateException(
                        "The player is already in another room."
                );
            }
        }

        return room;
    }

    public void leaveRoom(
            String roomId,
            String playerId
    ) {
        GameRoom room = requireRoom(roomId);

        if (room.isHost(playerId)) {
            deleteRoom(roomId, playerId);
            return;
        }

        synchronized (room) {
            room.removePlayer(playerId);

            roomIdByPlayerId.remove(
                    playerId,
                    room.getRoomId()
            );
        }
    }

    public void deleteRoom(
            String roomId,
            String requestingPlayerId
    ) {
        GameRoom room = requireRoom(roomId);

        synchronized (room) {
            if (!room.isHost(requestingPlayerId)) {
                throw new IllegalStateException(
                        "Only the host can delete the room."
                );
            }

            List<String> playerIds = room.getPlayerViews()
                    .stream()
                    .map(playerView ->
                            playerView.getPlayerId()
                    )
                    .toList();

            boolean removed =
                    roomsById.remove(room.getRoomId(), room);

            if (!removed) {
                throw new IllegalStateException(
                        "The room could not be deleted."
                );
            }

            for (String playerId : playerIds) {
                roomIdByPlayerId.remove(
                        playerId,
                        room.getRoomId()
                );
            }
        }
    }

    public void removePlayerAfterDisconnect(
            String playerId
    ) {
        Optional<String> optionalRoomId =
                findRoomIdForPlayer(playerId);

        if (optionalRoomId.isEmpty()) {
            return;
        }

        String roomId = optionalRoomId.get();

        try {
            leaveRoom(roomId, playerId);
        } catch (IllegalStateException exception) {
            roomIdByPlayerId.remove(playerId, roomId);
        }
    }

    public GameRoom requireRoom(String roomId) {
        if (roomId == null || roomId.isBlank()) {
            throw new IllegalArgumentException(
                    "Room ID is required."
            );
        }

        GameRoom room = roomsById.get(roomId.trim());

        if (room == null) {
            throw new IllegalStateException(
                    "Room not found."
            );
        }

        return room;
    }

    public Optional<GameRoom> findRoom(String roomId) {
        if (roomId == null || roomId.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                roomsById.get(roomId.trim())
        );
    }

    public Optional<String> findRoomIdForPlayer(
            String playerId
    ) {
        if (playerId == null || playerId.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                roomIdByPlayerId.get(playerId)
        );
    }

    public List<RoomInfo> getAvailableRooms() {
        return roomsById.values()
                .stream()
                .filter(room -> !room.isStarted())
                .map(GameRoom::toRoomInfo)
                .sorted(
                        Comparator.comparing(
                                RoomInfo::getRoomName,
                                String.CASE_INSENSITIVE_ORDER
                        )
                )
                .toList();
    }

    public int getRoomCount() {
        return roomsById.size();
    }

    private void ensurePlayerIsNotAlreadyInRoom(
            String playerId
    ) {
        if (playerId == null || playerId.isBlank()) {
            throw new IllegalArgumentException(
                    "Player ID is required."
            );
        }

        if (roomIdByPlayerId.containsKey(playerId)) {
            throw new IllegalStateException(
                    "The player is already in another room."
            );
        }
    }

    private String validateRoomName(String roomName) {
        if (roomName == null || roomName.isBlank()) {
            throw new IllegalArgumentException(
                    "Room name is required."
            );
        }

        String trimmedRoomName = roomName.trim();

        if (trimmedRoomName.length()
                > MAX_ROOM_NAME_LENGTH) {

            throw new IllegalArgumentException(
                    "Room name must contain at most "
                            + MAX_ROOM_NAME_LENGTH
                            + " characters."
            );
        }

        return trimmedRoomName;
    }

    private String generateRoomId() {
        return UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();
    }
}