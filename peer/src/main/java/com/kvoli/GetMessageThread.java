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
                        System.out.println(incomingIdentity + ": " + content);
                    }

                    // Adopted from A1 but heavily dumbed down.
                    else if (protocol.equals("roomlist")) {
                        ArrayList<String> rooms = jRead.getJSONRooms();
                        ArrayList<String> localRooms = new ArrayList<String>();

                        for (String room : rooms) {
                            String roomName = jRead.getJSONRoomName(room);
                            String roomCount = jRead.getJSONRoomCount(room);
                            System.out.println("Room: " + roomName + " with " + roomCount + " users.");
                        }
                    }

                    else if (protocol.equals("roomchange")) {
                        String identity = jRead.getJSONIdentity();
                        String former = jRead.getJSONFormerIdentity();
                        String roomid = jRead.getJSONRoomId();
                        boolean validChange = false;

                        if (former.equals("")) {
                            System.out.println(identity + " moves to " + roomid);
                            validChange = true;
                        }
                        else {
                            System.out.println(identity + " moved from " + former + " to " + roomid);
                        }
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
