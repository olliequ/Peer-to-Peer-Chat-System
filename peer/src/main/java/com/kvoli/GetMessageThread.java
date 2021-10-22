package com.kvoli;

import com.kvoli.base.JSONReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;

public class GetMessageThread extends Thread {
    private Peer peer;
    private BufferedReader reader;
    private Socket socket;
    boolean getPeerMessages = true;
    private String serverAssignedIdentity = "NULL";             // Our IP and outgoing port number
    private String myCurrentRoom = "";

    public GetMessageThread(Peer peer) {
        this.peer = peer;
        this.socket = peer.socket;
        reader = new BufferedReader(new InputStreamReader(peer.FromConnectedPeer));
    }

    @Override
    public void run() {
        while (getPeerMessages) {
            try {
                String in = reader.readLine();
                if (in != null) {
                    JSONReader jRead = new JSONReader();
                    jRead.readInput(in);
                    String protocol = "null";

                    System.out.println("DEBUG Server Text:  " + in);

                    // If we've received some form of input then we've established a connection.
                    this.peer.connectionEstablishedWithServer = true;


                    try {
                        protocol = jRead.getJSONType();
                    } catch (Exception e) {
                        System.out.println("GetMessageThread exception when retrieving JSON type.");
                    }

                    if (protocol.equals("message")) {
                        String content = jRead.getJSONContent();
                        String incomingIdentity = jRead.getJSONIdentity();


                        // On LMS, Austen said a peers identity displayed during chat is the outgoing port.
                        // The specification does not outline a way/commmand for us to know OUR outgoing port.
                        // Only the server we are connected to knows our outgoing port.
                        // Thus, im using the "welcome" message that the server sends to determine our outgoing port.

                        // Upon initial connection the server tells us our identity (IP + outgoing port)
                        if (serverAssignedIdentity.equals("NULL")) {
                            serverAssignedIdentity = content;
                        }

                        //System.out.println("DEBUG: serverAssID is " + serverAssignedIdentity);
                        //System.out.println("DEBUG: incomID is " + incomingIdentity);

                        // If it's our own message, prepend our room ID in front of our message.
                        if (incomingIdentity.equals(serverAssignedIdentity)) {
                            String room = "[" + myCurrentRoom + "] ";
                            System.out.println(room + incomingIdentity + ": " + content);
                        }
                        // Case for when its someone elses message.
                        else {
                            System.out.println(incomingIdentity + ": " + content);
                        }
                    }

                    // Adopted from A1 but heavily dumbed down.
                    else if (protocol.equals("roomlist")) {
                        ArrayList<String> rooms = jRead.getJSONRooms();
                        ArrayList<String> localRooms = new ArrayList<String>();

                        // Print the room list from the server
                        for (String room : rooms) {
                            String roomName = jRead.getJSONRoomName(room);
                            String roomCount = jRead.getJSONRoomCount(room);
                            System.out.println("Room: " + roomName + " with " + roomCount + " users.");
                        }

                        // If this peer is hosting rooms locally then we should also return their local room list.
                        peer.getLocalRoomList();

                    }

                    // Used for when a peer joins another room.
                    else if (protocol.equals("roomchange")) {
                        String identity = jRead.getJSONIdentity();
                        String former = jRead.getJSONFormerIdentity();
                        String roomid = jRead.getJSONRoomId();

                        if (former.equals(roomid)) {
                            System.out.println("The requested room is invalid or non existent.");
                        }

                        else if (former.equals("")) {
                            System.out.println(identity + " moves to " + roomid);
                            myCurrentRoom = roomid;
                        }
                        else {
                            myCurrentRoom = roomid;
                            System.out.println(identity + " moved from " + former + " to " + roomid);
                        }
                    }

                    // For when a peer issues a #who command.
                    else if (protocol.equals("roomcontents")) {
                        String owner = jRead.getJSONOwner();
                        String roomid = jRead.getJSONRoomId();
                        ArrayList<String> identities = jRead.getJSONIdentities();

                        // Iterate over the identities array from RoomContents and print each user to the screen.
                        // For A2 I removed the code that told us the room owner.
                        System.out.print(roomid + " contains: ");
                        for (String person : identities) {
                            System.out.print(person + " ");
                        }
                        System.out.println();
                    }



                }
            } catch (IOException e) {
                System.out.println("GetMessageThread exception when retrieving messages from peer");
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
