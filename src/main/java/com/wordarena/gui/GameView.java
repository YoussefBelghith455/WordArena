
package com.wordarena.gui;

import com.wordarena.common.GameMode;
import com.wordarena.jms.EventSubscriber;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class GameView extends VBox {

    private final Label[][] cells = new Label[6][5];
    private final List<String> pendingNotifications = new ArrayList<>();

    private int currentAttempt = 0;
    private final String secretWord = "TRAIN";

    private EventSubscriber eventSubscriber;
    private PlayerPanelView playerPanel;
    private CooperativePanelView cooperativePanel;
    private GameHeaderView gameHeader;
    private EventFeedView eventFeed;

    private final String username;
    private final String opponentUsername;
    private final String roomName;
    private final GameMode gameMode;

    public GameView(String username) {
        this(username, "Waiting for player...", "Room 1", GameMode.DUEL);
    }

    public GameView(String username, String opponentUsername) {
        this(username, opponentUsername, "Room 1", GameMode.DUEL);
    }

    public GameView(
            String username,
            String opponentUsername,
            String roomName,
            GameMode gameMode
    ) {

        this.username = username;
        this.opponentUsername = opponentUsername;
        this.roomName = roomName;
        this.gameMode = gameMode;

        getStyleClass().add("game-root");
        setSpacing(20);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(30));

        Label title = new Label("WORDARENA");
        title.getStyleClass().add("lobby-title");

        eventFeed = new EventFeedView();
        gameHeader = new GameHeaderView(roomName, gameMode);

        Label attemptsLabel = new Label("Attempts remaining: 6");
        attemptsLabel.getStyleClass().add("game-attempts");

        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(6);
        grid.setAlignment(Pos.CENTER);

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 5; col++) {

                Label cell = new Label("");
                cell.setMinSize(55, 55);
                cell.setPrefSize(55, 55);
                cell.setMaxSize(55, 55);
                cell.setAlignment(Pos.CENTER);
                cell.getStyleClass().add("wordle-cell");

                cells[row][col] = cell;
                grid.add(cell, col, row);
            }
        }

        Region leftPanel;

        if (gameMode == GameMode.COOPERATIVE) {

            cooperativePanel = new CooperativePanelView(
                    username,
                    opponentUsername
            );

            leftPanel = cooperativePanel;

        } else {

            playerPanel = new PlayerPanelView(username);
            playerPanel.updateOpponentName(opponentUsername);

            leftPanel = playerPanel;
        }

        HBox gameLayout = new HBox(
                20,
                leftPanel,
                grid,
                eventFeed
        );

        gameLayout.setAlignment(Pos.CENTER);

        TextField guessField = new TextField();
        guessField.setPromptText("Enter a 5-letter word");
        guessField.setMaxWidth(250);

        Button submitButton = new Button("Submit Guess");
        submitButton.setPrefWidth(250);
        submitButton.getStyleClass().add("neon-button");

        OnScreenKeyboard keyboard = new OnScreenKeyboard(key -> {

            if (key.equals("ENTER")) {

                submitButton.fire();

            } else if (key.equals("BACKSPACE")) {

                String current = guessField.getText();

                if (!current.isEmpty()) {

                    guessField.setText(
                            current.substring(0, current.length() - 1)
                    );
                }

            } else if (guessField.getText().length() < 5) {

                guessField.appendText(key);
            }
        });

        Label messageLabel = new Label();
        messageLabel.setWrapText(true);

        Button playAgainButton = new Button("Play Again");
        playAgainButton.setPrefWidth(250);
        playAgainButton.getStyleClass().add("neon-button");
        playAgainButton.setDisable(true);

        Button backButton = new Button("Back to Lobby");
        backButton.setPrefWidth(250);
        backButton.getStyleClass().add("secondary-button");

        getChildren().addAll(
                title,
                gameHeader,
                attemptsLabel,
                gameLayout,
                guessField,
                keyboard,
                submitButton,
                messageLabel,
                playAgainButton,
                backButton
        );

        String brokerUrl = "tcp://localhost:61616";
        String jmsUser = System.getenv("WORDARENA_JMS_USER");
        String jmsPassword = System.getenv("WORDARENA_JMS_PASSWORD");

        if (jmsUser != null && jmsPassword != null) {

            try {

                eventSubscriber = new EventSubscriber(
                        brokerUrl,
                        jmsUser,
                        jmsPassword,
                        event -> {

                            if ("room1".equals(event.getRoomId())) {

                                String message = event.getMessage();

                                eventFeed.addEvent(message);
                                showToast(message);
                            }
                        }
                );

                gameHeader.updateConnectionStatus("CONNECTED");

                eventFeed.addEvent("Connected to Artemis JMS");
                showToast("Connected to Artemis JMS");

            } catch (Exception e) {

                gameHeader.updateConnectionStatus("ERROR");

                eventFeed.addEvent("JMS connection failed");
                showToast("JMS connection failed");
            }

        } else {

            gameHeader.updateConnectionStatus("DISCONNECTED");

            eventFeed.addEvent("JMS credentials not configured");
            showToast("JMS credentials not configured");
        }

        submitButton.setOnAction(event -> {

            String guess = guessField.getText().trim().toUpperCase();

            if (!guess.matches("[A-Z]{5}")) {

                messageLabel.setText("Enter exactly 5 letters");
                return;
            }

            if (currentAttempt >= 6) {

                messageLabel.setText("No attempts remaining");
                return;
            }

            int[] feedback = new int[5];
            int[] remainingLetters = new int[26];

            for (int i = 0; i < 5; i++) {

                if (guess.charAt(i) == secretWord.charAt(i)) {

                    feedback[i] = 2;

                } else {

                    remainingLetters[secretWord.charAt(i) - 'A']++;
                }
            }

            for (int i = 0; i < 5; i++) {

                if (feedback[i] == 0) {

                    int index = guess.charAt(i) - 'A';

                    if (remainingLetters[index] > 0) {

                        feedback[i] = 1;
                        remainingLetters[index]--;
                    }
                }
            }

            for (int col = 0; col < 5; col++) {

                Label cell = cells[currentAttempt][col];

                cell.setText(
                        String.valueOf(guess.charAt(col))
                );

                keyboard.updateKeyFeedback(
                        String.valueOf(guess.charAt(col)),
                        feedback[col]
                );

                String color = switch (feedback[col]) {

                    case 2 -> "#16a34a";
                    case 1 -> "#ca8a04";
                    default -> "#64748b";
                };

                cell.setStyle(
                        "-fx-background-color: " + color + ";" +
                                "-fx-border-color: " + color + ";" +
                                "-fx-background-radius: 8;" +
                                "-fx-border-radius: 8;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-size: 24px;" +
                                "-fx-font-weight: bold;"
                );
            }

            currentAttempt++;

            if (gameMode == GameMode.COOPERATIVE) {

                cooperativePanel.updateSharedAttempts(currentAttempt);

                cooperativePanel.updateLastGuess(
                        username,
                        guess
                );

                cooperativePanel.updateStatus(
                        "Team guess submitted"
                );

            } else {

                playerPanel.updatePlayerProgress(
                        currentAttempt / 6.0
                );
            }

            attemptsLabel.setText(
                    "Attempts remaining: " + (6 - currentAttempt)
            );

            messageLabel.setText(
                    "Guess submitted: " + guess
            );

            guessField.clear();

            if (guess.equals(secretWord)) {

                messageLabel.setText(
                        "Congratulations! You found the word!"
                );

                gameHeader.updateStatus("FINISHED");

                if (cooperativePanel != null) {

                    cooperativePanel.updateStatus(
                            "TEAM VICTORY!"
                    );
                }

                eventFeed.addEvent(
                        username + " solved the word!"
                );

                showToast(
                        username + " solved the word!"
                );

                submitButton.setDisable(true);
                guessField.setDisable(true);
                keyboard.setKeyboardDisabled(true);
                playAgainButton.setDisable(false);

                showResultsPopup(true);
                return;
            }

            if (currentAttempt == 6) {

                messageLabel.setText(
                        "Game over - No attempts remaining"
                );

                gameHeader.updateStatus("FINISHED");

                if (cooperativePanel != null) {

                    cooperativePanel.updateStatus(
                            "ROUND LOST"
                    );
                }

                eventFeed.addEvent(
                        "Round ended - No attempts remaining"
                );

                showToast(
                        "Round ended - No attempts remaining"
                );

                submitButton.setDisable(true);
                guessField.setDisable(true);
                keyboard.setKeyboardDisabled(true);
                playAgainButton.setDisable(false);

                showResultsPopup(false);
            }
        });

        playAgainButton.setOnAction(event -> restartGame());
        backButton.setOnAction(event -> returnToLobby());

        sceneProperty().addListener(
                (observable, oldScene, newScene) -> {

                    if (newScene != null) {

                        Platform.runLater(
                                this::displayPendingNotifications
                        );
                    }
                }
        );
    }

    private void showResultsPopup(boolean won) {

        if (getScene() == null ||
                !(getScene().getRoot() instanceof StackPane overlay)) {

            return;
        }

        GameResultsView resultsView = new GameResultsView();

        resultsView.showResult(
                won,
                secretWord,
                currentAttempt
        );

        Button playAgain = new Button("PLAY AGAIN");
        playAgain.getStyleClass().add("neon-button");
        playAgain.setPrefWidth(220);

        Button backToLobby = new Button("BACK TO LOBBY");
        backToLobby.getStyleClass().add("secondary-button");
        backToLobby.setPrefWidth(220);

        playAgain.setOnAction(event -> restartGame());
        backToLobby.setOnAction(event -> returnToLobby());

        VBox popup = new VBox(
                20,
                resultsView,
                playAgain,
                backToLobby
        );

        popup.setAlignment(Pos.CENTER);
        popup.setMaxWidth(450);
        popup.setMaxHeight(Region.USE_PREF_SIZE);
        popup.getStyleClass().add("results-popup");

        StackPane backdrop = new StackPane(popup);

        backdrop.setStyle(
                "-fx-background-color: rgba(0, 0, 0, 0.78);"
        );

        backdrop.setPickOnBounds(true);

        overlay.getChildren().add(backdrop);

        StackPane.setAlignment(
                backdrop,
                Pos.CENTER
        );
    }

    private void restartGame() {

        closeSubscriber();

        GameView newGame = new GameView(
                username,
                opponentUsername,
                roomName,
                gameMode
        );

        ScrollPane scrollPane = new ScrollPane(newGame);
        scrollPane.setFitToWidth(true);

        StackPane overlay = new StackPane(scrollPane);

        Scene scene = new Scene(overlay, 1100, 800);
        applyTheme(scene);

        Stage stage = (Stage) getScene().getWindow();

        stage.setScene(scene);
        stage.setTitle("WordArena - Game");
    }

    private void returnToLobby() {

        closeSubscriber();

        LobbyView lobby = new LobbyView(username);

        Scene scene = new Scene(lobby, 800, 600);
        applyTheme(scene);

        Stage stage = (Stage) getScene().getWindow();

        stage.setScene(scene);
        stage.setTitle("WordArena - Lobby");
    }

    private void showToast(String message) {

        if (!Platform.isFxApplicationThread()) {

            Platform.runLater(
                    () -> showToast(message)
            );

            return;
        }

        if (getScene() == null ||
                !(getScene().getRoot() instanceof StackPane overlay)) {

            pendingNotifications.add(message);
            return;
        }

        VBox toastContainer =
                (VBox) overlay.lookup("#toastContainer");

        if (toastContainer == null) {

            toastContainer = new VBox(10);
            toastContainer.setId("toastContainer");
            toastContainer.setAlignment(Pos.TOP_RIGHT);
            toastContainer.setMaxWidth(Region.USE_PREF_SIZE);
            toastContainer.setMaxHeight(Region.USE_PREF_SIZE);
            toastContainer.setMouseTransparent(true);

            StackPane.setAlignment(
                    toastContainer,
                    Pos.TOP_RIGHT
            );

            StackPane.setMargin(
                    toastContainer,
                    new Insets(20)
            );

            overlay.getChildren().add(toastContainer);
        }

        ToastNotification toast = new ToastNotification(message);

        toastContainer.getChildren().add(toast);
    }

    private void displayPendingNotifications() {

        if (getScene() == null ||
                !(getScene().getRoot() instanceof StackPane)) {

            return;
        }

        List<String> messages =
                new ArrayList<>(pendingNotifications);

        pendingNotifications.clear();

        for (String message : messages) {

            showToast(message);
        }
    }

    private void applyTheme(Scene scene) {

        scene.getStylesheets().add(
                getClass()
                        .getResource("/css/wordarena.css")
                        .toExternalForm()
        );
    }

    public void shutdown() {
        closeSubscriber();
    }

    private void closeSubscriber() {

        if (eventSubscriber != null) {

            eventSubscriber.close();
            eventSubscriber = null;
        }

        gameHeader.updateConnectionStatus("DISCONNECTED");
    }
}
