package com.wordarena.server.jms;

import com.wordarena.common.EventType;
import com.wordarena.common.GameEvent;
import com.wordarena.jms.EventPublisher;

import java.util.Objects;

public class ServerEventPublisher {

    public static final String DEFAULT_BROKER_URL =
            "tcp://localhost:61616";

    private static final String JMS_USER_ENV =
            "WORDARENA_JMS_USER";

    private static final String JMS_PASSWORD_ENV =
            "WORDARENA_JMS_PASSWORD";

    private final EventPublisher eventPublisher;
    private final boolean enabled;

    public ServerEventPublisher() {
        this(
                DEFAULT_BROKER_URL,
                System.getenv(JMS_USER_ENV),
                System.getenv(JMS_PASSWORD_ENV)
        );
    }

    public ServerEventPublisher(
            String brokerUrl,
            String username,
            String password
    ) {
        String validatedBrokerUrl = requireText(
                brokerUrl,
                "JMS broker URL"
        );

        if (username == null
                || username.isBlank()
                || password == null
                || password.isBlank()) {

            this.eventPublisher = null;
            this.enabled = false;

            System.out.println(
                    "JMS event publication is disabled because "
                            + JMS_USER_ENV
                            + " or "
                            + JMS_PASSWORD_ENV
                            + " is not configured."
            );

            return;
        }

        this.eventPublisher = new EventPublisher(
                validatedBrokerUrl,
                username,
                password
        );

        this.enabled = true;

        System.out.println(
                "JMS event publication is configured for "
                        + validatedBrokerUrl
        );
    }

    public void publish(
            EventType eventType,
            String roomId,
            String playerId,
            String publicMessage
    ) {
        Objects.requireNonNull(
                eventType,
                "Event type is required."
        );

        String validatedRoomId = requireText(
                roomId,
                "Room ID"
        );

        String safePlayerId =
                playerId == null ? "" : playerId.trim();

        String validatedPublicMessage = requireText(
                publicMessage,
                "Public event message"
        );

        if (!enabled) {
            System.out.println(
                    "JMS event skipped because publication "
                            + "is disabled: "
                            + eventType
                            + " in room "
                            + validatedRoomId
            );

            return;
        }

        GameEvent gameEvent = new GameEvent(
                eventType,
                validatedRoomId,
                safePlayerId,
                validatedPublicMessage
        );

        try {
            eventPublisher.publish(gameEvent);
        } catch (RuntimeException exception) {
            /*
             * JMS notifications must not terminate TCP gameplay.
             * TCP remains the authoritative state channel.
             */
            System.err.println(
                    "Unable to publish JMS event "
                            + eventType
                            + " for room "
                            + validatedRoomId
                            + ": "
                            + exception.getMessage()
            );
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    private static String requireText(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " is required."
            );
        }

        return value.trim();
    }
}