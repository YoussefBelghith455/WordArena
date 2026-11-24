
package com.wordarena.client.tcp;

import com.wordarena.common.GameMessage;
import com.wordarena.gui.DoubleHostMultiplayerView;
import com.wordarena.gui.LobbyView;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.function.Consumer;

public class DoubleHostGameController {

    private final GameSession gameSession;
    private final DoubleHostMultiplayerView view;
    private final String roomId;

    private Consumer<String[]> resultHandler;

    private boolean started;
    private boolean finished;
    private boolean awaitingFinalResult;
    private boolean leavingRoom;
    private boolean closed;

    public DoubleHostGameController(
            GameSession gameSession,
            DoubleHostMultiplayerView view
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
        if (closed || finished || awaitingFinalResult
                || leavingRoom) {
            return;
        }

        if (!gameSession.getTcpClient().isConnected()) {
            handleConnectionFailure(
                    new IOException("Connection unavailable.")
            );
            return;
        }

        if (guess == null || !guess.matches("[A-Z]{5}")) {
            view.showError("Enter exactly 5 English letters.");
            return;
        }

        if (view.getAttemptsUsed() >= view.getMaxAttempts()) {
            view.showError("No attempts remaining.");
            return;
        }

        sendRequest("GUESS", guess);
    }

    private void sendRequest(String type, String content) {
        Thread thread = new Thread(() -> {
            try {
                gameSession.send(type, roomId, content);
            } catch (IOException exception) {
                Platform.runLater(() -> {
                    if (closed) {
                        return;
                    }

                    if (!gameSession.getTcpClient().isConnected()) {
                        handleConnectionFailure(exception);
                        return;
                    }

                    if ("LEAVE_ROOM".equals(type)) {
                        leavingRoom = false;
                        view.setLeaveHandler(this::leaveRoom);
                    }

                    view.showError(
                            "Unable to send request: "
                                    + exception.getMessage()
                    );
                });
            }
        }, "wordarena-double-host-" + type.toLowerCase());

        thread.setDaemon(true);
        thread.start();
    }

    private void handleServerMessage(GameMessage message) {
        if (closed || message == null) {
            return;
        }

        if ("OK".equals(message.getType())
                && leavingRoom
                && roomId.equals(message.getRoomId())
                && gameSession.getPlayerId().equals(message.getPlayerId())
                && "Successfully left the room.".equals(message.getContent())) {
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
                    handleGuessResult(message.getContent());

            case "ROOM_PLAYERS" ->
                    handleRoomPlayers(message.getContent());

            case "ROUND_STATE" ->
                    handleRoundState(message.getContent());

            case "GAME_OVER" ->
                    handleGameOver(message.getContent());

            default -> {
            }
        }
    }

    private void handleServerError(GameMessage message) {
        String messageRoomId = message.getRoomId();

        if (messageRoomId != null
                && !messageRoomId.isBlank()
                && !roomId.equals(messageRoomId)) {
            return;
        }

        String error = message.getContent() == null
                ? "Unknown server error."
                : message.getContent();

        if (error.startsWith("ROOM_NOT_FOUND|")
                && roomId.equals(messageRoomId)) {
            returnToLobby();
            return;
        }

        if (leavingRoom) {
            leavingRoom = false;
            view.setLeaveHandler(this::leaveRoom);
            view.showError("Unable to leave room: " + error);
            return;
        }

        if (!finished) {
            view.showError("Server error: " + error);
        }
    }

    private void handleGuessResult(String content) {
        if (content == null) {
            return;
        }

        String[] fields = content.split("\\|", -1);

        if (fields.length != 8
                || !"DOUBLE_HOST".equals(fields[0])) {
            return;
        }

        String word = fields[1];
        String[] feedback = fields[2].split(",", -1);
        String playerId = fields[5];

        if (!gameSession.getPlayerId().equals(playerId)) {
            return;
        }

        if (!word.matches("[A-Z]{5}")
                || feedback.length != 5
                || (!"true".equals(fields[7])
                && !"false".equals(fields[7]))) {
            view.showError("Invalid guess response.");
            return;
        }

        for (String value : feedback) {
            if (!"CORRECT".equals(value)
                    && !"PRESENT".equals(value)
                    && !"ABSENT".equals(value)) {
                view.showError("Invalid letter feedback.");
                return;
            }
        }

        try {
            int attemptsUsed = Integer.parseInt(fields[3]);
            int attemptsRemaining = Integer.parseInt(fields[4]);
            int score = Integer.parseInt(fields[6]);
            boolean solved = Boolean.parseBoolean(fields[7]);

            if (attemptsUsed < 1
                    || attemptsUsed > view.getMaxAttempts()
                    || attemptsRemaining < 0
                    || attemptsUsed + attemptsRemaining
                    != view.getMaxAttempts()
                    || score < 0) {
                view.showError("Invalid guess statistics.");
                return;
            }

            view.displayGuess(
                    word,
                    feedback,
                    attemptsUsed,
                    playerId,
                    gameSession.getUsername(),
                    solved
            );

            view.updateAttempts(attemptsUsed);
            view.updatePlayerScore(score);

            if (!finished && (solved || attemptsRemaining == 0)) {
                awaitingFinalResult = true;
                view.awaitFinalResult();
            }

        } catch (NumberFormatException exception) {
            view.showError("Invalid guess statistics.");
        }
    }

    private void handleRoomPlayers(String content) {
        if (content == null || content.isBlank()) {
            return;
        }

        String[] players = content.split(";", -1);

        for (String player : players) {
            String[] fields = player.split("~", -1);

            if (fields.length != 6) {
                continue;
            }

            try {
                String playerId = fields[0];
                int attempts = Integer.parseInt(fields[3]);
                int score = Integer.parseInt(fields[4]);

                if (attempts < 0
                        || attempts > view.getMaxAttempts()
                        || score < 0) {
                    continue;
                }

                if (gameSession.getPlayerId().equals(playerId)) {
                    view.updateAttempts(attempts);
                    view.updatePlayerScore(score);
                } else {
                    view.updateOpponentStats(attempts, score);
                }

            } catch (NumberFormatException exception) {
                continue;
            }
        }
    }

    private void handleRoundState(String content) {
        if (finished || content == null) {
            return;
        }

        String[] fields = content.split("\\|", -1);

        if (fields.length != 3
                || !"DOUBLE_HOST".equals(fields[0])) {
            return;
        }

        if ("FINISHED".equals(fields[1])) {
            awaitingFinalResult = true;
            view.awaitFinalResult();
        }
    }

    private void handleGameOver(String content) {
        if (finished || content == null) {
            return;
        }

        String[] fields = content.split("\\|", -1);

        if (fields.length != 7
                || !"DOUBLE_HOST".equals(fields[0])) {
            return;
        }

        String result = fields[1];
        String winnerId = fields[2];
        String firstPlayerId = fields[3];
        String firstTargetWord = fields[4];
        String secondPlayerId = fields[5];
        String secondTargetWord = fields[6];

        if (!"WINNER".equals(result)
                && !"NO_WINNER".equals(result)) {
            return;
        }

        if (!firstTargetWord.matches("[A-Z]{5}")
                || !secondTargetWord.matches("[A-Z]{5}")
                || firstPlayerId.isBlank()
                || secondPlayerId.isBlank()
                || firstPlayerId.equals(secondPlayerId)) {
            return;
        }

        if ("WINNER".equals(result)
                && !winnerId.equals(firstPlayerId)
                && !winnerId.equals(secondPlayerId)) {
            return;
        }

        if ("NO_WINNER".equals(result)
                && !winnerId.isBlank()) {
            return;
        }

        String ownPlayerId = gameSession.getPlayerId();
        String targetWord;

        if (ownPlayerId.equals(firstPlayerId)) {
            targetWord = firstTargetWord;
        } else if (ownPlayerId.equals(secondPlayerId)) {
            targetWord = secondTargetWord;
        } else {
            return;
        }

        finished = true;
        awaitingFinalResult = false;

        String message;

        if ("NO_WINNER".equals(result)) {
            message = "DRAW";
        } else if (ownPlayerId.equals(winnerId)) {
            message = "VICTORY!";
        } else {
            message = "DEFEAT!";
        }

        view.finishRound(
                message + " Opponent's secret word: " + targetWord
        );

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
                    new IOException("Connection unavailable.")
            );
            return;
        }

        leavingRoom = true;

        view.setLeaveHandler(null);
        view.showError("Leaving room...");

        sendRequest("LEAVE_ROOM", "");
    }

    private void returnToLobby() {
        if (closed || view.getScene() == null
                || !(view.getScene().getWindow() instanceof Stage stage)) {
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

        var stylesheet = getClass().getResource(
                "/css/wordarena.css"
        );

        if (stylesheet != null) {
            scene.getStylesheets().add(
                    stylesheet.toExternalForm()
            );
        }

        stage.setScene(scene);
        stage.setTitle("WordArena - Lobby");
    }

    private void handleConnectionFailure(Exception exception) {
        if (closed) {
            return;
        }

        leavingRoom = false;

        view.setGuessHandler(null);
        view.setLeaveHandler(null);

        view.handleConnectionFailure(
                "Connection lost: " + exception.getMessage()
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
