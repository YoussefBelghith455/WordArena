
package com.wordarena.jms;

import com.wordarena.common.GameEvent;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSConsumer;
import jakarta.jms.Topic;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

import java.util.function.Consumer;

public class EventSubscriber implements AutoCloseable {

    private final JMSContext context;
    private final JMSConsumer consumer;

    public EventSubscriber(String brokerUrl, String username,
                           String password, Consumer<GameEvent> onEvent) {

        ConnectionFactory factory = new ActiveMQConnectionFactory(
                brokerUrl, username, password
        );

        context = factory.createContext();

        Topic topic = context.createTopic("wordarena.events");

        consumer = context.createConsumer(topic);

        consumer.setMessageListener(message -> {
            try {
                GameEvent event = message.getBody(GameEvent.class);
                onEvent.accept(event);
            } catch (Exception e) {
                System.err.println("Error receiving JMS event: " + e.getMessage());
            }
        });
    }

    @Override
    public void close() {
        consumer.close();
        context.close();
    }
}
