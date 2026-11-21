
package com.wordarena.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LobbyView extends VBox {

    public LobbyView(String username) {

        getStyleClass().add("lobby-root");
        setAlignment(Pos.CENTER);
        setSpacing(25);
        setPadding(new Insets(40));

        Label title = new Label("WORDARENA");
        title.getStyleClass().add("lobby-title");

        Label subtitle = new Label("MULTIPLAYER WORD BATTLE");
        subtitle.getStyleClass().add("lobby-subtitle");

        Label welcome = new Label("Welcome back, " + username + "!");
        welcome.getStyleClass().add("lobby-welcome");

        Label roomsLabel = new Label("AVAILABLE ROOMS");
        roomsLabel.getStyleClass().add("section-title");

        Label room1 = new Label("DUEL  |  Room 1  |  1/2 Players");
        Label room2 = new Label("COOPERATIVE  |  Room 2  |  2/4 Players");

        room1.getStyleClass().add("room-card");
        room2.getStyleClass().add("room-card");

        VBox roomsBox = new VBox(12, room1, room2);
        roomsBox.setAlignment(Pos.CENTER);
        roomsBox.setMaxWidth(460);

        Button createButton = new Button("CREATE ROOM");
        Button joinButton = new Button("JOIN ROOM");
        Button testGameButton = new Button("TEST GAME");

        createButton.getStyleClass().add("neon-button");
        joinButton.getStyleClass().add("neon-button");
        testGameButton.getStyleClass().add("secondary-button");

        createButton.setPrefWidth(190);
        joinButton.setPrefWidth(190);
        testGameButton.setPrefWidth(390);

        HBox actions = new HBox(15, createButton, joinButton);
        actions.setAlignment(Pos.CENTER);

        VBox header = new VBox(10, title, subtitle, welcome);
        header.setAlignment(Pos.CENTER);

        VBox roomSection = new VBox(15, roomsLabel, roomsBox);
        roomSection.setAlignment(Pos.CENTER);

        getChildren().addAll(
                header,
                roomSection,
                actions,
                testGameButton
        );

        createButton.setOnAction(event -> {

            CreateRoomView view = new CreateRoomView(username);

            Scene scene = new Scene(view, 800, 500);
            applyTheme(scene);

            Stage stage = (Stage) createButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("WordArena - Create Room");
        });

        joinButton.setOnAction(event -> {

            JoinRoomView view = new JoinRoomView(username);

            Scene scene = new Scene(view, 800, 500);
            applyTheme(scene);

            Stage stage = (Stage) joinButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("WordArena - Join Room");
        });

        testGameButton.setOnAction(event -> {

            GameView gameView = new GameView(username);

            ScrollPane scrollPane = new ScrollPane(gameView);
            scrollPane.setFitToWidth(true);

            StackPane overlay = new StackPane(scrollPane);

            Scene scene = new Scene(overlay, 800, 800);
            applyTheme(scene);

            Stage stage = (Stage) testGameButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("WordArena - Game");
        });
    }

    private void applyTheme(Scene scene) {
        scene.getStylesheets().add(
                getClass().getResource("/css/wordarena.css").toExternalForm()
        );
    }
}
