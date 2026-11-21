
package com.wordarena.jms;

import com.wordarena.common.GameEvent;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.Topic;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

public class EventPublisher {

    private final ConnectionFactory connectionFactory;

    public EventPublisher(String brokerUrl, String username, String password) {
        connectionFactory = new ActiveMQConnectionFactory(
                brokerUrl, username, password
        );
    }

    public void publish(GameEvent event) {
        try (JMSContext context = connectionFactory.createContext()) {
            Topic topic = context.createTopic("wordarena.events");
            context.createProducer().send(topic, event);
        }
    }
}
