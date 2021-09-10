package com.kvoli;

import com.kvoli.base.Base;


import java.util.ArrayList;

public class Room {

    // Instance variables
    private String roomID;
    private String roomOwner = "null";
    private ArrayList<String> roomContents = new ArrayList<>();

    // Constructor 1: Default
    public Room (String roomID) {
        this.roomID = roomID;
    }

    // Constructor 2: For user room creation
    public Room (String roomID, String roomOwner) {
        this.roomID = roomID;
        this.roomOwner = roomOwner;
    }



    // Methods
    protected void addUser (String clientID) {
        roomContents.add(clientID);
    }

    protected void removeUser (String clientID) { roomContents.remove(clientID); }

    protected void changeUserID (String oldID, String newID) {
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

    protected void setRoomOwner(String roomID) {
        this.roomOwner = roomID;
    }

    protected String getRoomOwner() {
        return this.roomOwner;
    }

}

