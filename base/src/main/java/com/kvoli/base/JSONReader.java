package com.kvoli.base;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JSONReader {
    public String readMSg(String jsonString) {
        String content = null;

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            content = jsonNode.get("content").asText();
            return content;

        } catch (Exception e) {
            e.printStackTrace();
            // Using ncat instead of the client can trigger this as well.
            System.out.println("Reading exception. The message received may not have been a JSON object.");
        }
        return content;
    }


    // TODO: PROPERLY PARSE JSONS SENT FROM SERVER
}
