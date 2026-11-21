package com.wordarena.server.tcp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameServer {

    public static final int DEFAULT_PORT = 5000;

    private final int port;
    private final ExecutorService clientExecutor;
    private final PlayerSessionRegistry sessionRegistry;

    public GameServer(int port) {
        this.port = port;
        this.clientExecutor = Executors.newCachedThreadPool();
        this.sessionRegistry = new PlayerSessionRegistry();
    }

    public void start() {
        System.out.println("Starting WordArena TCP server...");

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println(
                    "WordArena TCP server is listening on port " + port
            );

            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();

                System.out.println(
                        "Client connected: "
                                + clientSocket.getRemoteSocketAddress()
                );

                clientExecutor.submit(
                        new ClientHandler(
                                clientSocket,
                                sessionRegistry
                        )
                );
            }
        } catch (IOException exception) {
            System.err.println(
                    "TCP server error: " + exception.getMessage()
            );
        } finally {
            clientExecutor.shutdown();
            System.out.println("WordArena TCP server stopped.");
        }
    }

    public static void main(String[] args) {
        GameServer server = new GameServer(DEFAULT_PORT);
        server.start();
    }
}