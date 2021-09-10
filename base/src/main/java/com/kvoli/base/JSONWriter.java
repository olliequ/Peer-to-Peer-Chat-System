package com.kvoli.base;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class JSONWriter {

    /**
     * Called by the server to build a JSON string (Message) with the appropriate values (Identity & Content).
     * @param clientMsg
     * @param userID
     * @return JSON String message
     */

    // Used for the "Message" protocol -- to send simple on-screen messages to clients.
    public String buildJSON (String clientMsg, String userID, String FromServerOrNot) {
        String serverMessage = null;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // Build a new JSON string out of these fields
            ServerPackets.Message message = new ServerPackets.Message(userID, clientMsg, FromServerOrNot);
            serverMessage = objectMapper.writeValueAsString(message);
            // System.out.println("Testing: " + serverMessage);
            return serverMessage;
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error when writing message");
        }
        return serverMessage;
    }


    // Used for the "IdentityChange" protocol.
    public String buildJSONNewID(String formerID, String newID) {
        String serverMessage = null;
        ObjectMapper objectMapper = new ObjectMapper();

        try{
            // Build a new JSON string out of these fields
            ServerPackets.NewIdentity identityChange = new ServerPackets.NewIdentity(formerID, newID);
            serverMessage = objectMapper.writeValueAsString(identityChange);
            //System.out.println("Testing: " + serverMessage);
            return serverMessage;

        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error when writing newID message");
        }
        return serverMessage;
    }


    // Used for the "JoinRoom" protocol
    public String buildJSONJoinRoom(String identity, String formerRoom, String roomID) {
        String serverMessage = null;
        ObjectMapper objectMapper = new ObjectMapper();

        try{
            // Build a new JSON string out of these fields
            ServerPackets.RoomChange roomChange = new ServerPackets.RoomChange(identity, formerRoom, roomID);
            serverMessage = objectMapper.writeValueAsString(roomChange);
            //System.out.println("Testing: " + serverMessage);
            return serverMessage;

        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error when writing newID message");
        }
        return serverMessage;
    }


    // Build json string for the current room
    public String buildJsonRoomInfo(String roomid, int count){
        String serverMessage = null;
        ObjectMapper objectMapper = new ObjectMapper();

        try{
            // Build a new JSON string out of these fields
            ServerPackets.RoomInfo roomInfo = new ServerPackets.RoomInfo(roomid, count);
            serverMessage = objectMapper.writeValueAsString(roomInfo);
            return serverMessage;

        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error when writing newID message");
        }
        return serverMessage;

    }


    public String buildJsonRoomList(List<String> rooms) {
        String serverMessage = null;
        ObjectMapper objectMapper = new ObjectMapper();

        ArrayList<ArrayList<String>> test = new ArrayList<ArrayList<String>>();

        try{
            // Build a new JSON string out of these fields
            ServerPackets.RoomList roomList = new ServerPackets.RoomList(rooms);
            serverMessage = objectMapper.writeValueAsString(roomList);
            //System.out.println("Testing: " + serverMessage);
            return serverMessage;

        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error when writing newID message");
        }
        return serverMessage;
    }




}
