package com.kvoli;

import com.kvoli.base.ClientPackets;
import com.kvoli.base.JSONWriter;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class InputThread extends Thread {
    private Peer peer;
    private PrintWriter writer;
    private BufferedReader reader;
    // private Socket socket;
    private boolean getUserInput = true;
    //private String clientID;
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_RESET = "\u001B[0m";

    public InputThread(Peer peer) {
        this.peer = peer;
        // this.socket = peer.socket;
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
                String destIP = "localhost";   // TODO: destination IP is currently hardcoded to make testing easier.
                String input = text.replaceAll("#connect", "");
                input = input.stripLeading();
                String[] connectArguments = input.split("\\s+");
                List<String> connectArgumentsAL = Arrays.asList(connectArguments);

                int destinationPort = Integer.parseInt(input);
                System.out.println("---> Attempting to connect to: " + destIP + " " + destinationPort);
                peer.connectToPeer(destIP, destinationPort, 0, false, "");

//                if (connectArgumentsAL.get(0).equals("")) {
//                    System.out.println("You need to enter an IP address and Port Number. You can't connect to nothing!");
//                }
//                else if (connectArgumentsAL.size()>2) {
//                    System.out.println("Too many arguments supplied.");
//                }
//                else if (connectArgumentsAL.size()==1) {
//                    String[] firstArgument = connectArgumentsAL.get(0).split(":");
//                    // Only IP provided and nothing else.
//                    if (firstArgument.length == 1) {
//                        System.out.println("---> Attempting to connect to: " + firstArgument[0]);
//                        peer.connectToPeer(firstArgument[0], 4444, 0, false, "");
//                    }
//                    // Only IP and its port provided. Outgoing port not provided.
//                    else {
//                        System.out.println("---> Attempting to connect to: " + firstArgument[0] + ":" + firstArgument[1]);
//                        peer.connectToPeer(firstArgument[0], Integer.parseInt(firstArgument[1]), 0, false, "");
//                    }
//                }
//                else {
//                    String[] firstArgument = connectArgumentsAL.get(0).split(":");
//                    // IP of peer supplied (but not a port) and an outgoing port.
//                    if (firstArgument.length == 1 && connectArgumentsAL.size() == 2) {
//                        System.out.println("---> Attempting to connect to: " + firstArgument[0]+" on outgoing port: "+connectArgumentsAL.get(1));
//                        peer.connectToPeer(firstArgument[0], 4444, Integer.parseInt(connectArgumentsAL.get(1)), false, "");
//                    }
//                    // IP of peer supplied, its port, and an outgoing port.
//                    else {
//                        int destPort = Integer.parseInt(input);
//                        System.out.println("---> Attempting to connect to: " + destIP + ":" + destPort+" on outgoing port "+connectArgumentsAL.get(1));
//                        peer.connectToPeer(firstArgument[0], Integer.parseInt(firstArgument[1]), Integer.parseInt(connectArgumentsAL.get(1)), false, "");
//                    }
//                }
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
                    // System.out.format(ANSI_BLUE+"Sending #list JSON:"+ANSI_RESET+" %s%n", msg);
                    writer.println(msg);
                    writer.flush();
                }
            }

            // Peer can ping the server it is connected to and ask for a list of other people connected to the server.
            else if (text.contains("#listneighbors")) {
                 if (peer.connectionEstablishedWithServer) {
                     ClientPackets.ListNeighbors listN = new ClientPackets.ListNeighbors();
                     String msg = jWrite.buildListNeighborsMsg(listN);
                     //System.out.format(ANSI_BLUE+"Sending #listneighbors JSON:"+ANSI_RESET+" %s%n", msg);
                     writer.println(msg);
                     writer.flush();
                 }
                 else {
                     System.out.println("You're not connected to anyone, and thus cannot retrieve a given peer's neighbours!");
                 }
            }

            else if (text.contains("#create")) {
                String input = text.replaceAll("#create", "");
                input = input.stripLeading();
                peer.createLocalRoom(input, peer.serverIdentityInetAddress.toString());
            }

            else if (text.contains("#delete")) {
                String input = text.replaceAll("#delete", "");
                input = input.stripLeading();
                if (peer.connectionEstablishedWithServer) {
                    System.out.println("You can't issue a #delete if you're connected to a remote peer.");
                }
                else {
                    peer.deleteLocalRoom(input);
                }
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
                    // System.out.format(ANSI_BLUE+"Sending #join JSON:"+ANSI_RESET+" %s%n", msg);
                    writer.println(msg);
                    writer.flush();
                }
            }

            else if (text.contains("#who") && (peer.connectionEstablishedWithServer)) {
                String input = text.replaceAll("#who", "");
                input = input.stripLeading();
                ClientPackets.Who who = new ClientPackets.Who(input);
                String msg = jWrite.buildWhoMsg(who);
                // System.out.format(ANSI_BLUE+"Sending #who JSON:"+ANSI_RESET+" %s%n", msg);
                writer.println(msg);
                writer.flush();
            }

            else if (text.contains("#kick")) {
                String peerToKick = text.replaceAll("#kick", "");
                peerToKick = peerToKick.stripLeading();
                peer.kickPeer(peerToKick);
            }

            else if (text.contains("#listpeers")) {
                peer.displayConnectedPeers();
            }

            else if (text.contains("quit") && (peer.connectionEstablishedWithServer)) {
                String input = text.replaceAll("#quit", "");
                input = input.stripLeading();
                peer.clientToQuit = true;
                ClientPackets.Quit quitMsg = new ClientPackets.Quit();
                String msg = jWrite.buildQuitMsg(quitMsg);
                //System.out.format(ANSI_BLUE+"Sending #quit JSON:"+ANSI_RESET+" %s%n", msg);
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
                String[] portAndRooms = input.split("\\s+");
                String[] roomArray = Arrays.copyOfRange(portAndRooms, 1, portAndRooms.length);
//                for (String room : roomArray) {
//                    System.out.println(room);
//                }
                // TODO: Hardcoded to make testing easier
                String hostIP = "0.0.0.0";
                int hostListenPort = Integer.parseInt(portAndRooms[0]);

                // No room arguments supplied to the #migrate command. This is unaccepted.
                if (roomArray.length == 0) {
                    System.out.println(ANSI_RED+"You must specify rooms or write 'all'."+ANSI_RESET);
                }
                // One can't write 'all' and then also specify rooms to migrate.
                else if (roomArray[0].equals("all") && roomArray.length != 1) {
                    System.out.println(ANSI_RED+"You can't write 'all' and then also specify rooms."+ANSI_RESET);
                }
                // Accepted input arguments, so let's call the migration method.
                else {
                    try {
                        peer.sendMigration(hostIP, hostListenPort, roomArray);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            else if (text.contains("#help")) {
                System.out.println(ANSI_CYAN+"The following commands are available to you:\n"+ANSI_RESET+
                        "- #connect IP[:port] [local port]: Connect to another peer. You can specify a port to connect to, and off of locally.\n- #join: Join a room." +
                        "\n- #create room: Create a room locally.\n- #list: Retrieve both a local and a global list of rooms.\n- #who room: See who is in a specific room." +
                        "\n- #kick peer: Kick a peer connected to you.\n- #migrate IP[:port] all || #migrate IP[:port] [room#1]...[room#n]: Migrate rooms you're hosting (and any peers inside them) to another peer. " +
                        "\n\tSpecifying the 'all' flag will migrate all rooms you're hosting, otherwise you can choose specific rooms to migrate." +
                        "\n- #quit: Disconnect from a peer you're connected to.\n- #delete room: Delete a room you're hosting.\n- #ListNeighbors: See who else is connected to the peer you're connected to." +
                        "\n- #SearchNetwork: Retrieve all rooms and who's in them from a chain of peers in a network you're a part of.\n- #help: Lists all available commands you can use.");
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
