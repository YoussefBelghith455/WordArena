package com.wordarena.common.remote;

import com.wordarena.common.PlayerStats;
import com.wordarena.common.PlayerView;
import com.wordarena.common.RoomInfo;
import com.wordarena.common.ScoreEntry;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface WordArenaRemoteService extends Remote {

    String SERVICE_NAME = "WordArenaRemoteService";
    int REGISTRY_PORT = 1099;

    List<RoomInfo> getAvailableRooms()
            throws RemoteException;

    RoomInfo getRoomInfo(String roomId)
            throws RemoteException;

    List<PlayerView> getRoomPlayers(String roomId)
            throws RemoteException;

    List<ScoreEntry> getLeaderboard()
            throws RemoteException;

    PlayerStats getPlayerStats(String playerId)
            throws RemoteException;

    boolean ping()
            throws RemoteException;
}