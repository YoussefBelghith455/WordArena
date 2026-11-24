package com.wordarena.gui;

import com.wordarena.client.tcp.GameSession;
import com.wordarena.client.tcp.GameTcpClient;
import com.wordarena.common.GameMessage;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class WordArenaApp extends Application {

    private Stage primaryStage;
    private GameTcpClient tcpClient;
    private volatile GameTcpClient pendingClient;
    private GameSession gameSession;
    private volatile boolean closing;

    @Override
    public void start(Stage stage) {

        primaryStage = stage;

        Label title = new Label("WordArena");
        title.setStyle("-fx-font-size: 36px; -fx-font-weight: bold;");

        Label subtitle = new Label("Multiplayer Word Guessing Game");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter your username");
        usernameField.setMaxWidth(250);

        Button loginButton = new Button("Join Game");
        loginButton.setPrefWidth(250);

        Label messageLabel = new Label();

        loginButton.setOnAction(event -> {

            String username = usernameField.getText().trim();

            if (username.isEmpty()) {
                messageLabel.setText("Please enter a username");
                return;
            }

            loginButton.setDisable(true);
            messageLabel.setText("Connecting to server...");

            Thread loginThread = new Thread(() -> {

                GameTcpClient client = new GameTcpClient();
                pendingClient = client;

                try {

                    client.connect();

                    if (closing) {
                        closeClient(client);
                        return;
                    }

                    client.setMessageListener(response ->
                            Platform.runLater(() -> {

                                if (closing || pendingClient != client) {
                                    return;
                                }

                                if ("OK".equals(response.getType())) {

                                    String playerId = response.getPlayerId();

                                    if (playerId == null || playerId.isBlank()) {

                                        messageLabel.setText(
                                                "Server returned an invalid player ID."
                                        );

                                        loginButton.setDisable(false);
                                        closeClient(client);
                                        pendingClient = null;
                                        return;
                                    }

                                    tcpClient = client;
                                    pendingClient = null;

                                    gameSession = new GameSession(
                                            tcpClient,
                                            playerId,
                                            username
                                    );

                                    gameSession.startJms();
                                    gameSession.startRmi();

                                    LobbyView lobby = new LobbyView(
                                            username,
                                            gameSession
                                    );

                                    Scene lobbyScene = new Scene(
                                            lobby,
                                            800,
                                            500
                                    );

                                    applyTheme(lobbyScene);

                                    stage.setScene(lobbyScene);
                                    stage.setTitle("WordArena - Lobby");

                                } else if ("ERROR".equals(response.getType())) {

                                    messageLabel.setText(
                                            "Login failed: "
                                                    + response.getContent()
                                    );

                                    loginButton.setDisable(false);
                                    closeClient(client);
                                    pendingClient = null;
                                }
                            })
                    );

                    client.setErrorListener(exception ->
                            Platform.runLater(() -> {

                                if (closing) {
                                    return;
                                }

                                if (client == tcpClient) {

                                    System.err.println(
                                            "TCP connection lost: "
                                                    + exception.getMessage()
                                    );

                                } else if (pendingClient == client) {

                                    messageLabel.setText(
                                            "Connection lost: "
                                                    + exception.getMessage()
                                    );

                                    loginButton.setDisable(false);
                                    pendingClient = null;
                                    closeClient(client);
                                }
                            })
                    );

                    client.startListening();

                    client.send(
                            new GameMessage(
                                    "LOGIN",
                                    "",
                                    "",
                                    username
                            )
                    );

                } catch (IOException exception) {

                    closeClient(client);

                    Platform.runLater(() -> {

                        if (closing || pendingClient != client) {
                            return;
                        }

                        pendingClient = null;

                        messageLabel.setText(
                                "Cannot connect to server: "
                                        + exception.getMessage()
                        );

                        loginButton.setDisable(false);
                    });
                }
            });

            loginThread.setName("wordarena-login-thread");
            loginThread.setDaemon(true);
            loginThread.start();
        });

        usernameField.setOnAction(event -> {
            if (!loginButton.isDisabled()) {
                loginButton.fire();
            }
        });

        VBox root = new VBox(
                20,
                title,
                subtitle,
                usernameField,
                loginButton,
                messageLabel
        );

        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));

        Scene scene = new Scene(root, 800, 500);
        applyTheme(scene);

        stage.setTitle("WordArena - Login");
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> closeApplication());
        stage.show();
    }

    private void applyTheme(Scene scene) {

        scene.getStylesheets().add(
                getClass()
                        .getResource("/css/wordarena.css")
                        .toExternalForm()
        );
    }

    private void closeClient(GameTcpClient client) {

        if (client == null) {
            return;
        }

        try {
            client.close();
        } catch (IOException exception) {

            System.err.println(
                    "Unable to close TCP connection: "
                            + exception.getMessage()
            );
        }
    }

    private void closeApplication() {

        if (closing) {
            return;
        }

        closing = true;

        if (primaryStage != null) {

            Scene scene = primaryStage.getScene();

            if (scene != null) {
                shutdownGameViews(scene.getRoot());
            }
        }

        if (gameSession != null) {
            gameSession.closeRmi();
            gameSession.closeJms();
        }

        closeClient(tcpClient);

        if (pendingClient != tcpClient) {
            closeClient(pendingClient);
        }
    }

    private void shutdownGameViews(Node node) {

        if (node instanceof GameView gameView) {
            gameView.shutdown();
            return;
        }

        if (node instanceof ScrollPane scrollPane) {

            Node content = scrollPane.getContent();

            if (content != null) {
                shutdownGameViews(content);
            }
        }

        if (node instanceof Parent parent) {

            for (Node child : parent.getChildrenUnmodifiable()) {
                shutdownGameViews(child);
            }
        }
    }

    @Override
    public void stop() {
        closeApplication();
    }

    public static void main(String[] args) {
        launch(args);
    }
}