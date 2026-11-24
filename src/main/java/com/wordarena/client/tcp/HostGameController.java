package com.wordarena.client.tcp;

import com.wordarena.common.GameMessage;
import com.wordarena.gui.HostMultiplayerView;
import com.wordarena.gui.LobbyView;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.function.Consumer;

public class HostGameController {

    private final GameSession gameSession;
    private final HostMultiplayerView view;
    private final String roomId;

    private Consumer<String[]> resultHandler;

    private boolean finished;
    private boolean leavingRoom;
    private boolean closed;
    private boolean started;

    public HostGameController(
            GameSession gameSession,
            HostMultiplayerView view
    ) {
        this.gameSession = gameSession;
        this.view = view;
        this.roomId = view.getRoomId();
    }

    public void start() {

        if (started || closed) {
            return;
        }

        started = true;

        view.setGuessHandler(this::submitGuess);
        view.setLeaveHandler(this::leaveRoom);

        gameSession.setMessageHandler(
                this::handleServerMessage
        );

        gameSession.setErrorHandler(
                this::handleConnectionFailure
        );
    }

    private void submitGuess(String guess) {

        if (closed || finished
                || leavingRoom || view.isHost()) {
            return;
        }

        if (!gameSession.getTcpClient().isConnected()) {

            handleConnectionFailure(
                    new IOException("Not connected to the server.")
            );

            return;
        }

        sendRequest("GUESS", guess);
    }

    private void sendRequest(
            String type,
            String content
    ) {

        Thread thread = new Thread(() -> {

            try {

                gameSession.send(
                        type,
                        roomId,
                        content
                );

            } catch (IOException exception) {

                Platform.runLater(() -> {

                    if (closed) {
                        return;
                    }

                    if (!gameSession.getTcpClient()
                            .isConnected()) {

                        handleConnectionFailure(exception);

                        return;
                    }

                    if ("LEAVE_ROOM".equals(type)) {

                        leavingRoom = false;

                        view.setLeaveHandler(
                                this::leaveRoom
                        );
                    }

                    view.showError(
                            "Unable to send request: "
                                    + exception.getMessage()
                    );
                });
            }
        });

        thread.setName(
                "wordarena-host-" + type.toLowerCase()
        );

        thread.setDaemon(true);
        thread.start();
    }

    private void handleServerMessage(
            GameMessage message
    ) {

        if (closed || message == null) {
            return;
        }

        if ("OK".equals(message.getType())
                && leavingRoom
                && roomId.equals(message.getRoomId())
                && gameSession.getPlayerId()
                .equals(message.getPlayerId())
                && "Successfully left the room."
                .equals(message.getContent())) {

            returnToLobby();

            return;
        }

        if ("ERROR".equals(message.getType())) {

            handleServerError(message);

            return;
        }

        if (!roomId.equals(message.getRoomId())) {
            return;
        }

        switch (message.getType()) {

            case "GUESS_RESULT" ->
                    handleGuessResult(
                            message.getContent()
                    );

            case "ROUND_STATE" ->
                    handleRoundState(
                            message.getContent()
                    );

            case "GAME_OVER" ->
                    handleGameOver(
                            message.getContent()
                    );

            default -> {
            }
        }
    }

    private void handleServerError(
            GameMessage message
    ) {

        String responseRoomId = message.getRoomId();

        if (responseRoomId != null
                && !responseRoomId.isBlank()
                && !roomId.equals(responseRoomId)) {
            return;
        }

        String error = message.getContent() == null
                ? "Unknown server error."
                : message.getContent();

        if (error.startsWith("ROOM_NOT_FOUND|")
                && roomId.equals(responseRoomId)) {

            returnToLobby();

            return;
        }

        if (leavingRoom) {

            leavingRoom = false;

            view.setLeaveHandler(this::leaveRoom);

            view.showError(
                    "Unable to leave room: " + error
            );

            return;
        }

        view.showError("Server error: " + error);
    }

    private void handleGuessResult(String content) {

        if (closed || finished || content == null) {
            return;
        }

        String[] fields = content.split("\\|", -1);

        if (fields.length != 9
                || !"HOST".equals(fields[0])) {

            view.showError(
                    "Invalid HOST guess response."
            );

            return;
        }

        try {

            String word = fields[1];

            String[] feedback =
                    fields[2].split(",", -1);

            int attemptNumber =
                    Integer.parseInt(fields[3]);

            int attemptsUsed =
                    Integer.parseInt(fields[4]);

            int attemptsRemaining =
                    Integer.parseInt(fields[5]);

            String playerId = fields[6];
            String username = fields[7];

            boolean solved =
                    Boolean.parseBoolean(fields[8]);

            if (!word.matches("[A-Z]{5}")
                    || feedback.length != 5
                    || attemptNumber < 1
                    || attemptNumber > 6
                    || attemptsUsed != attemptNumber
                    || attemptsRemaining < 0
                    || attemptsUsed + attemptsRemaining != 6
                    || playerId.isBlank()
                    || username.isBlank()
                    || (!"true".equals(fields[8])
                    && !"false".equals(fields[8]))) {

                view.showError(
                        "Invalid HOST guess data."
                );

                return;
            }

            for (String state : feedback) {

                if (!"CORRECT".equals(state)
                        && !"PRESENT".equals(state)
                        && !"ABSENT".equals(state)) {

                    view.showError(
                            "Invalid letter feedback."
                    );

                    return;
                }
            }

            view.displayGuess(
                    word,
                    feedback,
                    attemptNumber,
                    playerId,
                    username,
                    solved
            );

            view.updateSharedAttempts(
                    attemptsUsed
            );

        } catch (NumberFormatException exception) {

            view.showError(
                    "Invalid attempt statistics."
            );
        }
    }

    private void handleRoundState(String content) {

        if (closed || finished || content == null) {
            return;
        }

        String[] fields = content.split("\\|", -1);

        if (fields.length != 5
                || !"HOST".equals(fields[0])) {
            return;
        }

        if (!"ACTIVE".equals(fields[1])
                && !"FINISHED".equals(fields[1])) {
            return;
        }

        try {

            int attemptsUsed =
                    Integer.parseInt(fields[2]);

            int attemptsRemaining =
                    Integer.parseInt(fields[3]);

            if (attemptsUsed < 0
                    || attemptsRemaining < 0
                    || attemptsUsed + attemptsRemaining != 6) {
                return;
            }

            view.updateSharedAttempts(
                    attemptsUsed
            );

        } catch (NumberFormatException exception) {
            return;
        }
    }

    private void handleGameOver(String content) {

        if (closed || finished || content == null) {
            return;
        }

        String[] fields = content.split("\\|", -1);

        if (fields.length != 6
                || !"HOST".equals(fields[0])) {

            view.showError(
                    "Invalid HOST game result."
            );

            return;
        }

        String winningSide = fields[1];
        String secretWord = fields[4];

        if (!"GUESSERS_WIN".equals(winningSide)
                && !"HOST_WINS".equals(winningSide)) {

            view.showError(
                    "Unknown winning side."
            );

            return;
        }

        if (!secretWord.matches("[A-Z]{5}")) {

            view.showError(
                    "Invalid secret word received."
            );

            return;
        }

        int attempts;

        try {

            attempts = Integer.parseInt(fields[5]);

        } catch (NumberFormatException exception) {

            view.showError(
                    "Invalid final attempt count."
            );

            return;
        }

        if (attempts < 1 || attempts > 6) {
            return;
        }

        finished = true;

        boolean hostWon =
                "HOST_WINS".equals(winningSide);

        boolean won =
                view.isHost() == hostWon;

        view.updateSharedAttempts(attempts);

        view.finishRound(
                (won ? "VICTORY!" : "DEFEAT!")
                        + " Secret word: "
                        + secretWord
        );

        view.showHostResults(hostWon, secretWord, attempts);

        if (resultHandler != null) {

            resultHandler.accept(fields);
        }
    }

    public void leaveRoom() {

        if (closed || !finished || leavingRoom) {
            return;
        }

        if (!gameSession.getTcpClient().isConnected()) {

            handleConnectionFailure(
                    new IOException("Connection lost.")
            );

            return;
        }

        leavingRoom = true;

        view.setLeaveHandler(null);

        view.showError("Leaving room...");

        sendRequest("LEAVE_ROOM", "");
    }

    private void returnToLobby() {

        if (closed) {
            return;
        }

        if (view.getScene() == null
                || !(view.getScene().getWindow()
                instanceof Stage stage)) {
            return;
        }

        closed = true;
        leavingRoom = false;

        view.setGuessHandler(null);
        view.setLeaveHandler(null);

        gameSession.clearRoom();

        gameSession.setMessageHandler(null);
        gameSession.setErrorHandler(null);

        LobbyView lobby = new LobbyView(
                gameSession.getUsername(),
                gameSession
        );

        Scene scene = new Scene(lobby, 800, 600);

        var stylesheet = getClass()
                .getResource("/css/wordarena.css");

        if (stylesheet != null) {

            scene.getStylesheets().add(
                    stylesheet.toExternalForm()
            );
        }

        stage.setScene(scene);
        stage.setTitle("WordArena - Lobby");
    }

    private void handleConnectionFailure(
            Exception exception
    ) {

        if (closed) {
            return;
        }

        leavingRoom = false;

        view.setGuessHandler(null);
        view.setLeaveHandler(null);

        view.handleConnectionFailure(
                "Connection lost: "
                        + exception.getMessage()
        );
    }

    public void setResultHandler(
            Consumer<String[]> resultHandler
    ) {

        this.resultHandler = resultHandler;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isLeavingRoom() {
        return leavingRoom;
    }

    public boolean isClosed() {
        return closed;
    }
}
