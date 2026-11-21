
package com.wordarena.gui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;

public class EventFeedView extends VBox {

    private static final int MAX_EVENTS = 20;

    private final ListView<String> eventList = new ListView<>();

    public EventFeedView() {

        getStyleClass().add("event-feed-panel");
        setSpacing(15);
        setPadding(new Insets(20));
        setAlignment(Pos.CENTER);
        setPrefWidth(260);
        setMaxWidth(300);

        Label title = new Label("LIVE GAME EVENTS");
        title.getStyleClass().add("event-feed-title");

        Label subtitle = new Label("REAL-TIME ARENA ACTIVITY");
        subtitle.getStyleClass().add("event-feed-subtitle");

        eventList.getStyleClass().add("event-feed-list");
        eventList.setPrefHeight(250);
        eventList.setPrefWidth(240);
        eventList.setFocusTraversable(false);

        eventList.setPlaceholder(
                new Label("Waiting for events...")
        );

        getChildren().addAll(
                title,
                subtitle,
                eventList
        );
    }

    public void addEvent(String message) {

        if (Platform.isFxApplicationThread()) {
            insertEvent(message);
        } else {
            Platform.runLater(() -> insertEvent(message));
        }
    }

    private void insertEvent(String message) {

        eventList.getItems().add(0, message);

        if (eventList.getItems().size() > MAX_EVENTS) {
            eventList.getItems().remove(
                    MAX_EVENTS,
                    eventList.getItems().size()
            );
        }
    }

    public void clearEvents() {
        if (Platform.isFxApplicationThread()) {
            eventList.getItems().clear();
        } else {
            Platform.runLater(() -> eventList.getItems().clear());
        }
    }
}
