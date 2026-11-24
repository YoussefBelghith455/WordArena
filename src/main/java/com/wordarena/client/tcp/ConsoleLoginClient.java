package com.wordarena.client.tcp;

import com.wordarena.common.GameMessage;
import com.wordarena.server.tcp.GameServer;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class ConsoleLoginClient {

    private static final String SERVER_HOST =
            "localhost";

    private final Scanner scanner;

    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;

    private String playerId;
    private String currentRoomId;
    private String reconnectToken;

    private volatile boolean running;
    private volatile boolean reconnecting;

    public ConsoleLoginClient() {
        scanner = new Scanner(System.in);
    }

    public void start() {
        try {
            connect();

            if (!login()) {
                return;
            }

            running = true;
            startServerMessageListener();
            runCommandLoop();
        } catch (
                IOException
                | ClassNotFoundException exception
        ) {
            System.err.println(
                    "WordArena client error: "
                            + exception.getMessage()
            );
        } finally {
            closeConnection();
        }
    }

    private void connect() throws IOException {
        socket = new Socket(
                SERVER_HOST,
                GameServer.DEFAULT_PORT
        );

        output =
                new ObjectOutputStream(
                        socket.getOutputStream()
                );

        output.flush();

        input =
                new ObjectInputStream(
                        socket.getInputStream()
                );

        System.out.println(
                "Connected to WordArena server at "
                        + SERVER_HOST
                        + ":"
                        + GameServer.DEFAULT_PORT
        );
    }

    private boolean login()
            throws IOException, ClassNotFoundException {

        System.out.print("Enter username: ");

        String username =
                scanner.nextLine();

        sendMessage(
                new GameMessage(
                        "LOGIN",
                        "",
                        "",
                        username
                )
        );

        Object object =
                input.readObject();

        if (!(object
                instanceof GameMessage response)) {

            return false;
        }

        printMessage(response);

        if (!"OK".equals(
                response.getType()
        )) {
            return false;
        }

        playerId =
                response.getPlayerId();

        Object tokenObject =
                input.readObject();

        if (tokenObject
                instanceof GameMessage tokenMessage
                && "RECONNECT_TOKEN".equals(
                tokenMessage.getType()
        )) {

            updateLocalState(
                    tokenMessage
            );

            printMessage(
                    tokenMessage
            );
        }

        return reconnectToken != null;
    }

    private void startServerMessageListener() {
        Thread listenerThread =
                new Thread(
                        this::listenForServerMessages,
                        "wordarena-console-listener"
                );

        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void listenForServerMessages() {
        while (running) {
            try {
                Object object =
                        input.readObject();

                if (object
                        instanceof GameMessage message) {

                    updateLocalState(message);

                    System.out.println();
                    printMessage(message);
                    printPrompt();
                }
            } catch (EOFException exception) {
                if (running
                        && !reconnecting) {

                    System.out.println();
                    System.out.println(
                            "Connection lost. Use reconnect "
                                    + "within two minutes."
                    );

                    printPrompt();
                }

                break;
            } catch (
                    IOException
                    | ClassNotFoundException exception
            ) {
                if (running
                        && !reconnecting) {

                    System.err.println();
                    System.err.println(
                            "Connection unavailable: "
                                    + exception.getMessage()
                    );

                    printPrompt();
                }

                break;
            }
        }
    }

    private void updateLocalState(
            GameMessage message
    ) {
        if ("RECONNECT_TOKEN".equals(
                message.getType()
        )) {
            String content =
                    message.getContent();

            int separatorIndex =
                    content.indexOf('|');

            reconnectToken =
                    separatorIndex < 0
                            ? content
                            : content.substring(
                            0,
                            separatorIndex
                    );
        }

        if ("RECONNECT_OK".equals(
                message.getType()
        )) {
            String[] fields =
                    message.getContent()
                            .split("\\|", -1);

            if (fields.length >= 3) {
                playerId = fields[0];

                currentRoomId =
                        fields[2].isBlank()
                                ? null
                                : fields[2];
            }
        }

        if ("ROOM_STATE".equals(
                message.getType()
        )
                && message.getRoomId() != null
                && !message.getRoomId().isBlank()) {

            currentRoomId =
                    message.getRoomId();
        }

        if ("OK".equals(message.getType())
                && message.getContent() != null
                && message.getContent().contains(
                "Successfully left the room."
        )) {

            currentRoomId = null;
        }
    }

    private void runCommandLoop()
            throws IOException {

        printHelp();

        while (running) {
            printPrompt();

            if (!scanner.hasNextLine()) {
                break;
            }

            String command =
                    scanner.nextLine()
                            .trim()
                            .toLowerCase();

            try {
                switch (command) {
                    case "create" -> createRoom();
                    case "join" -> joinRoom();
                    case "leave" -> leaveRoom();
                    case "ready" -> setReady();
                    case "lock" -> lockSecretWord();
                    case "start" -> startRound();
                    case "guess" -> submitGuess();
                    case "room" -> showCurrentRoom();
                    case "token" -> showReconnectToken();
                    case "reconnect" -> reconnect();
                    case "help" -> printHelp();
                    case "quit", "exit" ->
                            running = false;

                    default -> System.out.println(
                            "Unknown command. Enter help."
                    );
                }
            } catch (IOException exception) {
                System.out.println(
                        "The connection is unavailable. "
                                + "Use reconnect."
                );
            }
        }
    }

    private void reconnect() {
        if (reconnectToken == null
                || reconnectToken.isBlank()) {

            System.out.println(
                    "No reconnect token is available."
            );

            return;
        }

        try {
            reconnecting = true;
            closeSocketOnly();

            Thread.sleep(250L);

            connect();

            sendMessage(
                    new GameMessage(
                            "RECONNECT",
                            "",
                            "",
                            reconnectToken
                    )
            );

            Object object =
                    input.readObject();

            if (!(object
                    instanceof GameMessage response)) {

                throw new IOException(
                        "Unsupported reconnect response."
                );
            }

            printMessage(response);
            updateLocalState(response);

            if (!"RECONNECT_OK".equals(
                    response.getType()
            )) {
                closeSocketOnly();
                return;
            }

            reconnecting = false;
            startServerMessageListener();

            System.out.println(
                    "Reconnect completed."
            );
        } catch (
                IOException
                | ClassNotFoundException
                | InterruptedException exception
        ) {
            reconnecting = false;

            if (exception
                    instanceof InterruptedException) {

                Thread.currentThread()
                        .interrupt();
            }

            System.err.println(
                    "Reconnect failed: "
                            + exception.getMessage()
            );
        }
    }

    private void createRoom()
            throws IOException {

        System.out.print("Room name: ");
        String name = scanner.nextLine();

        System.out.print(
                "Game mode "
                        + "(DUEL, COOPERATIVE, HOST, DOUBLE_HOST): "
        );

        String mode =
                scanner.nextLine()
                        .trim()
                        .toUpperCase();

        System.out.print("Maximum players: ");
        String capacity =
                scanner.nextLine().trim();

        sendMessage(
                new GameMessage(
                        "CREATE_ROOM",
                        playerId,
                        "",
                        name
                                + "|"
                                + mode
                                + "|"
                                + capacity
                )
        );
    }

    private void joinRoom()
            throws IOException {

        System.out.print("Room ID: ");

        sendMessage(
                new GameMessage(
                        "JOIN_ROOM",
                        playerId,
                        scanner.nextLine().trim(),
                        ""
                )
        );
    }

    private void leaveRoom()
            throws IOException {

        String roomId =
                requireCurrentRoomId();

        if (roomId != null) {
            sendMessage(
                    new GameMessage(
                            "LEAVE_ROOM",
                            playerId,
                            roomId,
                            ""
                    )
            );
        }
    }

    private void setReady()
            throws IOException {

        String roomId =
                requireCurrentRoomId();

        if (roomId == null) {
            return;
        }

        System.out.print(
                "Ready state (true/false): "
        );

        sendMessage(
                new GameMessage(
                        "SET_READY",
                        playerId,
                        roomId,
                        scanner.nextLine().trim()
                )
        );
    }

    private void lockSecretWord()
            throws IOException {

        String roomId =
                requireCurrentRoomId();

        if (roomId == null) {
            return;
        }

        System.out.print(
                "Five-letter secret word: "
        );

        sendMessage(
                new GameMessage(
                        "LOCK_WORD",
                        playerId,
                        roomId,
                        scanner.nextLine()
                                .trim()
                                .toUpperCase()
                )
        );
    }

    private void startRound()
            throws IOException {

        String roomId =
                requireCurrentRoomId();

        if (roomId != null) {
            sendMessage(
                    new GameMessage(
                            "START_ROUND",
                            playerId,
                            roomId,
                            ""
                    )
            );
        }
    }

    private void submitGuess()
            throws IOException {

        String roomId =
                requireCurrentRoomId();

        if (roomId == null) {
            return;
        }

        System.out.print(
                "Five-letter guess: "
        );

        sendMessage(
                new GameMessage(
                        "GUESS",
                        playerId,
                        roomId,
                        scanner.nextLine()
                                .trim()
                                .toUpperCase()
                )
        );
    }

    private void showCurrentRoom() {
        System.out.println(
                currentRoomId == null
                        ? "You are not currently in a room."
                        : "Current room ID: "
                        + currentRoomId
        );
    }

    private void showReconnectToken() {
        System.out.println(
                reconnectToken == null
                        ? "No reconnect token is available."
                        : "Reconnect token: "
                        + reconnectToken
        );
    }

    private String requireCurrentRoomId() {
        if (currentRoomId == null
                || currentRoomId.isBlank()) {

            System.out.println(
                    "You are not currently in a room."
            );

            return null;
        }

        return currentRoomId;
    }

    private synchronized void sendMessage(
            GameMessage message
    ) throws IOException {

        output.writeObject(message);
        output.flush();
        output.reset();
    }

    private void printMessage(
            GameMessage message
    ) {
        System.out.println(
                "------------------------------"
        );

        System.out.println(
                "Type: " + message.getType()
        );

        System.out.println(
                "Player ID: "
                        + message.getPlayerId()
        );

        System.out.println(
                "Room ID: "
                        + message.getRoomId()
        );

        System.out.println(
                "Content: "
                        + message.getContent()
        );

        System.out.println(
                "------------------------------"
        );
    }

    private void printHelp() {
        System.out.println();
        System.out.println(
                "Available commands"
        );

        System.out.println(
                "------------------------------"
        );

        System.out.println(
                "create    - Create a room"
        );

        System.out.println(
                "join      - Join a room"
        );

        System.out.println(
                "leave     - Leave the current room"
        );

        System.out.println(
                "ready     - Change ready state"
        );

        System.out.println(
                "lock      - Lock a private word"
        );

        System.out.println(
                "start     - Start the round"
        );

        System.out.println(
                "guess     - Submit a guess"
        );

        System.out.println(
                "room      - Display the room ID"
        );

        System.out.println(
                "token     - Display reconnect token"
        );

        System.out.println(
                "reconnect - Reopen connection and restore state"
        );

        System.out.println(
                "quit      - Exit"
        );

        System.out.println(
                "------------------------------"
        );
    }

    private void printPrompt() {
        System.out.print("> ");
    }

    private void closeSocketOnly() {
        if (socket != null
                && !socket.isClosed()) {

            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void closeConnection() {
        running = false;
        closeSocketOnly();
    }

    public static void main(String[] args) {
        new ConsoleLoginClient().start();
    }
}