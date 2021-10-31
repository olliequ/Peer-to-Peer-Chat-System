package com.kvoli;

import com.kvoli.base.ClientPackets;
import com.kvoli.base.JSONReader;
import com.kvoli.base.JSONWriter;
import org.w3c.dom.ls.LSOutput;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.spec.RSAOtherPrimeInfo;
import java.util.ArrayList;

public class GetMessageThread extends Thread {
    private Peer peer;
    private BufferedReader reader;
    private PrintWriter writer;
    boolean getPeerMessages = true;
    private String serverAssignedIdentity = "NULL";             // Our IP and outgoing port number
    private String myCurrentRoom = "";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_RESET = "\u001B[0m";

    public GetMessageThread(Peer peer) {
        this.peer = peer;
        reader = new BufferedReader(new InputStreamReader(peer.FromConnectedPeer));
    }

    @Override
    public void run() {
        while (getPeerMessages) {
            JSONWriter jWrite = new JSONWriter();

            try {
                String in = reader.readLine(); // Reads in JSON objects as they arrive
                // System.out.println(in);
                if (in != null) {
                    JSONReader jRead = new JSONReader();
                    jRead.readInput(in);
                    String type = jRead.getJSONType();
                    String protocol = "null";
                    //System.out.println(ANSI_RED+"Received "+type+" JSON: "+ANSI_RESET + in);

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

                        // On LMS, Austen said that a peer's identity displayed during chat is the outgoing port.
                        // The specification does not outline a way/command for us to know OUR outgoing port.
                        // Only the server we are connected to knows our outgoing port.
                        // Thus, I'm using the "Welcome" message that the server sends to determine our outgoing port.

                        // Upon initial connection the server tells us our identity (IP + outgoing port)
                        if (serverAssignedIdentity.equals("NULL") && (!peer.serverIsSearchingNetwork)) {
                            serverAssignedIdentity = content;

                            // Send packet to inform the server of our listening address and port
                            //int peerListeningPort = peer.listeningPort;
                            ClientPackets.HostChange hostChange = new ClientPackets.HostChange(peer.serverIdentityListeningPort, peer.serverIP);
                            String msg = jWrite.buildHostChangeMsg(hostChange);

                            writer = new PrintWriter(peer.ToConnectedPeer, true);
                            writer.println(msg);
                            writer.flush();
                        }

                        // If it's our own message, prepend our room ID in front of our message.
                        if (incomingIdentity.equals(serverAssignedIdentity) && (!peer.serverIsSearchingNetwork)) {
                            String room = "[" + myCurrentRoom + "] ";
                            System.out.println(room + incomingIdentity + ": " + content);
                        }
                        // Case for when it's someone else's message.
                        else if (!peer.serverIsSearchingNetwork) {
                            System.out.println(ANSI_YELLOW+incomingIdentity+" says: "+ANSI_RESET+content);
                        }
                    }

                    // Adopted from A1 but heavily dumbed down.
                    else if (protocol.equals("roomlist")) {
                        ArrayList<String> rooms = jRead.getJSONRooms();
                        ArrayList<String> localRooms = new ArrayList<String>();

                        if (!peer.serverIsSearchingNetwork) {
                            //System.out.format(ANSI_YELLOW+"The peer you're connected to (%s) has the following rooms:%n"+ANSI_RESET, this.peer.connectedPeersIdentity);

                            // Print the room list from the server
                            if (rooms.isEmpty()) {
                                System.out.println("The peer you've connected to has no rooms currently created.");
                            }
                            else {
                                System.out.println("Rooms from server and their occupancies:");
                                for (String room : rooms) {
                                    String roomName = jRead.getJSONRoomName(room);
                                    String roomCount = jRead.getJSONRoomCount(room);
                                    System.out.println("\t- "+roomName + " currently has " + roomCount + " users.");
                                }
                            }

                            // If this peer is hosting rooms locally then we should also return their local room list.
                            //System.out.println(ANSI_YELLOW+"And the rooms you're locally hosting are:"+ANSI_RESET);
                            peer.getLocalRoomList();
                        }
                        else {
                            //System.out.println(rooms);
                            peer.neighborRooms.add(rooms);
                            for (String room : rooms) {
                                String roomName = jRead.getJSONRoomName(room);
                                String roomCount = jRead.getJSONRoomCount(room);
                                System.out.println("\t- "+roomName + " currently has " + roomCount + " users.");
                            }
                        }
                    }

                    // Used for when a peer joins another room.
                    else if (protocol.equals("roomchange")) {
                        String identity = jRead.getJSONIdentity();
                        String former = jRead.getJSONFormerIdentity();
                        String roomid = jRead.getJSONRoomId();

                        //System.out.println("---> peer.clientToQuit: "+peer.clientToQuit);

                        // The response of a quit command.
                        if (roomid.equals("") && (peer.clientToQuit)) {
                            if (!peer.serverIsSearchingNetwork) {
                                System.out.println("You have successfully disconnected from the host peer.");
                            }
                            peer.clientToQuit = false;
                            peer.destSocket.close();
                            peer.FromConnectedPeer.close();
                            peer.ToConnectedPeer.close();
                            getPeerMessages = false;
                            peer.connectionEstablishedWithServer = false;
                        }

                        else if (former.equals(roomid) && (!peer.clientToQuit)) {
                            System.out.println("The requested room is invalid or non existent.");
                        }

                        else if (former.equals("") && (!peer.clientToQuit)) {
                            System.out.println(identity + " moves to " + roomid);
                            myCurrentRoom = roomid;
                        }

                        else if (!former.equals("") && (roomid).equals("")) {
                            System.out.println("You have been moved to the null room as "+former+" has been deleted.");
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

                    // For when a peer gets kicked.
                    else if (protocol.equals("kick")) {
                        String message = jRead.getJSONKickMessage();
                        System.out.println("---> "+ANSI_RED+message+ANSI_RESET);
                        System.out.println("Disconnected from peer. Try and connect to another peer if you want.");
                        // System.out.println(peer.destSocket.isConnected());
                        //System.out.println(peer.destSocket.isClosed());
                        this.peer.destSocket.close();
//                        this.peer.ToConnectedPeer.close();
//                        this.peer.FromConnectedPeer.close();
                        //System.out.println(peer.destSocket.isClosed());
                        peer.connectionEstablishedWithServer = false;
                        //System.out.println("Sockets should be closed...");
                        getPeerMessages = false;
                    }

                    // The response of a #listneighbors command
                    else if (protocol.equals("neighbors")) {
                        ArrayList<String> peers = jRead.readListNeighbors();
                        if (!peer.serverIsSearchingNetwork) {
                            System.out.println("List of neighbors: " + peers);
                        }
                        // If peer searching network
                        else {
                            // Don't print anything out, just append it to our queue.
                            peer.neighborQueue.add(peers);
//                            System.out.println("peers: " + peers);
//                            for (String p : peers) {
//                                System.out.println("peer " + p);
//                            }

                        }
                    }

                    // The response of receiving a #migrationidentity JSON.
                    else if (protocol.equals("migrationidentity")) {
                        String migratingPeer = jRead.getJSONSender();
                        String peerToMigrate = jRead.getJSONIdentity();
                        String roomid = jRead.getJSONRoomId();
                        int totalIdentities = jRead.getJSONTotalIdentities();
                        System.out.println("---> We need to migrate to: "+jRead.getJSONIP()+":"+jRead.getJSONHost());
                        peer.destSocket.close();
                        peer.connectionEstablishedWithServer = false;
                        getPeerMessages = false;
                        try {
                            System.out.println("---> Attempting to connect to: " + jRead.getJSONIP() + ":" + jRead.getJSONHost());
                            peer.connectToPeer("localhost", Integer.parseInt(jRead.getJSONHost()), 0, true, jRead.getJSONRoomId());
                        } catch (Exception e) {
                            System.out.println("Error during re-migration.");
                        }
                    }

                    else if (protocol.equals("migrationsuccess")) {
                        String migrationWasSuccessful =  jRead.getJSONMigrationSuccess();
                        System.out.println(migrationWasSuccessful);
                        if (migrationWasSuccessful.equals("true")) {
                            peer.successfulMigrationProcedure();
                        }
                    }
                }

            } catch (IOException e) {
                System.out.println();
                System.out.println("Error occurred when retrieving message from the server.");
                System.out.println("You have been disconnected from the server.");
                peer.connectionEstablishedWithServer = false;
                System.out.println("You can try reconnecting to another peer.");
                //e.printStackTrace();
                //System.exit(0);
                break;
            }
        }
    }

}
