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
    public static final String ANSI_RED = "\u001B[31m";
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
                        else if (roomid.equals("") && !this.client.getClientToQuit()) {
                            System.out.println(identity + " leaves "+this.client.getCurrentRoom()+".");
                        }
                        else if (roomid.equals("") && this.client.getClientToQuit()) {
                            System.out.println(identity + " leaves "+this.client.getCurrentRoom()+".");
                            // System.out.println("You have been successfully disconnected from the server.");
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

                        if (identity.equals(this.client.getIdentity()) && validChange) {
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

                    // Received ROOMLIST from server: Will happen for #list, #createroom, and #deleteroom.
                    else if (type.equals("roomlist")) {
                        boolean alreadyExistsOrInvalid = false; // Remember, if it doesn't exist yet, but it has an invalid proposed name, we can't add it.
                        boolean roomInList = false;             // Will turn true if our iteration shows it is present -- i.e. successful creation.
                        // The 2 below variables are for when a client commands #delete. Track if they were successful in deleting their room.
                        boolean deleteDesiredRoom = false;
                        boolean displayList = true;

                        /**
                         * As noted above, we're going to receive a #roomlist command from the server for 3 different commands we issue:
                         * #list, #createroom, and #deleteroom. That's why this 'else-if' block is big.
                         * If we initially issued a #list command, then this.client.getListCommandStatus() will equal true.
                         * If we initially issued a #delete command, then this.client.getDeleteCommandStatus() will equal true.
                         * If we initially issued a #createroom command, then they will both be false. This is how below we identify which
                         * command we sent out, even though the server always send a roomlist JSON back to us.
                         */

                        // Iterate through list of rooms we received from server JSON. If our desired room to create is not
                        // present, then the room already exists.
                        for (JsonNode node : jsonNode.get("rooms")) {
                            // 'node' is the messy version of one item (room) in the received 'rooms' array.
                            String currentRoom = node.asText(); // Cleaned up version.
                            JsonNode jsonNode1 =  objectMapper.readTree(currentRoom);
                            String roomName = jsonNode1.get("roomid").asText(); // The clean, room-name.
                            String count = jsonNode1.get("count").asText();     // Clean room-count figure.

                            // Check if current room doesn't contain the room we want to create
                            if (!roomName.equals(this.client.getRoomToCreate())) {
                                alreadyExistsOrInvalid = true;
                            }
                            // If it does contain the room we want to create then room creation was successful. Works because room is always appended at the end.
                            else if (roomName.equals(this.client.getRoomToCreate())) {
                                roomInList = true;
                                alreadyExistsOrInvalid = false;
                            }

                            // For #delete command: If room not in list then it has been deleted.
                            if (!roomName.equals(this.client.getRoomToDelete())) {
                                //System.out.println("ROOMNAME " + roomName + " EQUALS " + this.client.getRoomToDelete());
                                deleteDesiredRoom = true;
                            }
                            else if (roomName.equals(this.client.getRoomToDelete())) {
                                deleteDesiredRoom = false;
                            }
                        }

                        /**
                         * If the room already existed, it's not in the received list, and we're not dealing with a #list or #delete command.
                         * Basically dealing with the case when we've received a JSON indicating the requested room to create already exists.
                         * Remember, if it already exists, we contain a room list JSON that doesn't contain it, lol.
                         */
                        if (alreadyExistsOrInvalid && !roomInList && !this.client.getListCommandStatus() && !this.client.getDeleteStatus()) {
                            System.out.println("Room " + this.client.getRoomToCreate() + " is invalid or already in use.");
                            this.client.setRoomToCreate("");
                            displayList = false;
                        }

                        /**
                         * If the room didn't already exist, it's in the current list, and we're not dealing with a #list or #delete command.
                         * Basically dealing with the case when we've received a JSON indicating the requested room creation is successful.
                         * If our desired room was present in the received list then room creation was successful.
                         * We also need to reset the clients RoomToCreate string to empty.
                         * We have this additional condition to capture edge cases.
                         */
                        else if (!alreadyExistsOrInvalid && roomInList && !this.client.getListCommandStatus() && !this.client.getDeleteStatus()) {
                            if (client.getClientToCreateRoom()) {
                                System.out.println("Room " + this.client.getRoomToCreate() + " created.");
                                this.client.setRoomToCreate("");
                                client.setClientToCreateRoom(false);
                            }
                        }

                        // For if we received the #delete command.
                        else if (alreadyExistsOrInvalid && !roomInList && !this.client.getListCommandStatus() && this.client.getDeleteStatus()) {
                            // If our room to delete wasn't in the list, then it was deleted.
                            System.out.println("Room " + this.client.roomToDelete + " was deleted.");
                            this.client.setRoomToDelete("");     // Reset this room to delete string, because delete was successful.
                            this.client.setDeleteStatus(false);  // Requirement to delete something no longer needed as job finished.
                        }

                        // If we received issued a #list command. If after deleting a room we want to print all rooms and their members
                        // then make getListCommandStatus() = true when you issue the delete request.
                        if (this.client.getListCommandStatus()) {
                            this.client.setListCommandStatus(false);
                            for (JsonNode node : jsonNode.get("rooms")) {
                                String currentRoom = node.asText();
                                JsonNode jsonNode1 =  objectMapper.readTree(String.valueOf(currentRoom));
                                String roomName = jsonNode1.get("roomid").asText();
                                String count = jsonNode1.get("count").asText();
                                System.out.println(roomName + " currently has " + count + " users inside.");
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
