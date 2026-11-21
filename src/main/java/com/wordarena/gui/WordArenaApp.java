
package com.wordarena.gui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class WordArenaApp extends Application {

    private Stage primaryStage;

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

            } else {

                LobbyView lobby = new LobbyView(username);

                Scene lobbyScene = new Scene(lobby, 800, 500);

                lobbyScene.getStylesheets().add(
                        getClass()
                                .getResource("/css/wordarena.css")
                                .toExternalForm()
                );

                stage.setScene(lobbyScene);
                stage.setTitle("WordArena - Lobby");
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

        scene.getStylesheets().add(
                getClass()
                        .getResource("/css/wordarena.css")
                        .toExternalForm()
        );

        stage.setTitle("WordArena - Login");
        stage.setScene(scene);

        stage.setOnCloseRequest(event -> closeApplication());

        stage.show();
    }

    private void closeApplication() {

        if (primaryStage == null) {
            return;
        }

        Scene scene = primaryStage.getScene();

        if (scene == null) {
            return;
        }

        shutdownGameViews(scene.getRoot());
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
