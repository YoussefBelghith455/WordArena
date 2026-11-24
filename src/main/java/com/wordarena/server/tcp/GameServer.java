package com.wordarena.server.tcp;

import com.wordarena.server.game.CooperativeGameService;
import com.wordarena.server.game.DuelGameService;
import com.wordarena.server.game.HostGameService;
import com.wordarena.server.game.RoomManager;
import com.wordarena.server.game.WordBank;
import com.wordarena.server.jms.GameEventNotifier;
import com.wordarena.server.jms.ServerEventPublisher;
import com.wordarena.server.rmi.GameStatisticsService;
import com.wordarena.server.rmi.PlayerStatisticsRepository;
import com.wordarena.server.rmi.RmiServiceManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
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
    private final CooperativeGameService cooperativeGameService;
    private final HostGameService hostGameService;

    private final GameEventNotifier gameEventNotifier;
    private final RmiServiceManager rmiServiceManager;

    public GameServer(int port) {
        this.port = port;

        this.clientExecutor =
                Executors.newCachedThreadPool();

        this.sessionRegistry =
                new PlayerSessionRegistry();

        this.roomManager =
                new RoomManager();

        this.connectionRegistry =
                new ClientConnectionRegistry();

        PlayerStatisticsRepository statisticsRepository =
                new PlayerStatisticsRepository();

        GameStatisticsService statisticsService =
                new GameStatisticsService(
                        statisticsRepository
                );

        WordBank wordBank =
                new WordBank();

        this.duelGameService =
                new DuelGameService(
                        roomManager,
                        wordBank,
                        statisticsService
                );

        this.cooperativeGameService =
                new CooperativeGameService(
                        roomManager,
                        wordBank,
                        statisticsService
                );

        this.hostGameService =
                new HostGameService(
                        roomManager,
                        statisticsService
                );

        this.gameEventNotifier =
                new GameEventNotifier(
                        new ServerEventPublisher()
                );

        this.rmiServiceManager =
                new RmiServiceManager(
                        roomManager,
                        statisticsRepository
                );
    }

    public void start() {
        System.out.println(
                "Starting WordArena server..."
        );

        System.out.println(
                "JMS publication enabled: "
                        + gameEventNotifier.isEnabled()
        );

        try {
            rmiServiceManager.start();
        } catch (RemoteException exception) {
            System.err.println(
                    "Unable to start the RMI service: "
                            + exception.getMessage()
            );

            return;
        }

        Runtime.getRuntime().addShutdownHook(
                new Thread(
                        rmiServiceManager::stop,
                        "wordarena-rmi-shutdown"
                )
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

                clientExecutor.submit(
                        new ClientHandler(
                                clientSocket,
                                sessionRegistry,
                                roomManager,
                                connectionRegistry,
                                duelGameService,
                                cooperativeGameService,
                                hostGameService,
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
            rmiServiceManager.stop();

            System.out.println(
                    "WordArena server stopped."
            );
        }
    }

    public static void main(String[] args) {
        new GameServer(DEFAULT_PORT).start();
    }
}