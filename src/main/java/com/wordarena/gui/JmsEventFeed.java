
package com.wordarena.gui;

import com.wordarena.client.tcp.GameSession;
import com.wordarena.common.GameEvent;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class JmsEventFeed extends VBox {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    private final ListView<GameEvent> eventList =
            new ListView<>();

    public JmsEventFeed(GameSession gameSession) {

        setSpacing(10);
        setPadding(new Insets(15));
        setAlignment(Pos.TOP_LEFT);

        setPrefWidth(300);
        setMinWidth(240);
        setMaxWidth(340);

        setStyle(
                "-fx-background-color: rgba(20, 15, 40, 0.90);" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #7C3AED;" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-width: 1;"
        );

        Label title = new Label("LIVE ROOM EVENTS");

        title.setStyle(
                "-fx-text-fill: #C084FC;" +
                        "-fx-font-size: 16px;" +
                        "-fx-font-weight: bold;"
        );

        Label connectionLabel =
                new Label("JMS: DISCONNECTED");

        connectionLabel.setStyle(
                getStatusStyle("DISCONNECTED")
        );

        eventList.setPrefHeight(180);
        eventList.setMinHeight(120);

        eventList.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-control-inner-background: #141028;" +
                        "-fx-border-color: transparent;"
        );

        Label placeholder =
                new Label("Waiting for room events...");

        placeholder.setStyle(
                "-fx-text-fill: #A78BFA;"
        );

        eventList.setPlaceholder(placeholder);

        eventList.setCellFactory(list -> new ListCell<>() {

            @Override
            protected void updateItem(
                    GameEvent event,
                    boolean empty
            ) {
                super.updateItem(event, empty);

                setText(null);
                setGraphic(null);

                setStyle(
                        "-fx-background-color: transparent;" +
                                "-fx-padding: 8;"
                );

                if (empty || event == null) {
                    return;
                }

                Label typeLabel = new Label(
                        event.getType() == null
                                ? "EVENT"
                                : event.getType()
                                .name()
                                .replace('_', ' ')
                );

                typeLabel.setStyle(
                        "-fx-font-size: 11px;" +
                                "-fx-font-weight: bold;" +
                                "-fx-text-fill: "
                                + getEventColor(event)
                                + ";"
                );

                Label messageLabel = new Label(
                        event.getMessage() == null
                                ? ""
                                : event.getMessage()
                );

                messageLabel.setWrapText(true);

                messageLabel.maxWidthProperty().bind(
                        Bindings.max(
                                80,
                                widthProperty().subtract(25)
                        )
                );

                messageLabel.setStyle(
                        "-fx-text-fill: #E9D5FF;" +
                                "-fx-font-size: 12px;"
                );

                Label timeLabel = new Label(
                        TIME_FORMAT.format(
                                Instant.ofEpochMilli(
                                        event.getTimestamp()
                                )
                        )
                );

                timeLabel.setStyle(
                        "-fx-text-fill: #94A3B8;" +
                                "-fx-font-size: 10px;"
                );

                VBox eventBox = new VBox(
                        4,
                        typeLabel,
                        messageLabel,
                        timeLabel
                );

                setGraphic(eventBox);
            }
        });

        if (gameSession != null) {

            connectionLabel.textProperty().bind(
                    Bindings.concat(
                            "JMS: ",
                            gameSession.jmsStatusProperty()
                    )
            );

            connectionLabel.styleProperty().bind(
                    Bindings.createStringBinding(
                            () -> getStatusStyle(
                                    gameSession.getJmsStatus()
                            ),
                            gameSession.jmsStatusProperty()
                    )
            );

            sceneProperty().addListener(
                    (observable, oldScene, newScene) -> {

                        if (newScene == null) {

                            eventList.setItems(
                                    javafx.collections.FXCollections
                                            .observableArrayList()
                            );

                        } else {

                            eventList.setItems(
                                    gameSession.getJmsEvents()
                            );

                            if (!eventList.getItems().isEmpty()) {

                                eventList.scrollTo(
                                        eventList.getItems().size() - 1
                                );
                            }
                        }
                    }
            );
        }

        getChildren().addAll(
                title,
                connectionLabel,
                eventList
        );
    }

    private String getEventColor(GameEvent event) {

        if (event.getType() == null) {
            return "#E9D5FF";
        }

        return switch (event.getType().name()) {

            case "PLAYER_JOINED" -> "#4ADE80";

            case "PLAYER_LEFT" -> "#F87171";

            case "ROUND_STARTED" -> "#60A5FA";

            case "PLAYER_SOLVED" -> "#FACC15";

            case "SCORE_UPDATED" -> "#C084FC";

            case "ROUND_ENDED" -> "#FB923C";

            default -> "#E9D5FF";
        };
    }

    private String getStatusStyle(String status) {

        String color = switch (status) {

            case "CONNECTED" -> "#4ADE80";

            case "CONNECTING" -> "#FACC15";

            case "NOT CONFIGURED" -> "#FB923C";

            default -> "#F87171";
        };

        return "-fx-text-fill: " + color + ";" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;";
    }
}
