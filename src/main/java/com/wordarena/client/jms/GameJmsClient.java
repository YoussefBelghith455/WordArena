
package com.wordarena.client.jms;

import com.wordarena.common.GameEvent;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSConsumer;
import jakarta.jms.Topic;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class GameJmsClient implements AutoCloseable {

    private static final String BROKER_URL =
            "tcp://localhost:61616";

    private static final String TOPIC_NAME =
            "wordarena.events";

    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor(task -> {
                Thread thread = new Thread(
                        task,
                        "wordarena-jms-thread"
                );
                thread.setDaemon(true);
                return thread;
            });

    private final ReadOnlyStringWrapper status =
            new ReadOnlyStringWrapper("DISCONNECTED");

    private final AtomicBoolean connected =
            new AtomicBoolean(false);

    private final AtomicBoolean connectionFailed =
            new AtomicBoolean(false);

    private final AtomicBoolean started =
            new AtomicBoolean(false);

    private final AtomicBoolean closed =
            new AtomicBoolean(false);

    private final Consumer<GameEvent> eventHandler;

    private ActiveMQConnectionFactory factory;
    private JMSContext context;
    private JMSConsumer consumer;

    public GameJmsClient(Consumer<GameEvent> eventHandler) {
        this.eventHandler = eventHandler;
    }

    public void start() {

        if (closed.get() || !started.compareAndSet(false, true)) {
            return;
        }

        executor.scheduleWithFixedDelay(
                this::maintainConnection,
                0,
                5,
                TimeUnit.SECONDS
        );
    }

    private void maintainConnection() {

        if (closed.get()) {
            return;
        }

        if (connectionFailed.getAndSet(false)) {
            closeResources();
        }

        if (!connected.get()) {
            connect();
        }
    }

    private void connect() {

        if (closed.get()) {
            return;
        }

        String username =
                System.getenv("WORDARENA_JMS_USER");

        String password =
                System.getenv("WORDARENA_JMS_PASSWORD");

        if (username == null || username.isBlank()
                || password == null || password.isBlank()) {

            updateStatus("NOT CONFIGURED");
            return;
        }

        updateStatus("CONNECTING");

        try {

            factory = new ActiveMQConnectionFactory(
                    BROKER_URL,
                    username,
                    password
            );

            factory.setInitialConnectAttempts(1);
            factory.setReconnectAttempts(0);
            factory.setCallTimeout(3000);
            factory.setClientFailureCheckPeriod(3000);
            factory.setConnectionTTL(6000);

            context = factory.createContext();

            context.setExceptionListener(exception -> {

                connected.set(false);
                connectionFailed.set(true);

                updateStatus("DISCONNECTED");
            });

            Topic topic = context.createTopic(TOPIC_NAME);

            consumer = context.createConsumer(topic);

            consumer.setMessageListener(message -> {

                try {

                    GameEvent event =
                            message.getBody(GameEvent.class);

                    if (event != null && !closed.get()) {

                        Platform.runLater(() -> {

                            if (!closed.get()
                                    && eventHandler != null) {

                                eventHandler.accept(event);
                            }
                        });
                    }

                } catch (Exception exception) {

                    System.err.println(
                            "Unable to process JMS event."
                    );
                }
            });

            if (closed.get() || connectionFailed.get()) {
                closeResources();
                return;
            }

            connected.set(true);
            updateStatus("CONNECTED");

        } catch (Exception exception) {

            closeResources();
            updateStatus("DISCONNECTED");
        }
    }

    private void updateStatus(String value) {

        if (closed.get()) {
            return;
        }

        Platform.runLater(() -> {

            if (!closed.get()) {
                status.set(value);
            }
        });
    }

    public ReadOnlyStringProperty statusProperty() {
        return status.getReadOnlyProperty();
    }

    public String getStatus() {
        return status.get();
    }

    public boolean isConnected() {
        return connected.get();
    }

    private void closeResources() {

        connected.set(false);

        if (consumer != null) {

            try {
                consumer.close();
            } catch (Exception ignored) {
            }

            consumer = null;
        }

        if (context != null) {

            try {
                context.close();
            } catch (Exception ignored) {
            }

            context = null;
        }

        if (factory != null) {

            try {
                factory.close();
            } catch (Exception ignored) {
            }

            factory = null;
        }
    }

    @Override
    public void close() {

        if (!closed.compareAndSet(false, true)) {
            return;
        }

        connected.set(false);

        executor.execute(this::closeResources);
        executor.shutdown();
    }
}
