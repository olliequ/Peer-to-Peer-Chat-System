package com.kvoli;

import com.kvoli.base.ClientPackets;
import com.kvoli.base.JSONWriter;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class InputThread extends Thread {
    private Peer peer;
    private PrintWriter writer;
    private BufferedReader reader;
    private Socket socket;
    private boolean getUserInput = true;
    //private String clientID;
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_RESET = "\u001B[0m";

    public InputThread(Peer peer) {
        this.peer = peer;
        this.socket = peer.socket;
    }

    /**
     * To connect to another peer type #connect <PORT>
     */

    @Override
    public void run() {
        System.out.println("----------------\n"+ANSI_BLUE+"Welcome! Please issue a command."+ANSI_RESET);
        while (getUserInput) {
            JSONWriter jWrite = new JSONWriter();
            String text = "";

            try {
                Scanner keyboard = new Scanner(System.in);
                text = keyboard.nextLine();
            } catch (Exception e) {
                System.out.println("InputThread exception when getting user input.");
            }

            // If we've established a connection with another peer ('server') then build a PrintWriter.
            // We cannot build this without establishing a connection.
            if (peer.connectionEstablishedWithServer) {
                writer = new PrintWriter(peer.ToConnectedPeer, true);
            }

            // Command parsing
            if (text.contains("#connect")) {
                String input = text.replaceAll("#connect", "");
                input = input.stripLeading();
                String destIP = "localhost";   // TODO: destination IP is currently hardcoded to make testing easier.
                if (input == "") {
                    System.out.println("You need to enter an IP address and Port Number. You can't connect to nothing!");
                }
                else {
                    try {
                        int destPort = Integer.parseInt(input);
                        System.out.println("---> Attempting to connect to: " + destIP + " " + destPort);
                        peer.connectToPeer(destIP, destPort, 0);
                        System.out.println(ANSI_GREEN+"Connection successful."+ANSI_RESET);
                    } catch (Exception e) {
                        System.out.println("Invalid input (parseInt error)");
                    }
                }
            }

            else if (text.equals("#list")) {
                ClientPackets.List listRoom = new ClientPackets.List();
                this.peer.clientListCmdStatus = true;

                // If we're not connected to anybody we should be able to issue a local list command to ourselves
                // to reveal the rooms that we are currently maintaining.
                if (!peer.connectionEstablishedWithServer) {
                    peer.getLocalRoomList();
                }
                else {
                    String msg = jWrite.buildListMsg(listRoom);
                    System.out.format(ANSI_BLUE+"Sending #list JSON:"+ANSI_RESET+" %s%n", msg);
                    writer.println(msg);
                    writer.flush();
                }
            }


            // Peer can ping the server it is connected to and ask for a list of other people connected to the server.
            else if (text.contains("#listneighbors") && (peer.connectionEstablishedWithServer)) {
                ClientPackets.ListNeighbors listN = new ClientPackets.ListNeighbors();
                String msg = jWrite.buildListNeighborsMsg(listN);
                //System.out.format(ANSI_BLUE+"Sending #listneighbors JSON:"+ANSI_RESET+" %s%n", msg);
                writer.println(msg);
                writer.flush();
            }


            else if (text.contains("#create")) {
                String input = text.replaceAll("#create", "");
                input = input.stripLeading();
                peer.createLocalRoom(input, peer.serverIdentityInetAddress.toString());

            }

            else if (text.contains("#join")) {
                String input = text.replaceAll("#join", "");
                input = input.stripLeading();

                // We don't need to be connected to someone else to join our own room.
                if (!peer.connectionEstablishedWithServer) {
                    peer.joinLocalRoom(peer.clientCurrentRoom, input);

                }
                else {
                    ClientPackets.Join joinRoom = new ClientPackets.Join(input);
                    String msg = jWrite.buildJoinMsg(joinRoom);
                    System.out.format(ANSI_BLUE+"Sending #join JSON:"+ANSI_RESET+" %s%n", msg);
                    writer.println(msg);
                    writer.flush();
                }
            }

            else if (text.contains("#who") && (peer.connectionEstablishedWithServer)) {
                String input = text.replaceAll("#who", "");
                input = input.stripLeading();
                ClientPackets.Who who = new ClientPackets.Who(input);
                String msg = jWrite.buildWhoMsg(who);
                System.out.format(ANSI_BLUE+"Sending #who JSON:"+ANSI_RESET+" %s%n", msg);
                writer.println(msg);
                writer.flush();
            }


            else if (text.contains("quit") && (peer.connectionEstablishedWithServer)) {
                String input = text.replaceAll("#quit", "");
                input = input.stripLeading();
                peer.clientToQuit = true;
                ClientPackets.Quit quitMsg = new ClientPackets.Quit();
                String msg = jWrite.buildQuitMsg(quitMsg);
                System.out.format(ANSI_BLUE+"Sending #quit JSON:"+ANSI_RESET+" %s%n", msg);
                writer.println(msg);
                writer.flush();
            }

            // Local command that allows the peer to find all the peers available to it
            else if (text.contains("search")) {
                try {
                    peer.searchNetwork();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Local command that begins the Room Migration process.
            else if (text.contains("migrate")) {
                String input = text.replaceAll("#migrate", "");
                input = input.stripLeading();

                // TODO: Hardcoded to make testing easier
                String rooms = "all";
                String hostIP = "0.0.0.0";
                int hostListenPort = Integer.parseInt(input);

                peer.sendMigration(rooms, hostIP, hostListenPort);


            }

            // Input is not a command therefore it must be a message.
            else {
                // DEBUGGING SEARCHNETWORK
//                System.out.println("Queue for this peer. ");
//                for (ArrayList<String> x: peer.neighborQueue) {
//                    for (String y : x) {
//                        System.out.println(y);
//                    }
//                }




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
