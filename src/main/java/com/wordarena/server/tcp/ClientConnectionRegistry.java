package com.wordarena.server.tcp;

import com.wordarena.common.GameMessage;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientConnectionRegistry {

    private final Map<String, ClientHandler> handlersByPlayerId;

    public ClientConnectionRegistry() {
        this.handlersByPlayerId = new ConcurrentHashMap<>();
    }

    public void register(
            String playerId,
            ClientHandler clientHandler
    ) {
        if (playerId == null || playerId.isBlank()) {
            throw new IllegalArgumentException(
                    "Player ID is required."
            );
        }

        if (clientHandler == null) {
            throw new IllegalArgumentException(
                    "Client handler is required."
            );
        }

        ClientHandler existingHandler =
                handlersByPlayerId.putIfAbsent(
                        playerId,
                        clientHandler
                );

        if (existingHandler != null) {
            throw new IllegalStateException(
                    "This player already has an active connection."
            );
        }
    }

    public void remove(
            String playerId,
            ClientHandler clientHandler
    ) {
        if (playerId == null || clientHandler == null) {
            return;
        }

        handlersByPlayerId.remove(
                playerId,
                clientHandler
        );
    }

    public void sendToPlayer(
            String playerId,
            GameMessage message
    ) {
        ClientHandler clientHandler =
                handlersByPlayerId.get(playerId);

        if (clientHandler == null) {
            return;
        }

        try {
            clientHandler.sendServerMessage(message);
        } catch (IOException exception) {
            System.err.println(
                    "Unable to send a message to player "
                            + playerId
                            + ": "
                            + exception.getMessage()
            );
        }
    }

    public int getConnectedClientCount() {
        return handlersByPlayerId.size();
    }
}