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
                        else {
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

                    else if (type.equals("roomcontents")) {
                        String owner =  jsonNode.get("owner").asText();
                        String roomid = jsonNode.get("roomid").asText();

                        // Iterate over the identities array from RoomContents and print each user to the screen.
                        System.out.print(roomid + " contains: ");
                        for (JsonNode node: jsonNode.get("identities")) {
                            String person = node.asText();
                            if (person.equals(owner)) {
                                System.out.print(person + "* ");
                            }
                            else {
                                System.out.print(person + " ");
                            }
                        }
                        System.out.println();
                    }

                    // TODO: NOT PROPER
                    // Received ROOMLIST from server
                    else if (type.equals("roomlist")) {
                        boolean alreadyExistsOrInvalid = false;
                        boolean roomInList = false;
                        // For when a client sends #delete. Track if they were successful in deleting their room.
                        boolean deleteDesiredRoom = false;
                        boolean displayList = true;


                        //System.out.println(this.client.getListCommandStatus());


                        // Logic for CreateRoom where room already exists.
                        // Iterate through list. If our desired room is not present then the room already exists.

                        for (JsonNode node : jsonNode.get("rooms")) {
                            String currentRoom = node.asText();

                            JsonNode jsonNode1 =  objectMapper.readTree(String.valueOf(currentRoom));
                            String roomName = jsonNode1.get("roomid").asText();
                            String count = jsonNode1.get("count").asText();


                            // Check if current room doesn't contain the room we want to create
                            if (!roomName.equals(this.client.getRoomToCreate())) {
                                alreadyExistsOrInvalid = true;
                            }
                            // If it does contain the room we want to create then room creation was successful
                            else if (roomName.equals(this.client.getRoomToCreate())) {
                                roomInList = true;
                                alreadyExistsOrInvalid = false;
                            }
                            //if (roomName.equals(this.client.getRoomToDelete()) || ((this.client.getRoomToDelete()).equals(""))) {
                            // If room not in list then it has been deleted
                            if (roomName.equals(this.client.getRoomToDelete()) == false) {
                                //System.out.println("ROOMNAME " + roomName + " EQUALS " + this.client.getRoomToDelete());
                                deleteDesiredRoom = true;
                            }
                            else if (roomName.equals(this.client.getRoomToDelete())) {
                                deleteDesiredRoom = false;
                            }
                            
//                            else if ((!roomName.equals(this.client.getRoomToDelete()))) {
//                                deleteDesiredRoom = true;
//                            }
                        }
//                        System.out.println("ALREADYEXISTS/INVALID = " + alreadyExistsOrInvalid);
//                        System.out.println("IN LIST = " + roomInList);
//                        System.out.println("DELETE DESIRED = " + deleteDesiredRoom);


                        if (alreadyExistsOrInvalid && roomInList == false && this.client.getListCommandStatus() == false && this.client.getDeleteStatus() == false) {
                            System.out.println("Room " + this.client.getRoomToCreate() + " is invalid or already in use.");
                            this.client.setRoomToCreate("");
                            displayList = false;
                        }

                        else if ((!alreadyExistsOrInvalid) && (roomInList == true) && this.client.getListCommandStatus() == false && this.client.getDeleteStatus() == false){
                            // If our desired room was present in the received list then room creation was successful.
                            // We also need to reset the clients RoomToCreate string to empty.
                            // We have this additional condition to capture edge cases.
                            if (client.getClientToCreateRoom() == true) {
                                System.out.println("Room " + this.client.getRoomToCreate() + " created.");
                                this.client.setRoomToCreate("");
                                client.setClientToCreateRoom(false);
                            }

                        }
                        else if ((alreadyExistsOrInvalid) && (roomInList == false) && this.client.getListCommandStatus() == false && this.client.getDeleteStatus() == true) {
                            // If our room to delete wasn't in the list then it was deleted
                            System.out.println("Room " + this.client.roomToDelete + " was deleted.");
                            this.client.setRoomToDelete("");                // Reset
                            this.client.setDeleteStatus(false);

                        }

                        // Now display the list of rooms....
                        if (this.client.getListCommandStatus() == true) {
                            this.client.setListCommandStatus(false);
                            for (JsonNode node : jsonNode.get("rooms")) {
                                String currentRoom = node.asText();
                                JsonNode jsonNode1 =  objectMapper.readTree(String.valueOf(currentRoom));
                                String roomName = jsonNode1.get("roomid").asText();
                                String count = jsonNode1.get("count").asText();
                                System.out.println(roomName + " with count " + count);
                            }
                        }
                    }
                }

            } catch (IOException e) {
                System.out.println();
                System.out.println("Error occurred when retrieving message from the server.");
                System.out.println("You have been disconnected from the server.");
                //e.printStackTrace();
                try {
                    socket.close();
                    System.exit(0);
                } catch (IOException ex) {
                    System.out.println("Exception occurred when closing socket.");
                    ex.printStackTrace();
                }
                break;
            }
        }
    }
}
