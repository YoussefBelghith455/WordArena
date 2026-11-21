
package com.wordarena.jms;

import com.wordarena.common.EventType;
import com.wordarena.common.GameEvent;

public class JmsTest {

    public static void main(String[] args) throws Exception {

        String brokerUrl = "tcp://localhost:61616";
        String username = System.getenv("WORDARENA_JMS_USER");
        String password = System.getenv("WORDARENA_JMS_PASSWORD");

        if (username == null || password == null) {
            throw new IllegalStateException(
                    "Configure WORDARENA_JMS_USER and WORDARENA_JMS_PASSWORD"
            );
        }

        try (EventSubscriber subscriber = new EventSubscriber(
                brokerUrl,
                username,
                password,
                event -> System.out.println(
                        "RECEIVED: " + event.getType()
                                + " - " + event.getMessage()
                )
        )) {

            Thread.sleep(1000);

            EventPublisher publisher = new EventPublisher(
                    brokerUrl, username, password
            );

            GameEvent event = new GameEvent(
                    EventType.PLAYER_JOINED,
                    "room1",
                    "player1",
                    "Player joined the room"
            );

            publisher.publish(event);

            System.out.println("EVENT SENT");

            Thread.sleep(2000);
        }
    }
}
