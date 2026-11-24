package com.wordarena.server.tcp;

import com.wordarena.common.GameMessage;
import com.wordarena.common.GameMode;
import com.wordarena.common.PlayerView;
import com.wordarena.server.game.CooperativeGameService;
import com.wordarena.server.game.CooperativeRound;
import com.wordarena.server.game.DoubleHostGameService;
import com.wordarena.server.game.DoubleHostRound;
import com.wordarena.server.game.DuelGameService;
import com.wordarena.server.game.DuelRound;
import com.wordarena.server.game.GamePlayer;
import com.wordarena.server.game.GameRoom;
import com.wordarena.server.game.HostGameService;
import com.wordarena.server.game.HostRound;
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
    private static final String LOCK_WORD = "LOCK_WORD";
    private static final String START_ROUND = "START_ROUND";
    private static final String GUESS = "GUESS";

    private static final String OK = "OK";
    private static final String ERROR = "ERROR";

    private final Socket clientSocket;
    private final PlayerSessionRegistry sessionRegistry;
    private final RoomManager roomManager;
    private final ClientConnectionRegistry connectionRegistry;

    private final DuelGameService duelGameService;
    private final CooperativeGameService cooperativeGameService;
    private final HostGameService hostGameService;
    private final DoubleHostGameService doubleHostGameService;

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
            CooperativeGameService cooperativeGameService,
            HostGameService hostGameService,
            DoubleHostGameService doubleHostGameService,
            GameEventNotifier gameEventNotifier
    ) {
        this.clientSocket = clientSocket;
        this.sessionRegistry = sessionRegistry;
        this.roomManager = roomManager;
        this.connectionRegistry = connectionRegistry;
        this.duelGameService = duelGameService;
        this.cooperativeGameService =
                cooperativeGameService;
        this.hostGameService = hostGameService;
        this.doubleHostGameService =
                doubleHostGameService;
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

    private void processMessages()
            throws IOException {

        while (!clientSocket.isClosed()) {
            try {
                Object object =
                        input.readObject();

                if (!(object
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

    private void processMessage(
            GameMessage message
    ) throws IOException {

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

                case LOCK_WORD -> {
                    requireAuthenticatedRequest(message);
                    processLockWord(message);
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

    private void processLogin(
            GameMessage message
    ) throws IOException {

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

        authenticatedPlayerId =
                session.playerId();

        connectionRegistry.register(
                authenticatedPlayerId,
                this
        );

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
    }

    private void processCreateRoom(
            GameMessage message
    ) {
        RoomProtocol.CreateRoomRequest request =
                RoomProtocol.parseCreateRoom(
                        message.getContent()
                );

        PlayerSessionRegistry.PlayerSession session =
                requireAuthenticatedSession();

        GameRoom room =
                roomManager.createRoom(
                        request.roomName(),
                        request.gameMode(),
                        request.maxPlayers(),
                        new GamePlayer(
                                session.playerId(),
                                session.username()
                        )
                );

        broadcastRoomState(room);

        gameEventNotifier.playerJoined(
                room.getRoomId(),
                session.playerId(),
                session.username()
        );
    }

    private void processJoinRoom(
            GameMessage message
    ) {
        String roomId =
                requireRoomId(
                        message.getRoomId()
                );

        PlayerSessionRegistry.PlayerSession session =
                requireAuthenticatedSession();

        GameRoom room =
                roomManager.joinRoom(
                        roomId,
                        new GamePlayer(
                                session.playerId(),
                                session.username()
                        )
                );

        broadcastRoomState(room);

        gameEventNotifier.playerJoined(
                roomId,
                session.playerId(),
                session.username()
        );
    }

    private void processLeaveRoom(
            GameMessage message
    ) throws IOException {

        String roomId =
                requireRoomId(
                        message.getRoomId()
                );

        PlayerSessionRegistry.PlayerSession session =
                requireAuthenticatedSession();

        GameRoom room =
                roomManager.requireRoom(roomId);

        boolean hostLeaving =
                room.isHost(
                        authenticatedPlayerId
                );

        List<String> previousIds =
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
                    previousIds,
                    roomId
            );
        } else {
            roomManager.findRoom(roomId)
                    .ifPresent(
                            this::broadcastRoomState
                    );
        }
    }

    private void processSetReady(
            GameMessage message
    ) {
        String roomId =
                requireRoomId(
                        message.getRoomId()
                );

        GameRoom room =
                roomManager.requireRoom(roomId);

        room.setPlayerReady(
                authenticatedPlayerId,
                RoomProtocol.parseReadyState(
                        message.getContent()
                )
        );

        broadcastRoomState(room);
    }

    private void processLockWord(
            GameMessage message
    ) throws IOException {

        String roomId =
                requireRoomId(
                        message.getRoomId()
                );

        GameRoom room =
                roomManager.requireRoom(roomId);

        if (room.getGameMode()
                == GameMode.HOST) {

            hostGameService.lockSecretWord(
                    roomId,
                    authenticatedPlayerId,
                    HostProtocol.parseSecretWord(
                            message.getContent()
                    )
            );

            sendMessage(
                    new GameMessage(
                            HostProtocol.WORD_LOCKED,
                            authenticatedPlayerId,
                            roomId,
                            HostProtocol.encodeWordLocked()
                    )
            );

            return;
        }

        if (room.getGameMode()
                == GameMode.DOUBLE_HOST) {

            doubleHostGameService.lockSecretWord(
                    roomId,
                    authenticatedPlayerId,
                    DoubleHostProtocol.parseSecretWord(
                            message.getContent()
                    )
            );

            boolean bothWordsLocked =
                    doubleHostGameService
                            .areBothWordsLocked(
                                    roomId
                            );

            sendMessage(
                    new GameMessage(
                            DoubleHostProtocol.WORD_LOCKED,
                            authenticatedPlayerId,
                            roomId,
                            DoubleHostProtocol
                                    .encodeWordLocked(
                                            bothWordsLocked
                                    )
                    )
            );

            if (bothWordsLocked) {
                broadcastToRoom(
                        room,
                        new GameMessage(
                                DoubleHostProtocol.WORD_LOCKED,
                                "",
                                roomId,
                                DoubleHostProtocol
                                        .encodeWordLocked(true)
                        )
                );
            }

            return;
        }

        throw new IllegalStateException(
                "Secret-word locking is not supported "
                        + "for this game mode."
        );
    }

    private void processStartRound(
            GameMessage message
    ) {
        String roomId =
                requireRoomId(
                        message.getRoomId()
                );

        GameRoom room =
                roomManager.requireRoom(roomId);

        PlayerSessionRegistry.PlayerSession session =
                requireAuthenticatedSession();

        GameMessage roundStarted;

        switch (room.getGameMode()) {
            case DUEL -> {
                DuelRound round =
                        duelGameService.startRound(
                                roomId,
                                authenticatedPlayerId
                        );

                roundStarted =
                        new GameMessage(
                                DuelProtocol.ROUND_STATE,
                                "",
                                roomId,
                                DuelProtocol
                                        .encodeRoundStarted(
                                                round
                                        )
                        );
            }

            case COOPERATIVE -> {
                CooperativeRound round =
                        cooperativeGameService.startRound(
                                roomId,
                                authenticatedPlayerId
                        );

                roundStarted =
                        new GameMessage(
                                CooperativeProtocol.ROUND_STATE,
                                "",
                                roomId,
                                CooperativeProtocol
                                        .encodeRoundStarted(
                                                round
                                        )
                        );
            }

            case HOST -> {
                HostRound round =
                        hostGameService.startRound(
                                roomId,
                                authenticatedPlayerId
                        );

                roundStarted =
                        new GameMessage(
                                HostProtocol.ROUND_STATE,
                                "",
                                roomId,
                                HostProtocol
                                        .encodeRoundStarted(
                                                round
                                        )
                        );
            }

            case DOUBLE_HOST -> {
                DoubleHostRound round =
                        doubleHostGameService.startRound(
                                roomId,
                                authenticatedPlayerId
                        );

                roundStarted =
                        new GameMessage(
                                DoubleHostProtocol.ROUND_STATE,
                                "",
                                roomId,
                                DoubleHostProtocol
                                        .encodeRoundStarted(
                                                round
                                        )
                        );
            }

            default -> throw new IllegalStateException(
                    "This game mode is not implemented yet."
            );
        }

        broadcastRoomState(room);
        broadcastToRoom(room, roundStarted);

        gameEventNotifier.roundStarted(
                roomId,
                session.playerId(),
                session.username()
        );
    }

    private void processGuess(
            GameMessage message
    ) throws IOException {

        String roomId =
                requireRoomId(
                        message.getRoomId()
                );

        GameRoom room =
                roomManager.requireRoom(roomId);

        switch (room.getGameMode()) {
            case DUEL ->
                    processDuelGuess(
                            room,
                            message.getContent()
                    );

            case COOPERATIVE ->
                    processCooperativeGuess(
                            room,
                            message.getContent()
                    );

            case HOST ->
                    processHostGuess(
                            room,
                            message.getContent()
                    );

            case DOUBLE_HOST ->
                    processDoubleHostGuess(
                            room,
                            message.getContent()
                    );
        }
    }

    private void processDuelGuess(
            GameRoom room,
            String content
    ) throws IOException {

        String roomId =
                room.getRoomId();

        DuelRound.GuessOutcome outcome =
                duelGameService.submitGuess(
                        roomId,
                        authenticatedPlayerId,
                        DuelProtocol.parseGuess(content)
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

        DuelRound round =
                duelGameService.requireActiveRound(
                        roomId
                );

        broadcastRoomState(room);

        broadcastToRoom(
                room,
                new GameMessage(
                        DuelProtocol.ROUND_STATE,
                        "",
                        roomId,
                        DuelProtocol.encodeRoundProgress(
                                round
                        )
                )
        );

        if (outcome.evaluation().solved()) {
            publishSolvedAndScore(
                    room,
                    authenticatedPlayerId
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
                                    round
                            )
                    )
            );

            publishRoundEnded(
                    room,
                    round.getWinnerPlayerId()
                            .orElse("")
            );
        }
    }

    private void processCooperativeGuess(
            GameRoom room,
            String content
    ) {
        String roomId =
                room.getRoomId();

        CooperativeRound.GuessOutcome outcome =
                cooperativeGameService.submitGuess(
                        roomId,
                        authenticatedPlayerId,
                        CooperativeProtocol.parseGuess(
                                content
                        )
                );

        CooperativeRound round =
                cooperativeGameService
                        .requireActiveRound(roomId);

        broadcastToRoom(
                room,
                new GameMessage(
                        CooperativeProtocol.GUESS_RESULT,
                        authenticatedPlayerId,
                        roomId,
                        CooperativeProtocol
                                .encodeGuessResult(
                                        outcome
                                )
                )
        );

        broadcastRoomState(room);

        broadcastToRoom(
                room,
                new GameMessage(
                        CooperativeProtocol.ROUND_STATE,
                        "",
                        roomId,
                        CooperativeProtocol
                                .encodeRoundProgress(
                                        round
                                )
                )
        );

        if (outcome.solved()) {
            publishSolvedAndScore(
                    room,
                    outcome.solvingPlayerId()
            );

            publishScores(room);
        }

        if (outcome.roundFinished()) {
            broadcastToRoom(
                    room,
                    new GameMessage(
                            CooperativeProtocol.GAME_OVER,
                            "",
                            roomId,
                            CooperativeProtocol
                                    .encodeGameOver(
                                            round
                                    )
                    )
            );

            publishRoundEnded(
                    room,
                    outcome.solvingPlayerId()
            );
        }
    }

    private void processHostGuess(
            GameRoom room,
            String content
    ) {
        String roomId =
                room.getRoomId();

        HostRound.GuessOutcome outcome =
                hostGameService.submitGuess(
                        roomId,
                        authenticatedPlayerId,
                        HostProtocol.parseGuess(content)
                );

        HostRound round =
                hostGameService.requireActiveRound(
                        roomId
                );

        broadcastToRoom(
                room,
                new GameMessage(
                        HostProtocol.GUESS_RESULT,
                        authenticatedPlayerId,
                        roomId,
                        HostProtocol.encodeGuessResult(
                                outcome
                        )
                )
        );

        broadcastRoomState(room);

        broadcastToRoom(
                room,
                new GameMessage(
                        HostProtocol.ROUND_STATE,
                        "",
                        roomId,
                        HostProtocol.encodeRoundProgress(
                                round
                        )
                )
        );

        if (outcome.solved()) {
            publishSolvedAndScore(
                    room,
                    outcome.solvingPlayerId()
            );
        }

        if (outcome.roundFinished()) {
            broadcastToRoom(
                    room,
                    new GameMessage(
                            HostProtocol.GAME_OVER,
                            "",
                            roomId,
                            HostProtocol.encodeGameOver(
                                    round
                            )
                    )
            );

            publishScores(room);

            publishRoundEnded(
                    room,
                    round.isSolved()
                            ? outcome.solvingPlayerId()
                            : round.getHostPlayerId()
            );
        }
    }

    private void processDoubleHostGuess(
            GameRoom room,
            String content
    ) throws IOException {

        String roomId =
                room.getRoomId();

        DoubleHostRound.GuessOutcome outcome =
                doubleHostGameService.submitGuess(
                        roomId,
                        authenticatedPlayerId,
                        DoubleHostProtocol.parseGuess(
                                content
                        )
                );

        DoubleHostRound round =
                doubleHostGameService
                        .requireActiveRound(
                                roomId
                        );

        /*
         * Guess feedback remains private because each
         * player is solving a different secret word.
         */
        sendMessage(
                new GameMessage(
                        DoubleHostProtocol.GUESS_RESULT,
                        authenticatedPlayerId,
                        roomId,
                        DoubleHostProtocol
                                .encodeGuessResult(
                                        outcome
                                )
                )
        );

        broadcastRoomState(room);

        broadcastToRoom(
                room,
                new GameMessage(
                        DoubleHostProtocol.ROUND_STATE,
                        "",
                        roomId,
                        DoubleHostProtocol
                                .encodeRoundProgress(
                                        round
                                )
                )
        );

        if (outcome.evaluation().solved()) {
            publishSolvedAndScore(
                    room,
                    authenticatedPlayerId
            );
        }

        if (outcome.roundFinished()) {
            broadcastToRoom(
                    room,
                    new GameMessage(
                            DoubleHostProtocol.GAME_OVER,
                            "",
                            roomId,
                            DoubleHostProtocol
                                    .encodeGameOver(
                                            round
                                    )
                    )
            );

            publishRoundEnded(
                    room,
                    outcome.winnerPlayerId()
            );
        }
    }

    private void publishSolvedAndScore(
            GameRoom room,
            String playerId
    ) {
        if (playerId == null
                || playerId.isBlank()) {

            return;
        }

        GamePlayer player =
                room.requirePlayer(playerId);

        gameEventNotifier.playerSolved(
                room.getRoomId(),
                player.getPlayerId(),
                player.getUsername()
        );

        gameEventNotifier.scoreUpdated(
                room.getRoomId(),
                player.getPlayerId(),
                player.getUsername(),
                player.getScore()
        );
    }

    private void publishScores(
            GameRoom room
    ) {
        for (PlayerView player :
                room.getPlayerViews()) {

            if (player.getScore() > 0) {
                gameEventNotifier.scoreUpdated(
                        room.getRoomId(),
                        player.getPlayerId(),
                        player.getUsername(),
                        player.getScore()
                );
            }
        }
    }

    private void publishRoundEnded(
            GameRoom room,
            String winnerId
    ) {
        String safeWinnerId =
                winnerId == null
                        ? ""
                        : winnerId;

        String winnerUsername =
                safeWinnerId.isBlank()
                        ? ""
                        : room.requirePlayer(
                        safeWinnerId
                ).getUsername();

        gameEventNotifier.roundEnded(
                room.getRoomId(),
                safeWinnerId,
                winnerUsername
        );
    }

    private void requireAuthenticatedRequest(
            GameMessage message
    ) {
        if (authenticatedPlayerId == null) {
            throw RoomProtocol.protocolError(
                    RoomProtocol.ErrorCode
                            .UNAUTHENTICATED,
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

    private String requireRoomId(
            String roomId
    ) {
        if (roomId == null
                || roomId.isBlank()) {

            throw RoomProtocol.protocolError(
                    RoomProtocol.ErrorCode
                            .INVALID_REQUEST_FORMAT,
                    "Room ID is required."
            );
        }

        return roomId.trim();
    }

    private void broadcastRoomState(
            GameRoom room
    ) {
        broadcastToRoom(
                room,
                new GameMessage(
                        RoomProtocol.ROOM_STATE,
                        "",
                        room.getRoomId(),
                        RoomProtocol.encodeRoomState(
                                room
                        )
                )
        );

        broadcastToRoom(
                room,
                new GameMessage(
                        RoomProtocol.ROOM_PLAYERS,
                        "",
                        room.getRoomId(),
                        RoomProtocol.encodeRoomPlayers(
                                room.getPlayerViews()
                        )
                )
        );
    }

    private void broadcastToRoom(
            GameRoom room,
            GameMessage message
    ) {
        for (PlayerView player :
                room.getPlayerViews()) {

            connectionRegistry.sendToPlayer(
                    player.getPlayerId(),
                    message
            );
        }
    }

    private void notifyRoomClosed(
            List<String> playerIds,
            String roomId
    ) {
        GameMessage message =
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

        for (String playerId : playerIds) {
            if (!playerId.equals(
                    authenticatedPlayerId
            )) {
                connectionRegistry.sendToPlayer(
                        playerId,
                        message
                );
            }
        }
    }

    private List<String> extractPlayerIds(
            GameRoom room
    ) {
        List<String> ids =
                new ArrayList<>();

        for (PlayerView player :
                room.getPlayerViews()) {

            ids.add(
                    player.getPlayerId()
            );
        }

        return ids;
    }

    private void sendDomainError(
            String roomId,
            IllegalStateException exception
    ) throws IOException {

        sendMessage(
                new GameMessage(
                        ERROR,
                        "",
                        safeRoomId(roomId),
                        RoomProtocol.encodeError(
                                RoomProtocol.ErrorCode
                                        .INTERNAL_ERROR,
                                exception.getMessage()
                        )
                )
        );
    }

    private void sendProtocolError(
            RoomProtocol.ErrorCode errorCode,
            String message
    ) throws IOException {

        sendMessage(
                new GameMessage(
                        ERROR,
                        "",
                        "",
                        RoomProtocol.encodeError(
                                errorCode,
                                message
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

    private String safeRoomId(
            String roomId
    ) {
        return roomId == null
                ? ""
                : roomId;
    }

    private void handleDisconnection() {
        if (authenticatedPlayerId == null) {
            return;
        }

        String playerId =
                authenticatedPlayerId;

        PlayerSessionRegistry.PlayerSession session =
                sessionRegistry.findByPlayerId(
                        playerId
                ).orElse(null);

        roomManager.findRoomIdForPlayer(
                        playerId
                )
                .ifPresent(roomId -> {
                    GameRoom room =
                            roomManager.findRoom(
                                    roomId
                            ).orElse(null);

                    List<String> previousIds =
                            room == null
                                    ? List.of()
                                    : extractPlayerIds(
                                    room
                            );

                    boolean wasHost =
                            room != null
                                    && room.isHost(
                                    playerId
                            );

                    roomManager.removePlayerAfterDisconnect(
                            playerId
                    );

                    if (session != null) {
                        gameEventNotifier.playerLeft(
                                roomId,
                                session.playerId(),
                                session.username()
                        );
                    }

                    if (room != null
                            && wasHost) {

                        notifyRoomClosed(
                                previousIds,
                                roomId
                        );
                    } else {
                        roomManager.findRoom(
                                        roomId
                                )
                                .ifPresent(
                                        this::broadcastRoomState
                                );
                    }
                });

        connectionRegistry.remove(
                playerId,
                this
        );

        sessionRegistry.remove(
                playerId
        );

        authenticatedPlayerId = null;
    }
}