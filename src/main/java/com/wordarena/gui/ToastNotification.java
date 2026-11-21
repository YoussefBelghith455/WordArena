
package com.wordarena.gui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.scene.layout.Region;

public class ToastNotification extends VBox {

    public ToastNotification(String message) {

        getStyleClass().add("toast-notification");
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(15));
        setSpacing(5);
        setMinWidth(300);
        setPrefWidth(300);
        setMaxWidth(300);

        setMinHeight(Region.USE_PREF_SIZE);
        setPrefHeight(Region.USE_COMPUTED_SIZE);
        setMaxHeight(Region.USE_PREF_SIZE);

        setMouseTransparent(true);

        Label title = new Label("WORDARENA");
        title.getStyleClass().add("toast-title");

        Label content = new Label(message);
        content.getStyleClass().add("toast-message");
        content.setWrapText(true);

        getChildren().addAll(title, content);

        setOpacity(0);
        setTranslateX(60);

        FadeTransition fadeIn = new FadeTransition(
                Duration.millis(300), this
        );
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        TranslateTransition slideIn = new TranslateTransition(
                Duration.millis(300), this
        );
        slideIn.setFromX(60);
        slideIn.setToX(0);

        PauseTransition wait = new PauseTransition(
                Duration.seconds(3)
        );

        FadeTransition fadeOut = new FadeTransition(
                Duration.millis(500), this
        );
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        SequentialTransition animation = new SequentialTransition(
                fadeIn,
                wait,
                fadeOut
        );

        animation.setOnFinished(event -> {
            if (getParent() instanceof javafx.scene.layout.Pane parent) {
                parent.getChildren().remove(this);
            }
        });

        slideIn.play();
        animation.play();
    }
}
