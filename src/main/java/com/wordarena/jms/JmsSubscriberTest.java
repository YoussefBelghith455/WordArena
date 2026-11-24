
package com.wordarena.jms;

import java.util.concurrent.CountDownLatch;
import java.time.LocalTime;

public class JmsSubscriberTest {

    public static void main(String[] args) throws Exception {

        String brokerUrl = "tcp://localhost:61616";
        String username = System.getenv("WORDARENA_JMS_USER");
        String password = System.getenv("WORDARENA_JMS_PASSWORD");

        if (username == null || password == null
                || username.isBlank() || password.isBlank()) {
            System.err.println("JMS credentials are not configured.");
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);

        try (EventSubscriber subscriber = new EventSubscriber(
                brokerUrl,
                username,
                password,
                event -> {
                    System.out.println(
                            "[" + LocalTime.now().withNano(0) + "] "
                                    + event.getType()
                                    + " | " + event.getMessage()
                    );
                }
        )) {
            System.out.println("================================");
            System.out.println("WordArena JMS Subscriber");
            System.out.println("Broker: " + brokerUrl);
            System.out.println("Topic: wordarena.events");
            System.out.println("Listening for events...");
            System.out.println("================================");

            latch.await();
        }
    }
}
