package com.kvoli;

import com.kvoli.base.ClientPackets;
import com.kvoli.base.JSONWriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class InputThread extends Thread {
    private Peer peer;
    private PrintWriter writer;
    private BufferedReader reader;
    private Socket socket;
    private boolean getUserInput = true;
    //private String clientID;

    public InputThread(Peer peer) {
        this.peer = peer;
        this.socket = peer.socket;
    }

    /**
     * Currently only one command
     * Usage: #connect PORTNUMBER
     * IP address is hardcoded to make testing easier.
     */

    @Override
    public void run() {
        System.out.println("Welcome. Please issue a command. ");
        while (getUserInput) {
            JSONWriter jWrite = new JSONWriter();
            String text = "";

            try {
                Scanner keyboard = new Scanner(System.in);
                text = keyboard.nextLine();
            } catch (Exception e) {
                System.out.println("InputThread exception when getting user input.");
            }

            // If we've established a connection with another peer ('server') then build a PrintWriter
            // We cannot build this without establishing a connection.
            if (peer.connectionEstablishedWithServer == true) {
                writer = new PrintWriter(peer.ToConnectedPeer, true);
            }


            // Command parsing
            if (text.contains("#connect")) {
                String input = text.replaceAll("#connect", "");
                input = input.stripLeading();

                // TODO: destination IP is currently hardcoded to make testing easier.
                String destIP = "localhost";
                int destPort = Integer.parseInt(input);

                System.out.println("DEBUG: Trying to connect to " + destIP + " " + destPort);
                peer.connectToPeer(destIP, destPort);

            }

            else if (text.contains("#list")) {
                ClientPackets.List listRoom = new ClientPackets.List();
                this.peer.clientListCmdStatus = true;

                // If we're not connected to anybody we should be able to issue a local list command to ourselves
                // to reveal the rooms that we are currently maintaining.
                if (!peer.connectionEstablishedWithServer) {
                    peer.getLocalRoomList();
                }
                else {
                    String msg = jWrite.buildListMsg(listRoom);
                    writer.println(msg);
                    writer.flush();
                }
            }

            else if (text.contains("#create")) {
                String input = text.replaceAll("#create", "");
                input = input.stripLeading();

                peer.createLocalRoom(input, peer.serverIdentity);

            }

            else if (text.contains("#join")) {
                String input = text.replaceAll("#join", "");
                input = input.stripLeading();

                // We don't need to be connected to someone else to join our own room.
                if (!peer.connectionEstablishedWithServer) {
                    peer.joinLocalRoom(peer.clientCurrentRoom, input);

                }

                else {
                    // Otherwise
                    ClientPackets.Join joinRoom = new ClientPackets.Join(input);
                    String msg = jWrite.buildJoinMsg(joinRoom);
                    writer.println(msg);
                    writer.flush();
                }
            }

            else if (text.contains("#who") && (peer.connectionEstablishedWithServer)) {
                String input = text.replaceAll("#who", "");
                input = input.stripLeading();
                ClientPackets.Who who = new ClientPackets.Who(input);
                String msg = jWrite.buildWhoMsg(who);
                writer.println(msg);
                writer.flush();
            }

            // Input is not a command therefore it must be a message.
            else {
                // Condition for if we're connected to the 'server' peer
                if (!text.equals("") && peer.connectionEstablishedWithServer) {
                    // Wrap this input into JSON.
                    ClientPackets.Message message = new ClientPackets.Message(text);
                    String msg = jWrite.buildMessage(message);
                    //System.out.println("SENT: " + msg);
                    writer.println(msg);
                    writer.flush();
                }

                // Condition for if WE are the 'server' peer, hence we are not connected to a 'server'.
                else if (!text.equals("")) {
                    peer.broadcastAsServer(text);
                }
            }


        }


    }


}
