package com.wordarena.server.tcp;

import com.wordarena.server.game.CooperativeRound;
import com.wordarena.server.game.DoubleHostRound;
import com.wordarena.server.game.DuelRound;
import com.wordarena.server.game.GameRoom;
import com.wordarena.server.game.HostRound;

import java.util.Objects;

public final class ReconnectProtocol {

    public static final String RECONNECT =
            "RECONNECT";

    public static final String RECONNECT_TOKEN =
            "RECONNECT_TOKEN";

    public static final String RECONNECT_OK =
            "RECONNECT_OK";

    public static final String SESSION_EXPIRED =
            "SESSION_EXPIRED";

    private static final String FIELD_SEPARATOR =
            "|";

    private ReconnectProtocol() {
    }

    public static String parseReconnectToken(
            String content
    ) {
        if (content == null
                || content.isBlank()) {

            throw RoomProtocol.protocolError(
                    RoomProtocol.ErrorCode
                            .INVALID_REQUEST_FORMAT,
                    "RECONNECT requires a reconnect token."
            );
        }

        return content.trim();
    }

    public static String encodeToken(
            ReconnectSession session
    ) {
        Objects.requireNonNull(
                session,
                "Reconnect session is required."
        );

        return String.join(
                FIELD_SEPARATOR,
                session.reconnectToken(),
                Long.toString(
                        session.expiresAtTimestamp()
                )
        );
    }

    public static String encodeReconnectSuccess(
            ReconnectSession session
    ) {
        Objects.requireNonNull(
                session,
                "Reconnect session is required."
        );

        return String.join(
                FIELD_SEPARATOR,
                session.playerId(),
                session.username(),
                session.roomId(),
                Long.toString(
                        session.expiresAtTimestamp()
                )
        );
    }

    public static String encodeRestoredRoomState(
            GameRoom room
    ) {
        Objects.requireNonNull(
                room,
                "Game room is required."
        );

        return RoomProtocol.encodeRoomState(
                room
        );
    }

    public static String encodeRestoredRoundState(
            GameRoom room
    ) {
        Objects.requireNonNull(
                room,
                "Game room is required."
        );

        return switch (room.getGameMode()) {
            case DUEL ->
                    room.getDuelRound()
                            .map(
                                    DuelProtocol::
                                            encodeRoundProgress
                            )
                            .orElse("");

            case COOPERATIVE ->
                    room.getCooperativeRound()
                            .map(
                                    CooperativeProtocol::
                                            encodeRoundProgress
                            )
                            .orElse("");

            case HOST ->
                    encodeHostRestoration(room);

            case DOUBLE_HOST ->
                    encodeDoubleHostRestoration(room);
        };
    }

    private static String encodeHostRestoration(
            GameRoom room
    ) {
        /*
         * HostRound is currently owned by HostGameService,
         * so the service supplies the active round during
         * final TCP restoration.
         */
        return room.isStarted()
                ? "HOST|ACTIVE"
                : "";
    }

    private static String encodeDoubleHostRestoration(
            GameRoom room
    ) {
        /*
         * DoubleHostRound is currently owned by
         * DoubleHostGameService. No private word is ever
         * included in reconnect state.
         */
        return room.isStarted()
                ? "DOUBLE_HOST|ACTIVE"
                : "";
    }

    public static String encodeDuelRound(
            DuelRound round
    ) {
        return DuelProtocol.encodeRoundProgress(
                Objects.requireNonNull(
                        round,
                        "Duel round is required."
                )
        );
    }

    public static String encodeCooperativeRound(
            CooperativeRound round
    ) {
        return CooperativeProtocol.encodeRoundProgress(
                Objects.requireNonNull(
                        round,
                        "Cooperative round is required."
                )
        );
    }

    public static String encodeHostRound(
            HostRound round
    ) {
        return HostProtocol.encodeRoundProgress(
                Objects.requireNonNull(
                        round,
                        "Host round is required."
                )
        );
    }

    public static String encodeDoubleHostRound(
            DoubleHostRound round
    ) {
        return DoubleHostProtocol.encodeRoundProgress(
                Objects.requireNonNull(
                        round,
                        "Double Host round is required."
                )
        );
    }
}