
package com.wordarena.gui;

import com.wordarena.common.GameMode;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class GameHeaderView extends HBox {

    private final Label roomValue = new Label();
    private final Label modeValue = new Label();
    private final Label statusValue = new Label();
    private final Label connectionValue = new Label();

    public GameHeaderView(String roomName, GameMode gameMode) {

        getStyleClass().add("game-header");
        setAlignment(Pos.CENTER);
        setSpacing(20);
        setPadding(new Insets(18, 25, 18, 25));
        setMaxWidth(900);

        VBox roomSection = createSection("ROOM", roomValue);
        VBox modeSection = createSection("MODE", modeValue);
        VBox statusSection = createSection("STATUS", statusValue);
        VBox connectionSection = createSection("CONNECTION", connectionValue);

        HBox.setHgrow(roomSection, Priority.ALWAYS);
        HBox.setHgrow(modeSection, Priority.ALWAYS);
        HBox.setHgrow(statusSection, Priority.ALWAYS);
        HBox.setHgrow(connectionSection, Priority.ALWAYS);

        getChildren().addAll(
                roomSection,
                modeSection,
                statusSection,
                connectionSection
        );

        updateRoom(roomName);
        updateMode(gameMode);
        updateStatus("PLAYING");
        updateConnectionStatus("DISCONNECTED");
    }

    private VBox createSection(String title, Label value) {

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("game-header-label");

        value.getStyleClass().add("game-header-value");

        VBox section = new VBox(7, titleLabel, value);
        section.setAlignment(Pos.CENTER);

        return section;
    }

    public void updateRoom(String roomName) {
        roomValue.setText(roomName);
    }

    public void updateMode(GameMode gameMode) {
        modeValue.setText(gameMode.toString());
    }

    public void updateStatus(String status) {

        statusValue.setText(status);

        statusValue.getStyleClass().removeAll(
                "game-status-playing",
                "game-status-waiting",
                "game-status-finished"
        );

        switch (status.toUpperCase()) {

            case "PLAYING" ->
                    statusValue.getStyleClass().add("game-status-playing");

            case "WAITING" ->
                    statusValue.getStyleClass().add("game-status-waiting");

            case "FINISHED" ->
                    statusValue.getStyleClass().add("game-status-finished");

            default -> {
            }
        }
    }

    public void updateConnectionStatus(String status) {

        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> updateConnectionStatus(status));
            return;
        }

        String normalizedStatus = status.toUpperCase();

        connectionValue.setText(normalizedStatus);

        connectionValue.getStyleClass().removeAll(
                "connection-connected",
                "connection-disconnected",
                "connection-error"
        );

        switch (normalizedStatus) {

            case "CONNECTED" ->
                    connectionValue.getStyleClass().add("connection-connected");

            case "DISCONNECTED" ->
                    connectionValue.getStyleClass().add("connection-disconnected");

            case "ERROR" ->
                    connectionValue.getStyleClass().add("connection-error");

            default -> {
            }
        }
    }
}
