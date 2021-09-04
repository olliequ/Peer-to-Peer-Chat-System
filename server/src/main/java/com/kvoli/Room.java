package com.kvoli;

import com.kvoli.base.Base;


import java.util.ArrayList;

public class Room extends Thread {
    private int roomID;
    private String roomName;

    // Should be String but using int for now (I am storing client port numbers).
    private ArrayList<Integer> roomContents = new ArrayList<>();


    public Room(int roomID, String roomName) {
        this.roomID = roomID;
        this.roomName = roomName;
    }


    protected void addUser(int clientID) {
        roomContents.add(clientID);
    }

    protected void removeUser(int clientID) {
        roomContents.remove(clientID);
    }

    protected ArrayList<Integer> getRoomContents() {
        return roomContents;
    }

    protected String getRoomName() {
        return roomName;
    }
}

