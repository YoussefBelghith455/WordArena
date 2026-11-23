package com.wordarena.server.tcp;

import com.wordarena.common.GameMessage;
import com.wordarena.common.PlayerView;
import com.wordarena.server.game.DuelGameService;
import com.wordarena.server.game.DuelRound;
import com.wordarena.server.game.GamePlayer;
import com.wordarena.server.game.GameRoom;
import com.wordarena.server.game.RoomManager;
import com.wordarena.server.jms.GameEventNotifier;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler implements Runnable {

    private static final String LOGIN = "LOGIN";
    private static final String CREATE_ROOM = "CREATE_ROOM";
    private static final String JOIN_ROOM = "JOIN_ROOM";
    private static final String LEAVE_ROOM = "LEAVE_ROOM";
    private static final String SET_READY = "SET_READY";
    private static final String START_ROUND = "START_ROUND";
    private static final String GUESS = "GUESS";

    private static final String OK = "OK";
    private static final String ERROR = "ERROR";

    private final Socket clientSocket;
    private final PlayerSessionRegistry sessionRegistry;
    private final RoomManager roomManager;
    private final ClientConnectionRegistry connectionRegistry;
    private final DuelGameService duelGameService;
    private final GameEventNotifier gameEventNotifier;

    private ObjectOutputStream output;
    private ObjectInputStream input;
    private String authenticatedPlayerId;

    public ClientHandler(
            Socket clientSocket,
            PlayerSessionRegistry sessionRegistry,
            RoomManager roomManager,
            ClientConnectionRegistry connectionRegistry,
            DuelGameService duelGameService,
            GameEventNotifier gameEventNotifier
    ) {
        this.clientSocket = clientSocket;
        this.sessionRegistry = sessionRegistry;
        this.roomManager = roomManager;
        this.connectionRegistry = connectionRegistry;
        this.duelGameService = duelGameService;
        this.gameEventNotifier = gameEventNotifier;
    }

    @Override
    public void run() {
        try (
                clientSocket;
                ObjectOutputStream socketOutput =
                        new ObjectOutputStream(
                                clientSocket.getOutputStream()
                        )
        ) {
            output = socketOutput;
            output.flush();

            try (
                    ObjectInputStream socketInput =
                            new ObjectInputStream(
                                    clientSocket.getInputStream()
                            )
            ) {
                input = socketInput;

                System.out.println(
                        "Handling client on thread: "
                                + Thread.currentThread().getName()
                );

                processMessages();
            }
        } catch (EOFException exception) {
            System.out.println(
                    "Client closed the connection: "
                            + clientSocket.getRemoteSocketAddress()
            );
        } catch (IOException exception) {
            System.err.println(
                    "Client connection error: "
                            + exception.getMessage()
            );
        } finally {
            handleDisconnection();
        }
    }

    private void processMessages() throws IOException {
        while (!clientSocket.isClosed()) {
            try {
                Object receivedObject = input.readObject();

                if (!(receivedObject
                        instanceof GameMessage message)) {

                    sendProtocolError(
                            RoomProtocol.ErrorCode
                                    .INVALID_REQUEST_FORMAT,
                            "The server accepts only "
                                    + "GameMessage objects."
                    );

                    continue;
                }

                processMessage(message);
            } catch (ClassNotFoundException exception) {
                sendProtocolError(
                        RoomProtocol.ErrorCode
                                .INVALID_REQUEST_FORMAT,
                        "The received object type "
                                + "is not supported."
                );
            }
        }
    }

    private void processMessage(GameMessage message)
            throws IOException {

        if (message.getType() == null
                || message.getType().isBlank()) {

            sendProtocolError(
                    RoomProtocol.ErrorCode
                            .INVALID_REQUEST_FORMAT,
                    "Message type is required."
            );

            return;
        }

        try {
            switch (message.getType()) {
                case LOGIN ->
                        processLogin(message);

                case CREATE_ROOM -> {
                    requireAuthenticatedRequest(message);
                    processCreateRoom(message);
                }

                case JOIN_ROOM -> {
                    requireAuthenticatedRequest(message);
                    processJoinRoom(message);
                }

                case LEAVE_ROOM -> {
                    requireAuthenticatedRequest(message);
                    processLeaveRoom(message);
                }

                case SET_READY -> {
                    requireAuthenticatedRequest(message);
                    processSetReady(message);
                }

                case START_ROUND -> {
                    requireAuthenticatedRequest(message);
                    processStartRound(message);
                }

                case GUESS -> {
                    requireAuthenticatedRequest(message);
                    processGuess(message);
                }

                default -> sendProtocolError(
                        RoomProtocol.ErrorCode
                                .INVALID_REQUEST_FORMAT,
                        "Unsupported message type: "
                                + message.getType()
                );
            }
        } catch (
                RoomProtocol.ProtocolException exception
        ) {
            sendMessage(
                    new GameMessage(
                            ERROR,
                            "",
                            safeRoomId(message.getRoomId()),
                            exception.toProtocolContent()
                    )
            );
        } catch (IllegalStateException exception) {
            sendDomainError(
                    message.getRoomId(),
                    exception
            );
        } catch (IllegalArgumentException exception) {
            sendProtocolError(
                    RoomProtocol.ErrorCode
                            .INVALID_REQUEST_FORMAT,
                    exception.getMessage()
            );
        }
    }

    private void processLogin(GameMessage message)
            throws IOException {

        if (authenticatedPlayerId != null) {
            sendProtocolError(
                    RoomProtocol.ErrorCode
                            .IDENTITY_MISMATCH,
                    "This connection is already authenticated."
            );

            return;
        }

        PlayerSessionRegistry.RegistrationResult result =
                sessionRegistry.register(
                        message.getContent()
                );

        if (!result.successful()) {
            sendMessage(
                    new GameMessage(
                            ERROR,
                            "",
                            "",
                            RoomProtocol.encodeError(
                                    RoomProtocol.ErrorCode
                                            .INVALID_REQUEST_FORMAT,
                                    result.errorMessage()
                            )
                    )
            );

            return;
        }

        PlayerSessionRegistry.PlayerSession session =
                result.session();

        authenticatedPlayerId = session.playerId();

        try {
            connectionRegistry.register(
                    authenticatedPlayerId,
                    this
            );
        } catch (RuntimeException exception) {
            sessionRegistry.remove(
                    authenticatedPlayerId
            );

            authenticatedPlayerId = null;
            throw exception;
        }

        sendMessage(
                new GameMessage(
                        OK,
                        session.playerId(),
                        "",
                        "Login successful. Welcome, "
                                + session.username()
                                + "."
                )
        );

        System.out.println(
                "Player logged in: "
                        + session.username()
                        + " ["
                        + session.playerId()
                        + "]"
        );
    }

    private void processCreateRoom(GameMessage message) {
        RoomProtocol.CreateRoomRequest request =
                RoomProtocol.parseCreateRoom(
                        message.getContent()
                );

        PlayerSessionRegistry.PlayerSession session =
                requireAuthenticatedSession();

        GamePlayer host = new GamePlayer(
                session.playerId(),
                session.username()
        );

        GameRoom room = roomManager.createRoom(
                request.roomName(),
                request.gameMode(),
                request.maxPlayers(),
                host
        );

        broadcastRoomState(room);

        gameEventNotifier.playerJoined(
                room.getRoomId(),
                session.playerId(),
                session.username()
        );
    }

    private void processJoinRoom(GameMessage message) {
        String roomId = requireRoomId(
                message.getRoomId()
        );

        PlayerSessionRegistry.PlayerSession session =
                requireAuthenticatedSession();

        GamePlayer player = new GamePlayer(
                session.playerId(),
                session.username()
        );

        GameRoom room = roomManager.joinRoom(
                roomId,
                player
        );

        broadcastRoomState(room);

        gameEventNotifier.playerJoined(
                room.getRoomId(),
                session.playerId(),
                session.username()
        );
    }

    private void processLeaveRoom(GameMessage message)
            throws IOException {

        String roomId = requireRoomId(
                message.getRoomId()
        );

        PlayerSessionRegistry.PlayerSession session =
                requireAuthenticatedSession();

        GameRoom room = roomManager.requireRoom(roomId);

        boolean hostLeaving =
                room.isHost(authenticatedPlayerId);

        List<String> playerIdsBeforeLeaving =
                extractPlayerIds(room);

        roomManager.leaveRoom(
                roomId,
                authenticatedPlayerId
        );

        sendMessage(
                new GameMessage(
                        OK,
                        authenticatedPlayerId,
                        roomId,
                        "Successfully left the room."
                )
        );

        gameEventNotifier.playerLeft(
                roomId,
                session.playerId(),
                session.username()
        );

        if (hostLeaving) {
            notifyRoomClosed(
                    playerIdsBeforeLeaving,
                    roomId
            );

            return;
        }

        roomManager.findRoom(roomId)
                .ifPresent(this::broadcastRoomState);
    }

    private void processSetReady(GameMessage message) {
        String roomId = requireRoomId(
                message.getRoomId()
        );

        boolean ready = RoomProtocol.parseReadyState(
                message.getContent()
        );

        GameRoom room = roomManager.requireRoom(roomId);

        room.setPlayerReady(
                authenticatedPlayerId,
                ready
        );

        broadcastRoomState(room);
    }

    private void processStartRound(GameMessage message) {
        String roomId = requireRoomId(
                message.getRoomId()
        );

        PlayerSessionRegistry.PlayerSession session =
                requireAuthenticatedSession();

        DuelRound duelRound =
                duelGameService.startRound(
                        roomId,
                        authenticatedPlayerId
                );

        GameRoom room = roomManager.requireRoom(roomId);

        broadcastRoomState(room);

        GameMessage roundStartedMessage =
                new GameMessage(
                        DuelProtocol.ROUND_STATE,
                        "",
                        roomId,
                        DuelProtocol.encodeRoundStarted(
                                duelRound
                        )
                );

        broadcastToRoom(
                room,
                roundStartedMessage
        );

        gameEventNotifier.roundStarted(
                roomId,
                session.playerId(),
                session.username()
        );
    }

    private void processGuess(GameMessage message)
            throws IOException {

        String roomId = requireRoomId(
                message.getRoomId()
        );

        String guessedWord =
                DuelProtocol.parseGuess(
                        message.getContent()
                );

        DuelRound.GuessOutcome outcome =
                duelGameService.submitGuess(
                        roomId,
                        authenticatedPlayerId,
                        guessedWord
                );

        sendMessage(
                new GameMessage(
                        DuelProtocol.GUESS_RESULT,
                        authenticatedPlayerId,
                        roomId,
                        DuelProtocol.encodeGuessResult(
                                outcome
                        )
                )
        );

        GameRoom room = roomManager.requireRoom(roomId);

        DuelRound duelRound =
                duelGameService.requireActiveRound(
                        roomId
                );

        GamePlayer guessingPlayer =
                room.requirePlayer(
                        authenticatedPlayerId
                );

        broadcastRoomState(room);

        broadcastToRoom(
                room,
                new GameMessage(
                        DuelProtocol.ROUND_STATE,
                        "",
                        roomId,
                        DuelProtocol.encodeRoundProgress(
                                duelRound
                        )
                )
        );

        if (outcome.evaluation().solved()) {
            gameEventNotifier.playerSolved(
                    roomId,
                    guessingPlayer.getPlayerId(),
                    guessingPlayer.getUsername()
            );

            gameEventNotifier.scoreUpdated(
                    roomId,
                    guessingPlayer.getPlayerId(),
                    guessingPlayer.getUsername(),
                    guessingPlayer.getScore()
            );
        }

        if (outcome.roundFinished()) {
            broadcastToRoom(
                    room,
                    new GameMessage(
                            DuelProtocol.GAME_OVER,
                            "",
                            roomId,
                            DuelProtocol.encodeGameOver(
                                    duelRound
                            )
                    )
            );

            String winnerPlayerId =
                    duelRound.getWinnerPlayerId()
                            .orElse("");

            String winnerUsername = "";

            if (!winnerPlayerId.isBlank()) {
                winnerUsername =
                        room.requirePlayer(
                                winnerPlayerId
                        ).getUsername();
            }

            gameEventNotifier.roundEnded(
                    roomId,
                    winnerPlayerId,
                    winnerUsername
            );
        }
    }

    private void requireAuthenticatedRequest(
            GameMessage message
    ) {
        if (authenticatedPlayerId == null) {
            throw RoomProtocol.protocolError(
                    RoomProtocol.ErrorCode.UNAUTHENTICATED,
                    "You must log in before using this command."
            );
        }

        String suppliedPlayerId =
                message.getPlayerId();

        if (suppliedPlayerId != null
                && !suppliedPlayerId.isBlank()
                && !authenticatedPlayerId.equals(
                suppliedPlayerId
        )) {

            throw RoomProtocol.protocolError(
                    RoomProtocol.ErrorCode
                            .IDENTITY_MISMATCH,
                    "The supplied player ID does not "
                            + "match this connection."
            );
        }
    }

    private PlayerSessionRegistry.PlayerSession
    requireAuthenticatedSession() {

        if (authenticatedPlayerId == null) {
            throw RoomProtocol.protocolError(
                    RoomProtocol.ErrorCode.UNAUTHENTICATED,
                    "You must log in first."
            );
        }

        return sessionRegistry.findByPlayerId(
                        authenticatedPlayerId
                )
                .orElseThrow(
                        () -> RoomProtocol.protocolError(
                                RoomProtocol.ErrorCode
                                        .UNAUTHENTICATED,
                                "The authenticated session "
                                        + "no longer exists."
                        )
                );
    }

    private String requireRoomId(String roomId) {
        if (roomId == null || roomId.isBlank()) {
            throw RoomProtocol.protocolError(
                    RoomProtocol.ErrorCode
                            .INVALID_REQUEST_FORMAT,
                    "Room ID is required."
            );
        }

        return roomId.trim();
    }

    private void broadcastRoomState(GameRoom room) {
        GameMessage roomStateMessage =
                new GameMessage(
                        RoomProtocol.ROOM_STATE,
                        "",
                        room.getRoomId(),
                        RoomProtocol.encodeRoomState(room)
                );

        GameMessage roomPlayersMessage =
                new GameMessage(
                        RoomProtocol.ROOM_PLAYERS,
                        "",
                        room.getRoomId(),
                        RoomProtocol.encodeRoomPlayers(
                                room.getPlayerViews()
                        )
                );

        broadcastToRoom(room, roomStateMessage);
        broadcastToRoom(room, roomPlayersMessage);
    }

    private void broadcastToRoom(
            GameRoom room,
            GameMessage message
    ) {
        for (PlayerView playerView :
                room.getPlayerViews()) {

            connectionRegistry.sendToPlayer(
                    playerView.getPlayerId(),
                    message
            );
        }
    }

    private void notifyRoomClosed(
            List<String> previousPlayerIds,
            String roomId
    ) {
        GameMessage roomClosedMessage =
                new GameMessage(
                        ERROR,
                        "",
                        roomId,
                        RoomProtocol.encodeError(
                                RoomProtocol.ErrorCode
                                        .ROOM_NOT_FOUND,
                                "The host left and the room "
                                        + "was closed."
                        )
                );

        for (String playerId : previousPlayerIds) {
            if (!playerId.equals(
                    authenticatedPlayerId
            )) {
                connectionRegistry.sendToPlayer(
                        playerId,
                        roomClosedMessage
                );
            }
        }
    }

    private List<String> extractPlayerIds(
            GameRoom room
    ) {
        List<String> playerIds = new ArrayList<>();

        for (PlayerView playerView :
                room.getPlayerViews()) {

            playerIds.add(
                    playerView.getPlayerId()
            );
        }

        return playerIds;
    }

    private void sendDomainError(
            String roomId,
            IllegalStateException exception
    ) throws IOException {

        RoomProtocol.ErrorCode errorCode =
                mapDomainError(exception.getMessage());

        sendMessage(
                new GameMessage(
                        ERROR,
                        "",
                        safeRoomId(roomId),
                        RoomProtocol.encodeError(
                                errorCode,
                                exception.getMessage()
                        )
                )
        );
    }

    private RoomProtocol.ErrorCode mapDomainError(
            String message
    ) {
        if (message == null) {
            return RoomProtocol.ErrorCode.INTERNAL_ERROR;
        }

        return switch (message) {
            case "Room not found." ->
                    RoomProtocol.ErrorCode.ROOM_NOT_FOUND;

            case "The room is full." ->
                    RoomProtocol.ErrorCode.ROOM_FULL;

            case "The player is already in another room.",
                 "The player is already in this room." ->
                    RoomProtocol.ErrorCode.ALREADY_IN_ROOM;

            case "The player is not in this room.",
                 "The player does not belong to this Duel round." ->
                    RoomProtocol.ErrorCode.NOT_IN_ROOM;

            case "Only the room host can start the game.",
                 "Only the host can delete the room." ->
                    RoomProtocol.ErrorCode.NOT_HOST;

            case "Every player must be ready before starting." ->
                    RoomProtocol.ErrorCode.PLAYERS_NOT_READY;

            case "At least two players are required.",
                 "The Duel round has not started.",
                 "This room is not configured for DUEL mode." ->
                    RoomProtocol.ErrorCode.ROOM_NOT_READY;

            case "The game has already started.",
                 "Cannot join a room after the game has started.",
                 "Ready states cannot change after the game starts.",
                 "The Duel round has already finished.",
                 "This player has already solved the word.",
                 "This player has no attempts remaining." ->
                    RoomProtocol.ErrorCode.ROOM_ALREADY_STARTED;

            default ->
                    RoomProtocol.ErrorCode.INTERNAL_ERROR;
        };
    }

    private void sendProtocolError(
            RoomProtocol.ErrorCode errorCode,
            String readableMessage
    ) throws IOException {

        sendMessage(
                new GameMessage(
                        ERROR,
                        "",
                        "",
                        RoomProtocol.encodeError(
                                errorCode,
                                readableMessage
                        )
                )
        );
    }

    public synchronized void sendServerMessage(
            GameMessage message
    ) throws IOException {
        sendMessage(message);
    }

    private synchronized void sendMessage(
            GameMessage message
    ) throws IOException {

        output.writeObject(message);
        output.flush();
        output.reset();
    }

    private String safeRoomId(String roomId) {
        return roomId == null ? "" : roomId;
    }

    private void handleDisconnection() {
        if (authenticatedPlayerId == null) {
            return;
        }

        String disconnectedPlayerId =
                authenticatedPlayerId;

        PlayerSessionRegistry.PlayerSession session =
                sessionRegistry.findByPlayerId(
                        disconnectedPlayerId
                ).orElse(null);

        roomManager.findRoomIdForPlayer(
                        disconnectedPlayerId
                )
                .ifPresent(roomId -> {
                    GameRoom room =
                            roomManager.findRoom(roomId)
                                    .orElse(null);

                    List<String> previousPlayerIds =
                            room == null
                                    ? List.of()
                                    : extractPlayerIds(room);

                    boolean wasHost =
                            room != null
                                    && room.isHost(
                                    disconnectedPlayerId
                            );

                    roomManager.removePlayerAfterDisconnect(
                            disconnectedPlayerId
                    );

                    if (session != null) {
                        gameEventNotifier.playerLeft(
                                roomId,
                                session.playerId(),
                                session.username()
                        );
                    }

                    if (room != null && wasHost) {
                        notifyRoomClosed(
                                previousPlayerIds,
                                roomId
                        );
                    } else {
                        roomManager.findRoom(roomId)
                                .ifPresent(
                                        this::broadcastRoomState
                                );
                    }
                });

        connectionRegistry.remove(
                disconnectedPlayerId,
                this
        );

        boolean removed =
                sessionRegistry.remove(
                        disconnectedPlayerId
                );

        if (removed) {
            System.out.println(
                    "Player session removed: "
                            + disconnectedPlayerId
            );
        }

        authenticatedPlayerId = null;
    }
}