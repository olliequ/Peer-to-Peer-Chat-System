package com.kvoli;

import com.kvoli.base.Base;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


public class GetMessageThread extends Thread {
    private Client client;
    private BufferedReader reader;
    private Socket socket;
    private String clientID;

    public GetMessageThread(Client client) {
        this.client = client;
        this.socket = client.socket;
        reader = new BufferedReader(new InputStreamReader(client.FromServer));
    }

    @Override
    public void run() {
        boolean gettingMessages = true;
        while (gettingMessages) {
            try {
                String in = reader.readLine();
                if (in != null) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode jsonNode = objectMapper.readTree(in);
                    String type = "null";

                    try {
                        type = jsonNode.get("type").asText();
                        } catch (Exception e) {}

                    // Received MESSAGE from server
                    if (type.equals("message")) {
                        String content = jsonNode.get("content").asText();
                        String identity = jsonNode.get("identity").asText();
                        System.out.println(identity + ": " + content);
                        clientID = identity;
                    }

                    // Received NEWIDENTITY from server
                    else if (type.equals("newidentity")) {
                        String former = jsonNode.get("former").asText();
                        String identity = jsonNode.get("identity").asText();
                        if (former.equals(identity))  {
                            System.out.println("Requested identity invalid or in use."); // Why not showing on all?
                        }
                        else {
                            System.out.println(former + " is now " + identity);
                        }
                    }

                    // Received ROOMCHANGE from server
                    else if (type.equals("roomchange")) {
                        String identity = jsonNode.get("identity").asText();
                        String former = jsonNode.get("former").asText();
                        String roomid = jsonNode.get("roomid").asText();
                        if (former.equals(roomid)) {
                            System.out.println("The requested room is invalid or non existent.");
                        }
                        else if (roomid.equals("")) {
                            System.out.println(identity + " has left the server.");
                        }
                        else {
                            System.out.println(identity + " moved from " + former + " to " + roomid);
                        }
                    }

                    // Received ROOMLIST from server
                    else if (type.equals("roomlist")) {
                        //String rooms = jsonNode.get("rooms").get(0).asText();

                        // Used for printing out each room in the list we received.
                        // TODO: Currently used for debugging. Should be deleted before submission.
                        for (JsonNode node : jsonNode.get("rooms")) {
                            System.out.println(node.asText());
                        }

                    }
                }

            } catch (IOException e) {
                System.out.println("Exception occurred when reading message.");
                e.printStackTrace();
                try {
                    socket.close();
                } catch (IOException ex) {
                    System.out.println("Exception occurred when closing socket.");
                    ex.printStackTrace();
                }
                break;
            }
        }
    }
}
