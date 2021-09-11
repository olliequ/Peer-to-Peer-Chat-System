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
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_RESET = "\u001B[0m";

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
                    //System.out.println();
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode jsonNode = objectMapper.readTree(in);
                    String type = "null";

                    try {
                        type = jsonNode.get("type").asText();
                        } catch (Exception e) {}

                    // Received MESSAGE from server
                    if (type.equals("message")) {
//                        String content = jsonNode.get("content").asText();
//                        String identity = jsonNode.get("identity").asText();
//                        System.out.println(identity + ": " + content);
//                        this.client.setWelcomeStatus(true);

                        String content = jsonNode.get("content").asText();
                        String IncomingIdentity = jsonNode.get("identity").asText();

                        if (this.client.getIdentity().equals("1stEver")) {
                            this.client.setIdentity(IncomingIdentity);
                            this.client.setReadyToRock(true);
                            System.out.println(content);
                        }
                        else if (IncomingIdentity.equals("Server")) {
                            System.out.println(content);
                        }
                        else {
                            String currentRoom = this.client.getCurrentRoom();
                            if (this.client.getIdentity().equals(IncomingIdentity)) {
                                System.out.println("["+ currentRoom + "] " + IncomingIdentity + "> " + content);
                            }
                            else {
                                System.out.println("["+ currentRoom + "] " + IncomingIdentity + ": " + content);
                            }
                        }
                    }

                    // Received NEWIDENTITY from server
                    else if (type.equals("newidentity")) {
                        String former = jsonNode.get("former").asText();
                        String newIdentity = jsonNode.get("identity").asText();

                        if (former.equals(newIdentity))  {
                            System.out.println("Requested identity invalid or in use.");
                        }
                        // Handle identity assignment to new client.
                        else if (former.equals("")) {
                            this.client.setIdentity(newIdentity);
                        }
                        // Handle identity assignment to someone who sent a changeidentity request.
                        else if (newIdentity.equals(this.client.getRequestedIdentity())) {
                            this.client.setIdentity(newIdentity);
                            System.out.println(former + " is now " + newIdentity);
                        }
                    }

                    // Received ROOMCHANGE from server
                    else if (type.equals("roomchange")) {
                        String identity = jsonNode.get("identity").asText();
                        String former = jsonNode.get("former").asText();
                        String roomid = jsonNode.get("roomid").asText();
                        boolean validChange = false;

                        if (former.equals(roomid)) {
                            System.out.println("The requested room is invalid or non existent.");
                        }
                        else if (roomid.equals("") && (this.client.getClientToQuit() == false)) {
                            System.out.println(identity + " has left the server.");
                        }
                        else if (roomid.equals("") && (this.client.getClientToQuit() == true)) {
                            System.out.println("You have been disconnected from the server.");
                            gettingMessages = false;
                            this.client.close();
                        }

                        else if (former.equals("")) {
                            System.out.println(identity + " moves to " + roomid);
                            validChange = true;
                        }
                        else {
                            System.out.println(identity + " moved from " + former + " to " + roomid);
                            validChange = true;
                        }

                        if (identity.equals(this.client.getIdentity()) && validChange == true) {
                            this.client.setCurrentRoom(roomid);
                        }

                    }

                    // TODO: NOT PROPER
                    // Received ROOMLIST from server
                    else if (type.equals("roomlist")) {
                        boolean alreadyExistsOrInvalid = false;
                        boolean roomInList = false;
                        // For when a client sends #delete. Track if they were successful in deleting their room.
                        boolean deleteDesiredRoom = false;
                        boolean displayList = true;

                        // Logic for CreateRoom where room already exists.
                        // Iterate through list. If our desired room is not present then the room already exists.
                        for (JsonNode node : jsonNode.get("rooms")) {
                            String currentRoom = node.asText();

                            JsonNode jsonNode1 =  objectMapper.readTree(String.valueOf(currentRoom));
                            String name = jsonNode1.get("roomid").asText();
                            String count = jsonNode1.get("count").asText();
                            System.out.println("Room " + name + " with count " + count);

                            //System.out.println(currentRoom);
                            // Check if current room doesn't contain the room we want to create
                            if (!currentRoom.contains(this.client.getRoomToCreate())) {
                                alreadyExistsOrInvalid = true;
                            }
                            // If it does contain the room we want to create then room creation was successful
                            else if (currentRoom.contains(this.client.getRoomToCreate())) {
                                roomInList = true;
                                alreadyExistsOrInvalid = false;
                            }
                            if (currentRoom.contains(this.client.getRoomToDelete())) {
                                // Room deletion not successful
                            }
                            else {
                                deleteDesiredRoom = true;
                            }
                        }
                        if (alreadyExistsOrInvalid && roomInList == false) {
                            System.out.println("Room " + this.client.getRoomToCreate() + " is invalid or already in use.");
                            this.client.setRoomToCreate("");
                            displayList = false;
                        }

                        else if ((!alreadyExistsOrInvalid) && (roomInList == true) && (deleteDesiredRoom == false)){
                            // If our desired room was present in the received list then room creation was successful.
                            // We also need to reset the clients RoomToCreate string to empty.
                            // We have this additional condition to capture edge cases.
                            if (client.getClientToCreateRoom() == true) {
                                System.out.println("Room " + this.client.getRoomToCreate() + " created.");
                                this.client.setRoomToCreate("");
                                client.setClientToCreateRoom(false);
                            }

                        }
                        else if ((!alreadyExistsOrInvalid) && (deleteDesiredRoom == true)) {
                            System.out.println("Room " + this.client.roomToDelete + " was deleted.");
                            this.client.setRoomToDelete("");                // Reset
                            displayList = false;
                        }

                        // Now display the list of rooms....
                        if (displayList) {
                            for (JsonNode node : jsonNode.get("rooms")) {
                                String currentRoom = node.asText();




                            }
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
