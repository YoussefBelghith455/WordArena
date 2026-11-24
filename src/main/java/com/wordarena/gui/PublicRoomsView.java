package com.wordarena.gui;

import com.wordarena.client.tcp.GameSession;
import com.wordarena.common.RoomInfo;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Objects;

public class PublicRoomsView extends VBox {

    private final GameSession gameSession;
    private final ListView<RoomInfo> roomsList;

    private final ObservableList<RoomInfo> displayedRooms =
            FXCollections.observableArrayList();

    private final ReadOnlyObjectWrapper<RoomInfo> selectedRoom =
            new ReadOnlyObjectWrapper<>();

    private boolean refreshingRooms;

    private final ListChangeListener<RoomInfo> roomsListener =
            change -> updateRooms();

    public PublicRoomsView(GameSession gameSession) {

        this.gameSession = gameSession;

        setSpacing(10);
        setPadding(new Insets(12));
        setMaxWidth(600);
        setAlignment(Pos.CENTER);

        Label title = new Label("AVAILABLE ROOMS");
        title.getStyleClass().add("section-title");

        Button refreshButton = new Button("REFRESH");
        refreshButton.getStyleClass().add("neon-button");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(
                10,
                title,
                spacer,
                refreshButton
        );

        header.setAlignment(Pos.CENTER_LEFT);

        Label placeholder = new Label(
                "No public rooms available."
        );

        placeholder.setWrapText(true);
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setStyle(
                "-fx-text-fill: #A5A0BC;"
        );

        roomsList = new ListView<>(displayedRooms);

        roomsList.setPrefHeight(180);
        roomsList.setMinHeight(120);
        roomsList.setMaxWidth(Double.MAX_VALUE);

        roomsList.setStyle(
                "-fx-background-color: #151020;"
                        + "-fx-border-color: #50396D;"
                        + "-fx-border-radius: 8;"
                        + "-fx-background-radius: 8;"
        );

        roomsList.setPlaceholder(placeholder);

        roomsList.setCellFactory(
                list -> new RoomCell()
        );

        roomsList.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldRoom, newRoom) -> {

                    if (!refreshingRooms) {
                        selectedRoom.set(newRoom);
                    }
                });

        Label refreshLabel = new Label(
                "Automatically refreshed every 5 seconds"
        );

        refreshLabel.setStyle(
                "-fx-text-fill: #A5A0BC;"
                        + "-fx-font-size: 11px;"
        );

        if (gameSession != null) {

            placeholder.textProperty().bind(
                    Bindings.createStringBinding(
                            () -> switch (
                                    gameSession.getRmiStatus()
                                    ) {

                                case "CONNECTED" ->
                                        "No public rooms available.";

                                case "CONNECTING" ->
                                        "Connecting to RMI service...";

                                default ->
                                        "RMI service unavailable.";
                            },
                            gameSession.rmiStatusProperty()
                    )
            );

            refreshButton.setOnAction(
                    event -> gameSession.refreshRooms()
            );

            sceneProperty().addListener(
                    (observable, oldScene, newScene) -> {

                        if (oldScene != null) {

                            gameSession.getAvailableRooms()
                                    .removeListener(roomsListener);
                        }

                        if (newScene != null) {

                            gameSession.getAvailableRooms()
                                    .addListener(roomsListener);

                            updateRooms();

                            gameSession.refreshRooms();

                        } else {

                            displayedRooms.clear();

                            roomsList.getSelectionModel()
                                    .clearSelection();

                            selectedRoom.set(null);
                        }
                    }
            );

        } else {

            refreshButton.setDisable(true);

            placeholder.setText(
                    "RMI service unavailable."
            );
        }

        getChildren().addAll(
                header,
                roomsList,
                refreshLabel
        );
    }

    private void updateRooms() {

        if (gameSession == null) {
            return;
        }

        RoomInfo previousSelection = selectedRoom.get();

        String selectedRoomId =
                previousSelection == null
                        ? null
                        : previousSelection.getRoomId();

        refreshingRooms = true;

        try {

            displayedRooms.setAll(
                    gameSession.getAvailableRooms()
            );

            roomsList.getSelectionModel()
                    .clearSelection();

            if (selectedRoomId != null) {

                for (int index = 0;
                     index < displayedRooms.size();
                     index++) {

                    RoomInfo room =
                            displayedRooms.get(index);

                    if (Objects.equals(
                            selectedRoomId,
                            room.getRoomId()
                    )) {

                        roomsList.getSelectionModel()
                                .select(index);

                        break;
                    }
                }
            }

        } finally {

            refreshingRooms = false;

            selectedRoom.set(
                    roomsList.getSelectionModel()
                            .getSelectedItem()
            );
        }
    }

    public RoomInfo getSelectedRoom() {

        return selectedRoom.get();
    }

    public ReadOnlyObjectProperty<RoomInfo>
    selectedRoomProperty() {

        return selectedRoom.getReadOnlyProperty();
    }

    public boolean isSelectedRoomJoinable() {

        RoomInfo room = getSelectedRoom();

        return room != null
                && !room.isStarted()
                && room.getCurrentPlayers()
                < room.getMaxPlayers();
    }

    private static class RoomCell
            extends ListCell<RoomInfo> {

        private RoomCell() {

            selectedProperty().addListener(
                    (observable, oldValue, newValue) ->
                            updateSelectionStyle()
            );
        }

        private void updateSelectionStyle() {

            if (isEmpty() || getItem() == null) {

                setStyle(
                        "-fx-background-color: transparent;"
                );

                return;
            }

            setStyle(
                    isSelected()
                            ? "-fx-background-color: #6338B0;"
                              + "-fx-background-radius: 8;"
                              + "-fx-padding: 4;"
                            : "-fx-background-color: transparent;"
                              + "-fx-padding: 4;"
            );
        }

        @Override
        protected void updateItem(
                RoomInfo room,
                boolean empty
        ) {

            super.updateItem(room, empty);

            setText(null);
            setGraphic(null);

            if (empty || room == null) {

                updateSelectionStyle();

                return;
            }

            Label roomName = new Label(
                    room.getRoomName()
            );

            roomName.setStyle(
                    "-fx-text-fill: #DDD6FE;"
                            + "-fx-font-size: 14px;"
                            + "-fx-font-weight: bold;"
            );

            Label mode = new Label(
                    String.valueOf(room.getGameMode())
            );

            mode.setStyle(
                    "-fx-text-fill: #C4B5FD;"
                            + "-fx-font-size: 12px;"
            );

            Label roomId = new Label(
                    "Room ID: " + room.getRoomId()
            );

            roomId.setStyle(
                    "-fx-text-fill: #A5A0BC;"
                            + "-fx-font-size: 11px;"
            );

            Label players = new Label(
                    room.getCurrentPlayers()
                            + "/"
                            + room.getMaxPlayers()
                            + " PLAYERS"
            );

            players.setStyle(
                    "-fx-text-fill: #C4B5FD;"
                            + "-fx-font-size: 12px;"
            );

            boolean available =
                    !room.isStarted()
                            && room.getCurrentPlayers()
                            < room.getMaxPlayers();

            String statusText;

            if (room.isStarted()) {

                statusText = "IN GAME";

            } else if (!available) {

                statusText = "FULL";

            } else {

                statusText = "OPEN";
            }

            Label status = new Label(statusText);

            status.setStyle(
                    available
                            ? "-fx-text-fill: #4ADE80;"
                            : "-fx-text-fill: #F87171;"
            );

            VBox information = new VBox(
                    5,
                    roomName,
                    mode,
                    roomId
            );

            VBox details = new VBox(
                    5,
                    status,
                    players
            );

            details.setAlignment(Pos.CENTER_RIGHT);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox roomCard = new HBox(
                    10,
                    information,
                    spacer,
                    details
            );

            roomCard.setAlignment(Pos.CENTER_LEFT);
            roomCard.setPadding(new Insets(10));

            roomCard.setStyle(
                    "-fx-background-color: #20172F;"
                            + "-fx-background-radius: 6;"
                            + "-fx-border-color: #50396D;"
                            + "-fx-border-radius: 6;"
            );

            setGraphic(roomCard);

            updateSelectionStyle();
        }
    }
}