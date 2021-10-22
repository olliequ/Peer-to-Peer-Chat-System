package com.kvoli.base;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * This class is used for the construction of JSON strings (via Jackson).
 * It is used by both Server.java and SendMessageThread of Client.java
 */

public class JSONWriter {
    private ObjectMapper oMapper = new ObjectMapper();


    // Used for the "Message" protocol -- to send simple on-screen messages to clients.
    public String buildJSON (String clientMsg, String userID) {
        String serverMessage = null;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // Build a new JSON string out of these fields
            ServerPackets.Message message = new ServerPackets.Message(userID, clientMsg);
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
    public String buildJSONNewID (String formerID, String newID) {
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
    public String buildJSONJoinRoom (String identity, String formerRoom, String roomID) {
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
            System.out.println("Error when writing JoinRoom message");
        }
        return serverMessage;
    }

    // Build json string for the current room
    public String buildJsonRoomInfo (String roomid, int count){
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
            System.out.println("Error when writing RoomInfo message");
        }
        return serverMessage;

    }

    public String buildJsonRoomList (List<String> rooms) {
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
            System.out.println("Error when writing RoomList message");
        }
        return serverMessage;
    }

    public String buildJsonRoomContents (String roomid, List<String> identities, String owner) {
        String serverMessage = null;
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            ServerPackets.RoomContents roomContents = new ServerPackets.RoomContents(roomid, identities, owner);
            serverMessage = objectMapper.writeValueAsString(roomContents);
            return serverMessage;
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error when writing RoomContents message");
        }
        return serverMessage;
    }


    public String buildIdentityChangeMsg (ClientPackets.IdentityChange identityChange) {
        String msg = null;
        try {
            msg = oMapper.writeValueAsString(identityChange);
        } catch (JsonProcessingException e) {
            System.out.println("Exception in JSONWriter buildIdentityChangeMsg");
        }
        return msg;
    }

    public String buildJoinMsg (ClientPackets.Join join ) {
        String msg = null;
        try {
            msg = oMapper.writeValueAsString(join);
        } catch (JsonProcessingException e) {
            System.out.println("Exception in JSONWriter buildJoin");
        }
        return msg;
    }

    public String buildListMsg (ClientPackets.List list ) {
        String msg = null;
        try {
            msg = oMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            System.out.println("Exception in JSONWriter buildList");
        }
        return msg;
    }

    public String buildCreateRoomMsg (ClientPackets.CreateRoom createRoom) {
        String msg = null;
        try {
            msg = oMapper.writeValueAsString(createRoom);
        } catch (JsonProcessingException e) {
            System.out.println("Exception in JSONWriter createRoom");
        }
        return msg;
    }

    public String buildWhoMsg (ClientPackets.Who who) {
        String msg = null;
        try {
            msg = oMapper.writeValueAsString(who);
        } catch (JsonProcessingException e) {
            System.out.println("Exception in JSONWriter buildWhoMsg");
        }
        return msg;
    }

    public String buildQuitMsg (ClientPackets.Quit quit) {
        String msg = null;
        try {
            msg = oMapper.writeValueAsString(quit);
        } catch (JsonProcessingException e) {
            System.out.println("Exception in JSONWriter buildQuitMsg");
        }
        return msg;
    }

    public String buildDeleteMessage (ClientPackets.Delete delete) {
        String msg = null;
        try {
            msg = oMapper.writeValueAsString(delete);
        } catch (JsonProcessingException e) {
            System.out.println("Exception in JSONWriter buildDeleteMsg");
        }
        return msg;
    }

    public String buildMessage (ClientPackets.Message message) {
        String msg = null;
        try {
            msg = oMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            System.out.println("Exception in JSONWriter buildMessage");
        }
        return msg;
    }

    public String buildListNeighborsMsg (ClientPackets.ListNeighbors listNeighbors) {
        String msg = null;
        try {
            msg = oMapper.writeValueAsString(listNeighbors);
        } catch (JsonProcessingException e) {
            System.out.println("Exception in JSONWriter buildListNeighborsMsg");
        }
        return msg;
    }

    public String buildJsonListNeighbors (List<String> listOfNeighbors) {
        String serverMessage = null;
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            ServerPackets.Neighbors neighbors = new ServerPackets.Neighbors(listOfNeighbors);
            serverMessage = objectMapper.writeValueAsString(neighbors);
            return serverMessage;
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error when writing BuildJSONListNeighbors message");
        }
        return serverMessage;
    }
}
