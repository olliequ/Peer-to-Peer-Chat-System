package com.kvoli.base;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;


/**
 * This class is used to read JSON objects (via Jackson).
 * It is used by Server.java and GetMessageThread of Client.java.
 */

public class JSONReader {
    private JsonNode jNode;
    private JsonNode jNode1;
    private ObjectMapper oMapper = new ObjectMapper();

    public JSONReader() {}

    public void readInput(String in) {
        try {
            jNode = oMapper.readTree(in);
        } catch (JsonProcessingException e) {
            System.out.println("Exception on JSONReader - readInput");
        }
    }

    public String getJSONType() {
        return jNode.get("type").asText();
    }

    public String getJSONHost() {
        return jNode.get("port").asText();
    }

    public String getJSONIP() {
        return jNode.get("ip").asText();
    }

    public String getJSONContent() {
        return jNode.get("content").asText();
    }

    public String getJSONIdentity() {
        return jNode.get("identity").asText();
    }

    public String getJSONFormerIdentity() {
        return jNode.get("former").asText();
    }

    public String getJSONRoomId() {
        return jNode.get("roomid").asText();
    }

    public String getJSONOwner() {
        return jNode.get("owner").asText();
    }

    public int getJSONTotalIdentities() {
        return jNode.get("totalIdentities").asInt();
    }

    public String getJSONSender() {
        return jNode.get("sender").asText();
    }

    public String getJSONKickMessage() {
        return jNode.get("kickmessage").asText();
    }


    public ArrayList<String> getJSONIdentities() {
        ArrayList<String> identities = new ArrayList<String>();
        for (JsonNode node : jNode.get("identities")) {
            String person = node.asText();
            identities.add(person);
        }
        return identities;
    }

    public ArrayList<String> getJSONRooms() {
        ArrayList<String> rooms = new ArrayList<String>();
        for (JsonNode node : jNode.get("rooms")) {
            String currentRoom = node.asText();
            rooms.add(currentRoom);
        }
        return rooms;
    }

    // Used exclusively for roomlist method in Client GetMessageThread.
    public String getJSONRoomName(String currentRoom) {
        try {
            jNode1 = oMapper.readTree(currentRoom);
        } catch (JsonProcessingException e) {
            System.out.println("Exception getting roomName in jReader");
        }
        String roomName = jNode1.get("roomid").asText(); // The clean, room-name.
        return roomName;
    }

//    public String getJSONListNeighbor(String currentNeighbor) {
//        try {
//            jNode1 = oMapper.readTree(currentNeighbor);
//        } catch (JsonProcessingException e) {
//            System.out.println("Exception getting neighbor in jReader");
//        }
//        String roomName = jNode1.get("roomid").asText(); // The clean, room-name.
//        return roomName;
//    }

    public String getJSONRoomCount(String currentRoom) {
        try {
            jNode1 = oMapper.readTree(currentRoom);
        } catch (JsonProcessingException e) {
            System.out.println("Exception getting roomCount in jReader");
        }
        String count = jNode1.get("count").asText(); // The clean, room-name.
        return count;
    }


    public String readMSg(String jsonString) {
        String content = null;

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            content = jsonNode.get("content").asText();
            return content;

        } catch (Exception e) {
            //e.printStackTrace();
            // Using ncat instead of the client can trigger this as well.
            //System.out.println("Reading exception. The message received may not have been a JSON object.");
        }
        return content;
    }


    public ArrayList<String> readListNeighbors() {
        ArrayList<String> neighbors = new ArrayList<String>();

        try {
            //System.out.println(jNode.get("neighbors").asText());
            for (JsonNode node : jNode.get("neighbors")) {
                String current = node.asText();
                neighbors.add(current);
            }
        } catch (Exception e) {}

        return neighbors;
    }


    // Used for Room Migration feature (sendMigration method of Peer.java)
    public String getJSONMigrationSender() {
        return jNode.get("sender").asText();
    }
    public String getJSONMigrationIdentity() {
        return jNode.get("identity").asText();
    }
    public String getJSONMigrationRoomName() {
        return jNode.get("roomName").asText();
    }
    public String getJSONMigrationTotalRooms() {
        return jNode.get("totalRooms").asText();
    }
    public String getJSONMigrationTotalIdentities() {
        return jNode.get("totalIdentities").asText();
    }


}
