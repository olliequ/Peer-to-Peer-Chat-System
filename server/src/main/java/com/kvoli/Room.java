package com.kvoli;

import com.kvoli.base.Base;


import java.util.ArrayList;

public class Room extends Thread {
    private String roomID;

    // Should be String but using int for now (I am storing client port numbers).
    private ArrayList<String> roomContents = new ArrayList<>();


    public Room(String roomID) {
        this.roomID = roomID;
    }


    protected void addUser(String clientID) {
        roomContents.add(clientID);
    }

    protected void removeUser(String clientID) {
        //roomContents.remove(clientID);
        roomContents.remove(clientID);
    }

    protected void changeUserID(String oldID, String newID) {
        int indexOldID = roomContents.indexOf(oldID);
        roomContents.set(indexOldID, newID);
    }


    protected ArrayList<String> getRoomContents() {
        return roomContents;
    }

    protected String getRoomName() {
        return roomID;
    }

    protected int getRoomSize() {
        return roomContents.size();
    }
}

