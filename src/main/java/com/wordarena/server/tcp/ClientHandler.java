package com.wordarena.server.tcp;

import com.wordarena.common.GameMessage;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private static final String LOGIN = "LOGIN";
    private static final String OK = "OK";
    private static final String ERROR = "ERROR";

    private final Socket clientSocket;
    private final PlayerSessionRegistry sessionRegistry;

    private ObjectOutputStream output;
    private ObjectInputStream input;
    private String authenticatedPlayerId;

    public ClientHandler(
            Socket clientSocket,
            PlayerSessionRegistry sessionRegistry
    ) {
        this.clientSocket = clientSocket;
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public void run() {
        try (
                clientSocket;
                ObjectOutputStream socketOutput =
                        new ObjectOutputStream(
                                clientSocket.getOutputStream()
                        )
        ) {
            output = socketOutput;
            output.flush();

            try (
                    ObjectInputStream socketInput =
                            new ObjectInputStream(
                                    clientSocket.getInputStream()
                            )
            ) {
                input = socketInput;

                System.out.println(
                        "Handling client on thread: "
                                + Thread.currentThread().getName()
                );

                processMessages();
            }
        } catch (EOFException exception) {
            System.out.println(
                    "Client closed the connection: "
                            + clientSocket.getRemoteSocketAddress()
            );
        } catch (IOException exception) {
            System.err.println(
                    "Client connection error: "
                            + exception.getMessage()
            );
        } finally {
            removeAuthenticatedSession();
        }
    }

    private void processMessages() throws IOException {
        while (!clientSocket.isClosed()) {
            try {
                Object receivedObject = input.readObject();

                if (!(receivedObject instanceof GameMessage message)) {
                    sendError(
                            "The server accepts only GameMessage objects."
                    );
                    continue;
                }

                processMessage(message);
            } catch (ClassNotFoundException exception) {
                sendError(
                        "The received object type is not supported."
                );
            }
        }
    }

    private void processMessage(GameMessage message)
            throws IOException {

        if (message.getType() == null
                || message.getType().isBlank()) {
            sendError("Message type is required.");
            return;
        }

        if (LOGIN.equals(message.getType())) {
            processLogin(message);
            return;
        }

        sendError(
                "Unsupported message type: "
                        + message.getType()
        );
    }

    private void processLogin(GameMessage message)
            throws IOException {

        if (authenticatedPlayerId != null) {
            sendError(
                    "This connection is already authenticated."
            );
            return;
        }

        PlayerSessionRegistry.RegistrationResult result =
                sessionRegistry.register(message.getContent());

        if (!result.successful()) {
            sendError(result.errorMessage());
            return;
        }

        PlayerSessionRegistry.PlayerSession session =
                result.session();

        authenticatedPlayerId = session.playerId();

        GameMessage response = new GameMessage(
                OK,
                session.playerId(),
                "",
                "Login successful. Welcome, "
                        + session.username()
                        + "."
        );

        sendMessage(response);

        System.out.println(
                "Player logged in: "
                        + session.username()
                        + " ["
                        + session.playerId()
                        + "]"
        );
    }

    private void sendError(String errorMessage)
            throws IOException {

        GameMessage response = new GameMessage(
                ERROR,
                "",
                "",
                errorMessage
        );

        sendMessage(response);
    }

    private void sendMessage(GameMessage message)
            throws IOException {

        output.writeObject(message);
        output.flush();
        output.reset();
    }

    private void removeAuthenticatedSession() {
        if (authenticatedPlayerId == null) {
            return;
        }

        boolean removed =
                sessionRegistry.remove(authenticatedPlayerId);

        if (removed) {
            System.out.println(
                    "Player session removed: "
                            + authenticatedPlayerId
            );
        }

        authenticatedPlayerId = null;
    }
}