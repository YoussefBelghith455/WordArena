package com.wordarena.server.tcp;

import com.wordarena.common.GameMode;
import com.wordarena.common.PlayerView;
import com.wordarena.common.RoomInfo;
import com.wordarena.server.game.GameRoom;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class RoomProtocol {

    public static final String ROOM_STATE = "ROOM_STATE";
    public static final String ROOM_PLAYERS = "ROOM_PLAYERS";
    public static final String ROUND_STATE = "ROUND_STATE";
    public static final String ERROR = "ERROR";

    private static final String FIELD_SEPARATOR = "|";
    private static final String PLAYER_SEPARATOR = ";";
    private static final String PLAYER_FIELD_SEPARATOR = "~";

    private static final int CREATE_ROOM_FIELD_COUNT = 3;
    private static final int MIN_PLAYERS = 2;
    private static final int MAX_PLAYERS = 4;
    private static final int MAX_ROOM_NAME_LENGTH = 30;

    private RoomProtocol() {
    }

    public static CreateRoomRequest parseCreateRoom(
            String content
    ) {
        String[] fields = splitExact(
                content,
                FIELD_SEPARATOR,
                CREATE_ROOM_FIELD_COUNT,
                ErrorCode.INVALID_REQUEST_FORMAT,
                "CREATE_ROOM requires "
                        + "roomName|gameMode|maxPlayers."
        );

        String roomName = validateRoomName(fields[0]);
        GameMode gameMode = parseGameMode(fields[1]);
        int maxPlayers = parseMaximumPlayers(fields[2]);

        validateModeCapacity(gameMode, maxPlayers);

        return new CreateRoomRequest(
                roomName,
                gameMode,
                maxPlayers
        );
    }

    public static boolean parseReadyState(String content) {
        if (content == null) {
            throw protocolError(
                    ErrorCode.INVALID_REQUEST_FORMAT,
                    "SET_READY requires true or false."
            );
        }

        String normalizedContent =
                content.trim().toLowerCase(Locale.ROOT);

        if ("true".equals(normalizedContent)) {
            return true;
        }

        if ("false".equals(normalizedContent)) {
            return false;
        }

        throw protocolError(
                ErrorCode.INVALID_REQUEST_FORMAT,
                "SET_READY requires true or false."
        );
    }

    public static String encodeRoomState(GameRoom room) {
        Objects.requireNonNull(
                room,
                "Game room is required."
        );

        return encodeRoomState(
                room.toRoomInfo(),
                room.getHostPlayerId()
        );
    }

    public static String encodeRoomState(
            RoomInfo roomInfo,
            String hostPlayerId
    ) {
        Objects.requireNonNull(
                roomInfo,
                "Room information is required."
        );

        String validatedHostPlayerId = requireFieldText(
                hostPlayerId,
                "Host player ID"
        );

        return String.join(
                FIELD_SEPARATOR,
                validateProtocolField(
                        roomInfo.getRoomName(),
                        "Room name"
                ),
                roomInfo.getGameMode().name(),
                Integer.toString(
                        roomInfo.getCurrentPlayers()
                ),
                Integer.toString(
                        roomInfo.getMaxPlayers()
                ),
                Boolean.toString(
                        roomInfo.isStarted()
                ),
                validateProtocolField(
                        validatedHostPlayerId,
                        "Host player ID"
                )
        );
    }

    public static String encodeRoomPlayers(
            List<PlayerView> players
    ) {
        Objects.requireNonNull(
                players,
                "Player list is required."
        );

        return players.stream()
                .map(RoomProtocol::encodePlayer)
                .reduce(
                        (firstPlayer, nextPlayer) ->
                                firstPlayer
                                        + PLAYER_SEPARATOR
                                        + nextPlayer
                )
                .orElse("");
    }

    public static String encodeError(
            ErrorCode errorCode,
            String readableMessage
    ) {
        Objects.requireNonNull(
                errorCode,
                "Error code is required."
        );

        String validatedMessage = requireFieldText(
                readableMessage,
                "Error message"
        );

        return errorCode.name()
                + FIELD_SEPARATOR
                + validatedMessage;
    }

    public static ProtocolException protocolError(
            ErrorCode errorCode,
            String readableMessage
    ) {
        return new ProtocolException(
                errorCode,
                readableMessage
        );
    }

    private static String encodePlayer(PlayerView player) {
        Objects.requireNonNull(
                player,
                "Player information is required."
        );

        return String.join(
                PLAYER_FIELD_SEPARATOR,
                validateProtocolField(
                        player.getPlayerId(),
                        "Player ID"
                ),
                validateProtocolField(
                        player.getUsername(),
                        "Username"
                ),
                Boolean.toString(player.isReady()),
                Integer.toString(player.getAttemptsUsed()),
                Integer.toString(player.getScore()),
                Boolean.toString(player.isSolved())
        );
    }

    private static String validateRoomName(String roomName) {
        String validatedRoomName = requireFieldText(
                roomName,
                "Room name"
        );

        if (validatedRoomName.length()
                > MAX_ROOM_NAME_LENGTH) {

            throw protocolError(
                    ErrorCode.INVALID_ROOM_NAME,
                    "Room name must contain at most "
                            + MAX_ROOM_NAME_LENGTH
                            + " characters."
            );
        }

        validateProtocolField(
                validatedRoomName,
                "Room name"
        );

        return validatedRoomName;
    }

    private static GameMode parseGameMode(
            String gameModeValue
    ) {
        String validatedGameMode = requireFieldText(
                gameModeValue,
                "Game mode"
        );

        try {
            return GameMode.valueOf(
                    validatedGameMode.toUpperCase(
                            Locale.ROOT
                    )
            );
        } catch (IllegalArgumentException exception) {
            throw protocolError(
                    ErrorCode.INVALID_GAME_MODE,
                    "Game mode must be DUEL, COOPERATIVE, "
                            + "HOST, or DOUBLE_HOST."
            );
        }
    }

    private static int parseMaximumPlayers(
            String maxPlayersValue
    ) {
        String validatedValue = requireFieldText(
                maxPlayersValue,
                "Maximum players"
        );

        final int maxPlayers;

        try {
            maxPlayers = Integer.parseInt(
                    validatedValue
            );
        } catch (NumberFormatException exception) {
            throw protocolError(
                    ErrorCode.INVALID_MAX_PLAYERS,
                    "Maximum players must be a number."
            );
        }

        if (maxPlayers < MIN_PLAYERS
                || maxPlayers > MAX_PLAYERS) {

            throw protocolError(
                    ErrorCode.INVALID_MAX_PLAYERS,
                    "Maximum players must be between "
                            + MIN_PLAYERS
                            + " and "
                            + MAX_PLAYERS
                            + "."
            );
        }

        return maxPlayers;
    }

    private static void validateModeCapacity(
            GameMode gameMode,
            int maxPlayers
    ) {
        if (gameMode == GameMode.DUEL
                && maxPlayers != 2) {

            throw protocolError(
                    ErrorCode.INVALID_MAX_PLAYERS,
                    "DUEL mode requires exactly 2 players."
            );
        }

        if (gameMode == GameMode.DOUBLE_HOST
                && maxPlayers != 2) {

            throw protocolError(
                    ErrorCode.INVALID_MAX_PLAYERS,
                    "DOUBLE_HOST mode requires exactly 2 players."
            );
        }
    }

    private static String[] splitExact(
            String content,
            String separator,
            int expectedFieldCount,
            ErrorCode errorCode,
            String errorMessage
    ) {
        if (content == null || content.isBlank()) {
            throw protocolError(
                    errorCode,
                    errorMessage
            );
        }

        String[] fields = content.split(
                "\\" + separator,
                -1
        );

        if (fields.length != expectedFieldCount) {
            throw protocolError(
                    errorCode,
                    errorMessage
            );
        }

        return fields;
    }

    private static String validateProtocolField(
            String value,
            String fieldName
    ) {
        String validatedValue = requireFieldText(
                value,
                fieldName
        );

        if (validatedValue.contains(FIELD_SEPARATOR)
                || validatedValue.contains(
                PLAYER_SEPARATOR
        )
                || validatedValue.contains(
                PLAYER_FIELD_SEPARATOR
        )) {

            throw protocolError(
                    ErrorCode.INVALID_REQUEST_FORMAT,
                    fieldName
                            + " cannot contain |, ;, or ~."
            );
        }

        return validatedValue;
    }

    private static String requireFieldText(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw protocolError(
                    ErrorCode.INVALID_REQUEST_FORMAT,
                    fieldName + " is required."
            );
        }

        return value.trim();
    }

    public enum ErrorCode {
        UNAUTHENTICATED,
        IDENTITY_MISMATCH,
        INVALID_REQUEST_FORMAT,
        INVALID_ROOM_NAME,
        INVALID_GAME_MODE,
        INVALID_MAX_PLAYERS,
        ROOM_NOT_FOUND,
        ROOM_FULL,
        ALREADY_IN_ROOM,
        NOT_IN_ROOM,
        NOT_HOST,
        PLAYERS_NOT_READY,
        ROOM_ALREADY_STARTED,
        ROOM_NOT_READY,
        INTERNAL_ERROR
    }

    public record CreateRoomRequest(
            String roomName,
            GameMode gameMode,
            int maxPlayers
    ) {
    }

    public static final class ProtocolException
            extends IllegalArgumentException {

        private final ErrorCode errorCode;

        public ProtocolException(
                ErrorCode errorCode,
                String readableMessage
        ) {
            super(readableMessage);

            this.errorCode = Objects.requireNonNull(
                    errorCode,
                    "Error code is required."
            );
        }

        public ErrorCode getErrorCode() {
            return errorCode;
        }

        public String toProtocolContent() {
            return encodeError(
                    errorCode,
                    getMessage()
            );
        }
    }
}