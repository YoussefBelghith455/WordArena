
package com.wordarena.client.tcp;

import com.wordarena.common.GameMessage;
import com.wordarena.server.tcp.GameServer;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.function.Consumer;

public class GameTcpClient implements AutoCloseable {

    private final String host;
    private final int port;

    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private Thread listenerThread;

    private volatile boolean connected;
    private volatile boolean intentionalClose;
    private volatile Consumer<GameMessage> messageListener;
    private volatile Consumer<Exception> errorListener;

    public GameTcpClient() {
        this("localhost", GameServer.DEFAULT_PORT);
    }

    public GameTcpClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public synchronized void connect() throws IOException {
        if (connected) {
            return;
        }

        if (listenerThread != null && listenerThread.isAlive()) {
            throw new IOException(
                    "Previous TCP listener is still running."
            );
        }

        intentionalClose = false;

        Socket newSocket = new Socket(host, port);

        try {
            newSocket.setKeepAlive(true);

            ObjectOutputStream newOutput =
                    new ObjectOutputStream(newSocket.getOutputStream());

            newOutput.flush();

            ObjectInputStream newInput =
                    new ObjectInputStream(newSocket.getInputStream());

            socket = newSocket;
            output = newOutput;
            input = newInput;
            connected = true;

        } catch (IOException exception) {
            newSocket.close();
            throw exception;
        }
    }

    public synchronized void startListening() {
        if (!connected) {
            throw new IllegalStateException(
                    "TCP client is not connected."
            );
        }

        if (listenerThread != null && listenerThread.isAlive()) {
            return;
        }

        listenerThread = new Thread(() -> {
            Exception disconnectReason = null;

            try {
                while (connected) {
                    Object received = input.readObject();

                    if (received instanceof GameMessage message) {
                        Consumer<GameMessage> listener = messageListener;

                        if (listener != null) {
                            listener.accept(message);
                        }
                    }
                }
            } catch (EOFException exception) {
                disconnectReason = new IOException(
                        "The server closed the connection.",
                        exception
                );
            } catch (SocketException exception) {
                disconnectReason = new IOException(
                        "The TCP connection was interrupted.",
                        exception
                );
            } catch (IOException | ClassNotFoundException exception) {
                disconnectReason = exception;
            } finally {
                boolean unexpectedDisconnect =
                        !intentionalClose && disconnectReason != null;

                connected = false;

                try {
                    closeSocket();
                } catch (IOException ignored) {
                }

                if (unexpectedDisconnect) {
                    Consumer<Exception> listener = errorListener;

                    if (listener != null) {
                        listener.accept(disconnectReason);
                    }
                }
            }
        });

        listenerThread.setName("wordarena-tcp-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public synchronized void send(GameMessage message)
            throws IOException {

        if (!connected || output == null) {
            throw new IOException(
                    "TCP client is not connected."
            );
        }

        try {
            output.writeObject(message);
            output.flush();
            output.reset();
        } catch (IOException exception) {
            connected = false;

            try {
                closeSocket();
            } catch (IOException closeException) {
                exception.addSuppressed(closeException);
            }

            throw exception;
        }
    }

    public void setMessageListener(
            Consumer<GameMessage> messageListener
    ) {
        this.messageListener = messageListener;
    }

    public void setErrorListener(
            Consumer<Exception> errorListener
    ) {
        this.errorListener = errorListener;
    }

    public boolean isConnected() {
        return connected;
    }

    private void closeSocket() throws IOException {
        Socket currentSocket = socket;

        if (currentSocket != null && !currentSocket.isClosed()) {
            currentSocket.close();
        }
    }

    @Override
    public synchronized void close() throws IOException {
        intentionalClose = true;
        connected = false;
        closeSocket();
    }
}
