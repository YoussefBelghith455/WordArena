package com.wordarena.server.tcp;

import com.wordarena.server.game.DuelGameService;
import com.wordarena.server.game.RoomManager;
import com.wordarena.server.game.WordBank;
import com.wordarena.server.jms.GameEventNotifier;
import com.wordarena.server.jms.ServerEventPublisher;

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
    private final RoomManager roomManager;
    private final ClientConnectionRegistry connectionRegistry;
    private final DuelGameService duelGameService;
    private final ServerEventPublisher serverEventPublisher;
    private final GameEventNotifier gameEventNotifier;

    public GameServer(int port) {
        this.port = port;
        this.clientExecutor = Executors.newCachedThreadPool();
        this.sessionRegistry = new PlayerSessionRegistry();
        this.roomManager = new RoomManager();
        this.connectionRegistry =
                new ClientConnectionRegistry();

        WordBank wordBank = new WordBank();

        this.duelGameService = new DuelGameService(
                roomManager,
                wordBank
        );

        this.serverEventPublisher =
                new ServerEventPublisher();

        this.gameEventNotifier =
                new GameEventNotifier(
                        serverEventPublisher
                );
    }

    public void start() {
        System.out.println(
                "Starting WordArena TCP server..."
        );

        System.out.println(
                "JMS publication enabled: "
                        + gameEventNotifier.isEnabled()
        );

        try (
                ServerSocket serverSocket =
                        new ServerSocket(port)
        ) {
            System.out.println(
                    "WordArena TCP server is listening on port "
                            + port
            );

            while (!serverSocket.isClosed()) {
                Socket clientSocket =
                        serverSocket.accept();

                System.out.println(
                        "Client connected: "
                                + clientSocket
                                .getRemoteSocketAddress()
                );

                clientExecutor.submit(
                        new ClientHandler(
                                clientSocket,
                                sessionRegistry,
                                roomManager,
                                connectionRegistry,
                                duelGameService,
                                gameEventNotifier
                        )
                );
            }
        } catch (IOException exception) {
            System.err.println(
                    "TCP server error: "
                            + exception.getMessage()
            );
        } finally {
            clientExecutor.shutdown();

            System.out.println(
                    "WordArena TCP server stopped."
            );
        }
    }

    public static void main(String[] args) {
        GameServer server =
                new GameServer(DEFAULT_PORT);

        server.start();
    }
}