package com.kvoli.base;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


public class JSONWriter {

    /**
     * Called by the server to build a JSON string (Message) with the appropriate values (Identity & Content).
     * @param clientMsg
     * @param userID
     * @return JSON String message
     */

    public String buildJSON(String clientMsg, String userID) {
        String serverMessage = null;
        ObjectMapper objectMapper = new ObjectMapper();

        try{
            // Create new field 'identity'.
            String identity = userID;

            // Build a new JSON string out of these fields
            ServerPackets.Message message = new ServerPackets.Message(identity, clientMsg);
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
}
