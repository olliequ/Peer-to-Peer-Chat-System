package com.kvoli;

import com.kvoli.base.*;
import org.w3c.dom.ls.LSOutput;


import java.io.*;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.StandardSocketOptions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Peer {
  private PrintWriter writer;

  // protected Socket socket;
  // Destination (server) socket (i.e. the peer that we have connected to).
  protected Socket destSocket;
  // Input and output streams from the peer (server) that we are connected to.
  protected OutputStream ToConnectedPeer;
  protected InputStream FromConnectedPeer;
  private BufferedReader reader;

  // All peers can be clients. These variables are used by InputThread and GetMessageThread
  protected boolean clientListCmdStatus = true;
  protected boolean connectionEstablishedWithServer = false;      // True when we are connected to another peer.
  protected String clientCurrentRoom;
  protected boolean clientToQuit = false;                         // When we want to #quit we change this value.

  // All peers can be servers. A peer must establish their own server identity.

  protected InetAddress serverIdentityInetAddress;
  protected int serverIdentityListeningPort;
  protected String serverIdentity;
  protected String serverIP;
  protected String connectedPeersIdentity;
  protected boolean serverIsSearchingNetwork = false;
  protected boolean serverIsGettingRooms = false;

  // Used for searchNetwork()
  protected List<ArrayList<String>> neighborQueue = new ArrayList<ArrayList<String>>();
  protected List<ArrayList<String>> neighborRooms = new ArrayList<ArrayList<String>>();

  private boolean acceptConnections = false;

  // As a server we maintain a list of connections and a list of rooms we are aware of.
  private volatile List<ServerConnection> currentConnections = new ArrayList<>();
  private volatile List<Room> currentRooms = new ArrayList<>();
  private volatile List<String> bannedPeers = new ArrayList<>();

  public static final String ANSI_RED = "\u001B[31m";
  public static final String ANSI_BLUE = "\u001B[34m";
  public static final String ANSI_CYAN = "\u001B[36m";
  public static final String ANSI_GREEN = "\u001B[32m";
  public static final String ANSI_YELLOW = "\u001B[33m";
  public static final String ANSI_RESET = "\u001B[0m";

  public final int outgoingPort = 54000;
  // Old code from A1
  public static int PORT = 4444;


  // Used for Room Migration feature
  boolean migrationInProgress = false;
  private volatile List<String> migratedRooms = new ArrayList<>();
  private volatile List<String> migratedIdentities = new ArrayList<>();


  // Currently in use by main
  public Peer() {}

  // Not in use at the moment.
  public Peer(int port) {
    this.PORT = port;
  }


  /** Handle Method
   * Spawns an InputThread for the 'client' (similar to SendMessageThread).
   * Spawns an infinite while loop to accept incoming connections. Assign identities to these connections from their
   * socket information.
   */
  protected void handle() {
    // Spawn thread to handle the peers input (user input)
    new InputThread(this).start();

    // Handle incoming connections to this peer.
    ServerSocket serverSocket;
    try {
      // Bind serverSocket to a port. We're using port 0 so that the OS will pick a random port.
      serverSocket = new ServerSocket(0); // This is the socket that we listen on to receive incoming connections.
      serverSocket.setOption(StandardSocketOptions.SO_REUSEPORT, true);
      serverSocket.setOption(StandardSocketOptions.SO_REUSEADDR, true);
      //serverSocket.setReuseAddress(true);
      acceptConnections = true;

      // From my understanding there are two ports. An INCOMING and an OUTGOING port.
      // This is the peers INCOMING port.
      System.out.printf("Listening to incoming peer connections on port "+ANSI_YELLOW+"%d.\n"+ANSI_RESET, serverSocket.getLocalPort());
      System.out.println("This peers IP address is: " + serverSocket.getInetAddress().getHostAddress());

      // All peers can be 'servers'. We need to establish our own identity. TODO: Unsure if this is correct.
      serverIdentityInetAddress = serverSocket.getInetAddress();        // 0.0.0.0\0.0.0.0
      serverIP = serverSocket.getInetAddress().getHostAddress();    // 0.0.0.0
      serverIdentityListeningPort = serverSocket.getLocalPort();
      serverIdentity = serverIP + ":" + serverIdentityListeningPort;
      System.out.println("This peer's identity is: "+ANSI_YELLOW+ serverIdentityInetAddress +ANSI_RESET+"\n----------------");

      // Testing purposes: create a test room
      clientCurrentRoom = "";
      //currentRooms.add(new Room("Test Room", serverIdentityInetAddress.toString()));

      // The peers port will accept incoming connections within an infinite loop.
      while (acceptConnections) {
        // Accepted a connection from a peer. Generate new socket based off the encompassing ServerSocket -- accept it.
        Socket socket = serverSocket.accept();
        socket.setOption(StandardSocketOptions.SO_REUSEPORT, true);
        socket.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        // socket.setOption(StandardSocketOptions.SO_LINGER, 1);
        // Note that the port number we received is the clients OUTGOING port.
        System.out.println(ANSI_CYAN+"---> Accepted connection from another peer who's using their port number: "+ANSI_RESET+ socket.getPort());

        // The connected peer's identity is a combination of their IP address and their outgoing port number
        String addressOfPeer = socket.getInetAddress().toString();
        int portOfPeer = socket.getPort();
        String clientIdentity = addressOfPeer + ':' + portOfPeer;
        System.out.println("\t- The identity of the peer that just connected to you is: " + clientIdentity);
        System.out.format("\t- Connected off of you on: %s:%d%n", socket.getLocalAddress(), socket.getLocalPort());
        for (String a : bannedPeers) {
          System.out.format("Banned peer: %s%n", a);
        }
        if (bannedPeers.contains(clientIdentity)) {
          System.out.println(ANSI_RED+"---> Oh wait -- this is a banned peer! It is not allowed to join us. Telling it to go away now."+ANSI_RESET);
          PrintWriter writerr = new PrintWriter(socket.getOutputStream());
          JSONWriter jsonBuilda = new JSONWriter();
          String message = "What did I tell you? You're banned. Stop trying to connect here.";
          ClientPackets.Kick kickthing = new ClientPackets.Kick(message);
          String bannedMessage = jsonBuilda.buildKickMsg(kickthing);
          writerr.println(bannedMessage);
          writerr.flush();
          socket.close();
        }
        else {
          // Each peer that connects to this peer will have its own thread of execution.
          // The connection will be able to handle itself.
          ServerConnection currentConnection = new ServerConnection(socket, clientIdentity, "");
          currentConnection.start();
          connect(currentConnection);
        }
      }

    } catch (IOException e) {
      System.out.println("Network exception occurred.");
      e.printStackTrace();
    }
  }

  // TIP: Use ALT + 7 in IJ to view all methods on the sidebar.
  // ***************************************************************************************************************
  // ***************************************************************************************************************
  // ***********************************     NEW METHODS FOR ASSIGNMENT 2     **************************************
  // ***************************************************************************************************************

  /**
   * Used by the connect command from InputThread. Allows a user to connect to another peer.
   * NEW IN A2
   * @param destIP              Currently using 'localhost'. TODO: Unsure if this is correct.
   * @param destPort            Port number of the peer we want to connect to.
   */
  protected synchronized void connectToPeer(String destIP, int destPort, int outGooingPort, boolean peerRemigratingOrNot, String remigrationRoom) {
    try {
      // Attempt to establish connection to the destination IP and Port
      // System.out.format("Connected to: %s, %d%n", this.destSocket.getInetAddress(), this.destSocket.getPort());
      this.destSocket = new Socket(destIP, destPort, serverIdentityInetAddress, outGooingPort);
      this.destSocket.setOption(StandardSocketOptions.SO_REUSEPORT, true);
      this.destSocket.setOption(StandardSocketOptions.SO_REUSEADDR, true);
      this.ToConnectedPeer = destSocket.getOutputStream();
      this.FromConnectedPeer = destSocket.getInputStream();
      this.reader = new BufferedReader(new InputStreamReader(FromConnectedPeer));
      new GetMessageThread(this).start();
      //System.out.format("\t- Connected to: %s:%d%n", this.destSocket.getInetAddress(), this.destSocket.getPort());
      //System.out.format("\t- Connected from: %s:%d%n", this.destSocket.getLocalAddress(), this.destSocket.getLocalPort());
      connectionEstablishedWithServer = true;
      connectedPeersIdentity = this.destSocket.getInetAddress()+":"+this.destSocket.getPort();

      if (peerRemigratingOrNot) {
          JSONWriter jWrite = new JSONWriter();
          ClientPackets.Join joinRoom = new ClientPackets.Join(remigrationRoom);
          String msg = jWrite.buildJoinMsg(joinRoom);
          // System.out.format(ANSI_BLUE+"Sending #join JSON:"+ANSI_RESET+" %s%n", msg);
          writer = new PrintWriter(ToConnectedPeer);
          // System.out.println(this.destSocket.isClosed());
          writer.println(msg);
          writer.flush();
      }

    } catch (IOException e) {
      System.out.println("Couldn't connect to peer.");
      e.printStackTrace();
    }
  }


  protected synchronized void sendMigration(String hostIP, int hostListenPort, String[] roomArray) throws InterruptedException {
    // First connect to the new host
    connectToPeer(hostIP, hostListenPort, 0, false, "");
    writer = new PrintWriter(ToConnectedPeer, true);
    JSONWriter jsonBuild = new JSONWriter();
    List<String> roomArrayList = Arrays.asList(roomArray); // ArrayList of rooms we need to migrate.
    String sender = serverIdentity;

    // User has specified to migrate all rooms over.
    if (roomArray[0].equals("all") && roomArray.length == 1) {
      int totalRooms = currentRooms.size();
      int totalIdentities = currentConnections.size();
      System.out.format("There are %d rooms, and %d peers to migrate.%n", totalRooms, totalIdentities);
      // Iterate through each room in currentRooms
      for (Room r: currentRooms) {
        /**
         * Send a JSON string of the below format
         * 'sender' : 'sender IP/port'
         * 'roomName' : 'name'
         * 'totalRooms' : number    <- max number of rooms we are sending. Peer 2 will keep track of this.
         *  When peer 2 has received all rooms it will reply with OK (later in code)
         */
        String roomName = r.getRoomName();
        String serverMessage = jsonBuild.buildJSONMigrationRoom(sender, roomName, totalRooms);
        // Send message to the new host
        writer.println(serverMessage);
        writer.flush();
      }

      int peersToMigrate = 0;
      for (Room r: currentRooms ) {
          peersToMigrate += r.getRoomSize();
      }
      System.out.println("Peers to migrate: "+peersToMigrate);
      Thread.sleep(10000); // Sleep necessary so that rooms are received and built on the receiving peer BEFORE the other peers migrate to it and request to join to the rooms.
      for (ServerConnection c : currentConnections) {
        // Send the following JSON string
        // 'sender' : 'sender IP/port'
        // 'identity' : 'IP/port'
        // 'roomName' : 'name'
        // 'totalIdentities': number
        if (!c.roomID.equals("")) {
          String serverMessage = jsonBuild.buildJSONMigrationIdentity(hostIP, hostListenPort, sender, c.identity, c.roomID, peersToMigrate);
          c.sendMessage(serverMessage+ "\n");
          c.close(); // May be able to delete this.
        }
      }

      // Quit
      clientToQuit = true;
      ClientPackets.Quit quitMsg = new ClientPackets.Quit();
      String serverMessage = jsonBuild.buildQuitMsg(quitMsg);
      writer.println(serverMessage);
      writer.flush();
    }

    // Else, the peer has specified specific rooms to migrate over. The remaining unspecified rooms are lost I assume...?
    else {
      for (String a : roomArray) {
        for (Room r: currentRooms) {
          String roomName = r.getRoomName();
          if (roomName.equals(a)) {
            String serverMessage = jsonBuild.buildJSONMigrationRoom(sender, roomName, roomArray.length);
            writer.println(serverMessage);
            writer.flush();
          }
        }
      }

      // TODO: Iterate through each connection in currentConnections
      int peersToMigrate = 0;
      for (Room r: currentRooms ) {
        if (roomArrayList.contains(r.getRoomName())) {
          peersToMigrate += r.getRoomSize();
        }
      }
      System.out.println("Peers to migrate: "+peersToMigrate);
      Thread.sleep(10000); // Sleep necessary so that rooms are received and built on the receiving peer BEFORE the other peers migrate to it and request to join to the rooms.
      for (ServerConnection c : currentConnections) {
        if (roomArrayList.contains(c.roomID)) {
          String serverMessage = jsonBuild.buildJSONMigrationIdentity(hostIP, hostListenPort, sender, c.identity, c.roomID, peersToMigrate);
          c.sendMessage(serverMessage+ "\n");
          c.close();
        }
      }

      // Quit as now all migrations have been made.
      clientToQuit = true;
      ClientPackets.Quit quitMsg = new ClientPackets.Quit();
      String serverMessage = jsonBuild.buildQuitMsg(quitMsg);
      writer.println(serverMessage);
      writer.flush();
    }
  }


  /**
   * Used for Extended Feature 1: Room Migration
   * Receive incoming values and construct new rooms when appropriate. These rooms will house the new identities that
   * are migrating to us from the old host.
   *
   * @param sender            incoming from sender - the host of the room
   * @param roomName          incoming from sender - name of current room being sent
   * @param totalRooms        incoming from sender - total number of rooms the host is sending to us
   */
  protected synchronized void handleMigratedRooms(String sender, String roomName, int totalRooms) {
    // Lock the migration feature for this host. We don't want some other random peer
    // to perform a concurrent migration on us while we're dealing with the existing sender.
    migrationInProgress = true;               // To be unlocked by handleMigratedIdentities when it finishes.


    // Add this room to our list.
    migratedRooms.add(roomName);

    // First check if we have received all rooms from the sender (former host).
    if (migratedRooms.size() == totalRooms) {
      // Begin construction of all the rooms
      System.out.println("---> All rooms received. Begin construction:");
      for (String room : migratedRooms) {
        boolean roomAlreadyExists = false;

        // What if we already have a room with the same name? In that case, do not create the room.
        for (Room r: currentRooms) {
          if (r.getRoomName().equals(room)) {
            System.out.println("Host Room: " + room + " equals my Room: " + r.getRoomName());
            roomAlreadyExists = true;
          }
        }

        // If the room doesn't already exist then we can safely create it.
        if (!roomAlreadyExists) {
          currentRooms.add(new Room(room, serverIdentity));
          System.out.println(ANSI_GREEN+"- Created room " + room + ". New owner: " + serverIdentity+ANSI_RESET);
        }
      }
    }
    else {
      // We haven't received all rooms from the sender. We need to receive all rooms before we can begin construction.
      System.out.println("---> Construction of rooms not commencing as not all rooms have been received yet.");
    }
  }


  /**
   *
   * @param sender            incoming from sender - the host of the room
   * @param identity          incoming from sender - name of the peer
   * @param roomName          incoming from sender - name of current room being sent
   * @param totalIdentities   incoming from sender - total number of identities the host is sending to us
   */
  protected synchronized void handleMigratedIdentities(String sender, String identity, String roomName, int totalIdentities) {



    // Upon completion, unlock migrationInProgress.
  }





  /**
   *  The peer should crawl over all peers accessible in the network using a breadth-first recursion strategy. We connect to
   *  each peer (using a separate connection to the peer's existing one -- if there is one that is) and find the rooms
   *  available in each peer using a #List command and also other peers to search (connect to) using #ListNeighbors.
   *  We connect to the first peer that is connected to us (e.g. peer B), then we ask peer B to hand over all the peers
   *  that are connected to it. TODO: Don't forget we have to use BFS. So, search each peer 1 peer away, then 2 away etc.
   */
  protected synchronized void searchNetwork() throws InterruptedException {
    // Enabling this causes roomlist in GetMessageThread to save list information to an arraylist.
    serverIsSearchingNetwork = true;

    // Store neighbors
    List<ArrayList<String>> peerData = new ArrayList<ArrayList<String>>();
    // ArrayList containing the #listneighbors from each peer
    ArrayList<String> tempList = new ArrayList<String>();

    ArrayList<String> peerInfo = new ArrayList<String>();

    // Enqueue our current connections to our queue
    System.out.println("DEBUG 1: CONSTRUCTING LIST");
    for (ServerConnection c : currentConnections) {
      String address = c.ipAddress + ":" + c.listenPort;
      tempList.add(address);
    }
    neighborQueue.add(tempList);
    peerData.add(tempList);


    // While the queue is not empty
    System.out.println("DEBUG 2: ENTERING WHILE LOOP");
    while (neighborQueue.size() > 0) {
      for (ArrayList<String> peers : neighborQueue) {
        for (String peer : peers) {
          // Peer addresses are in the format IP:Port
          // Extract both IP and Port from the string
          String[] address = peer.split(":");                           // [0] = IP, [1] = port

          // Connect to peer
          connectToPeer(address[0], Integer.parseInt(address[1]), 0, false, "");
          writer = new PrintWriter(ToConnectedPeer, true);
          JSONWriter jWrite = new JSONWriter();

          // Ask this peer to hand over its rooms via the List command.
          ClientPackets.List listRoom = new ClientPackets.List();
          String msg = jWrite.buildListMsg(listRoom);
          writer.println(msg);
          writer.flush();

          // Ask this peer to hand over its neighbours via the ListNeighbours request.
          ClientPackets.ListNeighbors listN = new ClientPackets.ListNeighbors();
          msg = jWrite.buildListNeighborsMsg(listN);
          writer.println(msg);
          writer.flush();

          // Send a quit message
          clientToQuit = true;
          ClientPackets.Quit quitMsg = new ClientPackets.Quit();
          msg = jWrite.buildQuitMsg(quitMsg);
          writer.println(msg);
          writer.flush();
        }
      }

      // Dequeue
      neighborQueue.remove(0);

      // IMPORTANT. Need to sleep to allow the input thread to read whatever we flushed above.
      TimeUnit.MILLISECONDS.sleep(100);

      // Store the identity of the peers neighbor and the rooms that they are hosting
      for (ArrayList<String> peers: neighborQueue) {
        // Add the initial neighbor
        if (peers.size() > 0) {
          for (String x : tempList) {
            peerInfo.add(x);
          }
          tempList = new ArrayList<String>();             // Reset
        }

        for (String peer : peers) {
          System.out.println(peer);
          peerInfo.add(peer);
        }
      }
    }

    // TODO: PROBLEM: PEERS AND ROOMS ARE OUT OF SYNC...
    System.out.println("DEBUG 3: PRINTING FINAL OUTPUT");
    for (String peer: peerInfo) {
      System.out.println(peer);
    }

    System.out.println("Rooms");
    for (ArrayList<String> x: neighborRooms) {
      System.out.println(x);
    }



//    System.out.println("DEBUG 3: PRINTING FINAL OUTPUT");
//    for (ArrayList<String> peers: peerData) {
//      for (String peer : peers) {
//        System.out.println(peer);
//      }
//    }
//
//    System.out.println("Rooms");
//    for (ArrayList<String> x: neighborRooms) {
//      System.out.println(x);
//    }
  }


  /**
   * If a server is acting as a client in their own room (i.e they type a message) then we need to broadcast it to
   * the other users.
   * We can't use the original broadcastRoom method from A1 because a server peer doesn't have a ServerConnection to themself.
   * NEW IN A2
   */
  protected synchronized void broadcastAsServer(String message) {
    String identity = serverIdentityInetAddress.toString();

    for (ServerConnection c: currentConnections) {
      // Ensure rooms match and that the room is not the default empty string.
      if (c.roomID.equals(clientCurrentRoom) && (c.roomID.length() > 1)) {
        JSONWriter jsonBuild = new JSONWriter();
        String serverMessage = jsonBuild.buildJSON(message, identity);
        //System.out.format("%nSending "+"JSON string(s). Check below:%n");
        //System.out.println("BroadcastRoom JSON: " + serverMessage);
        // Now broadcast the JSON string to everyone in the room.
        c.sendMessage(serverMessage + "\n");
      }
    }
  }



  /**
   * Method to get local room list.
   * This is used for when we're not connected to anybody but we're hosting a few rooms of our own.
   * NEW IN A2
   */
  protected synchronized void getLocalRoomList() {
    if (currentRooms.isEmpty()) {
      System.out.println("There are no rooms currently locally existing. #create one to get started.");
    }
    else {
      System.out.println("Below are the rooms you are currently hosting locally:");
      for (Room r: currentRooms) {
        System.out.println("\t- "+r.getRoomName() + " currently has " + r.getRoomSize() + " users.");
      }
    }
  }



  /**
   * Method to allow a peer to join local rooms when NOT connected to a remote peer.
   * For the purposes of this project we need to have two join room methods since a peer can be a server/client.
   * If I am not connected to someone I still need to be able to "connect" to a room that ive created.
   * NEW IN A2
   *
   * @param oldRoom
   * @param newRoom
   */
  protected synchronized void joinLocalRoom(String oldRoom, String newRoom) {
    JSONWriter jsonBuild = new JSONWriter();

    String identity = serverIdentityInetAddress.toString();

    // First check if the new 'room' is even valid.
    boolean newRoomIsValid = false;
    for (Room r: currentRooms) {
      if (r.getRoomName().equals(newRoom)) {
        newRoomIsValid = true;
        r.addUser(identity); // Room is valid, so let's add the user to this new room.
        System.out.println(identity + " moved to " + newRoom);
        clientCurrentRoom = newRoom;
        break;
      }
    }
    // Remove the peer from their old room
    if (newRoomIsValid) {
      for (Room r: currentRooms) {
        if (r.getRoomName().equals(oldRoom)) {
          r.removeUser(identity);
        }
      }
    }
    if (!newRoomIsValid) {
      System.out.println("The room you wanted to move to was invalid. ");
    }

    else {
      // Now if a peer happened to connect to us and join our local room before we moved to it then we must broadcast
      // our room movement to them.
      // This is based off the broadcast function from A1.
      for (ServerConnection c : currentConnections) {
        if (c.roomID.equals(newRoom)) {
          String serverMessage = jsonBuild.buildJSONJoinRoom(identity, oldRoom, newRoom);
          c.sendMessage(serverMessage + "\n");
        }
      }
    }
  }

 protected synchronized void kickPeer (String peerToKick) {
   for (ServerConnection c : currentConnections) {
     if (peerToKick.equals(c.identity)) {
       System.out.println("---> Kicking and banning "+peerToKick+" from this peer.");
       bannedPeers.add(peerToKick);
       JSONWriter jsonBuild = new JSONWriter();
       String message = "You have been kicked and banned from connecting to this peer. Do not come back.";
       ClientPackets.Kick kickthing = new ClientPackets.Kick(message);
       String serverMessage = jsonBuild.buildKickMsg(kickthing);
       c.justBeenKicked = true;
       c.sendMessage(serverMessage + "\n");
       // c.close();
       break;
     }
     else {
       System.out.println("No peer with the specified identity is connected to you, and thus no one could be kicked.");
     }
   }
 }

 protected synchronized void displayConnectedPeers () {
    if (currentConnections.isEmpty()) {
      System.out.println("Currently no one is connected to you.");
    }
    else {
      for (ServerConnection a : currentConnections) {
        System.out.println(a.identity);
      }
    }
 }

  /**
   * A server can act as a client. As such, they need to be able to create their own rooms.
   *
   * @param newRoomID
   * @param ownerIdentity
   */
  protected synchronized void createLocalRoom (String newRoomID, String ownerIdentity) {
    // Verify the new room id, and ensure new identity is alphanumeric and between 3 - 16 characters.
    boolean alreadyExists = false; // Check if the room already exists. Update the flag if it does.
    for (Room r: currentRooms) {
      if (r.getRoomName().equals(newRoomID)) {
        alreadyExists = true;
      }
    }

    // Check if the room exists or if it has an invalid name.
    if (alreadyExists || (!newRoomID.matches("[A-Za-z0-9]+") || (newRoomID.length() < 3) || ((newRoomID).length()) > 32)) {
      System.out.println("Room creation was not successful. Room already or exists or is invalid.");
    }

    // Otherwise, handle room creation.
    else {
      currentRooms.add(new Room(newRoomID, ownerIdentity));
      System.out.println("Successfully created room " + newRoomID);
    }
  }



  private synchronized void readMessage(String roomID, String msgContent, String msgIdentity) {
    // If the server is in the same room as the sender then they should be able to read the message
    if (clientCurrentRoom.equals(roomID)) {
      System.out.println(msgIdentity + ": " + msgContent);
    }
  }



  /**
   * The server should respond with a list of peers that are currently connected to it (not including its own network
   * address, or the address of the client that issued the request).
   */
  private synchronized String getListNeighbors(ServerConnection conn) {
    // Iterate through the currentConnections array list and build a JSON string out of it.

    List<String> neighbors = new ArrayList<String>();

    // Do not include the calling client in the list that is returned to the calling client.
    for (ServerConnection c: currentConnections) {
      if (!c.identity.equals(conn.identity)) {
        String listeningPort = Integer.toString(c.listenPort);
        String ip = c.ipAddress;
        String fullID = ip + ":" + listeningPort;
        neighbors.add(fullID);
      }
    }

    // Wrap this array into a Neighbors JSON
    JSONWriter jsonBuild = new JSONWriter();
    String neighborsMsg = jsonBuild.buildJsonListNeighbors(neighbors);

    // Return to the calling client
    return neighborsMsg;
  }






  /**
   * Send a packet to the client that discloses their identity (their outgoing port) when they initially connect to us.
   * Only we know what their outgoing port is....unless there's another way?
   * Uses the standard message protocol.
   * @param identity
   * @param conn
   */
  private void welcome(String identity, ServerConnection conn) {
    String idOfClient = "You are connected as "+identity+".";
    JSONWriter jsonBuild = new JSONWriter();   // Instantiate object that has method to build JSON string.
    String serverMessage = jsonBuild.buildJSON(idOfClient, serverIdentity); // Calls method that builds the JSON String.
    //System.out.format("%n"+"Sending "+"JSON string(s). Check below:%n");
    //System.out.format(ANSI_BLUE+"Sending Welcome JSON:"+ANSI_RESET+" %s%n", serverMessage);
    conn.sendMessage(serverMessage + ". \n");
  }


  // ***************************************************************************************************************
  // ***************************************************************************************************************
  // ***************************************************************************************************************
  // ***************************************************************************************************************
  // ***********************************     OLD METHODS FROM ASSIGNMENT 1     *************************************
  // ***************************************************************************************************************

  // Broadcast the connection of a new user.
  private synchronized void connect(ServerConnection conn) {
    currentConnections.add(conn);
  }

  // Broadcast the disconnection of a new user.
  private synchronized void disconnect(ServerConnection conn) {
    broadcast(String.format("%d has left the chat.\n", conn.socket.getPort()), conn);
    currentConnections.remove(conn);
  }


  // Rooms are strings that are stored in an arraylist. To access a particular room we need its index in the array.
  private int getRoomIndex(String roomID) {
    int index = 0;
    for (Room r: currentRooms) {
      if (r.getRoomName().equals(roomID)) {
        return index;
      }
      index += 1;
    }
    // Else, room not found.
    return -1;
  }


  // Old method. A bit redundant but still in use.
  // Broadcast a server message (CONNECT/DISCONNECT) to everyone in a room except the ignored person (usually yourself).
  private synchronized void broadcast(String message, ServerConnection ignored) {
    for (ServerConnection c : currentConnections) {
      // If not ignored, send message
      if (ignored == null || !ignored.equals(c)) {
        c.sendMessage(message);
      }
    }
  }


  /**
   * New method to broadcast to users within the same room as the messenger.
   * @param message   User message
   * @param roomID    The roomID to broadcast the message to
   * @param ignored
   */
  private synchronized void broadcastRoom(String message, String roomID, ServerConnection ignored, String ID, boolean isJson) {
    for (ServerConnection c : currentConnections) {
      if (c.roomID.equals(roomID) && !isJson) {
        if (ignored == null || !ignored.equals(c)) {
          /**
           Null if this method is called from the server, not a specific connection thread. Not gonna be null if called from a ServerConnection thread.
           We want to broadcast the message to everyone else in the room.
           First we need to build a JSON string out of the client message and append ID.
           */
          JSONWriter jsonBuild = new JSONWriter();
          String serverMessage = jsonBuild.buildJSON(message, ID);
          //System.out.format("%nSending "+"JSON string(s). Check below:%n");
          //System.out.println("BroadcastRoom JSON: " + serverMessage);
          // Now broadcast the JSON string to everyone in the room.
          c.sendMessage(serverMessage + "\n");
        }
      }

      // If we're already passing in a JSON then we don't need to build a JSON message. Just broadcast it.
      else if (c.roomID.equals(roomID) && isJson) {
        if (ignored == null || !ignored.equals(c)) {
          // Broadcast the JSON string to everyone in the room.
          System.out.format("%nSending "+"JSON string(s). Check below:%n");
          System.out.println("BroadcastRoom JSON: " + message);
          c.sendMessage(message + "\n");
        }
      }
    }
  }





  // Method to allow a peer to join a room hosted by a remote peer.
  protected synchronized void joinRoom(ServerConnection conn, String oldRoom, String newRoom) {
    // First check if the new 'room' is even valid.
    boolean newRoomIsValid = false;
    for (Room r: currentRooms) {
      if (r.getRoomName().equals(newRoom)) {
        newRoomIsValid = true;
        r.addUser(conn.identity); // Room is valid, so let's add the user to this new room.
        conn.roomID = newRoom;
        //clientCurrentRoom = newRoom;
        break;
      }
    }

    // Logic to handle new clients. By default, a new-joining client's 'old room' is just an empty string.
    if (newRoomIsValid && oldRoom.equals("")) {
      JSONWriter jsonBuild = new JSONWriter();
      //String serverMessage = jsonBuild.buildJSONJoinRoom(conn.identity, oldRoom, newRoom);
      //String newRoomContents = getRoomContents(conn, newRoom);
      //broadcastRoom(serverMessage, "MainHall", null, conn.identity, true);
      //conn.sendMessage(newRoomContents + "\n");

      // Send RoomChange message to client and all other clients in the room
      String serverMessage = jsonBuild.buildJSONJoinRoom(conn.identity, oldRoom, newRoom);
      //System.out.println(serverMessage);
      System.out.println(conn.identity + " moved to " + newRoom);
      //broadcastRoom(serverMessage, newRoom, null, conn.identity, true);
      conn.sendMessage(serverMessage + "\n");
    }

    // Logic to remove an existing client from their old room.
    else if (newRoomIsValid && (!oldRoom.equals(""))) {
      // Jump to the 'old' room and remove client from it.
      for (Room r: currentRooms) {
        if (r.getRoomName().equals(oldRoom)) {
          r.removeUser(conn.identity);
          // Send message to everyone in the old room that the client is leaving.
          JSONWriter jsonBuild = new JSONWriter();
          String serverMessage = jsonBuild.buildJSONJoinRoom(conn.identity, oldRoom, newRoom);
          broadcastRoom(serverMessage, oldRoom, conn, conn.identity, true);
          // Broadcast to old members where the client moved. This will have its own 'Sending JSON' blue section thing.
          // There will be one section per old person that it's sent to.
          // And then the below sends a message to everyone in the new room that the client has joined
          broadcastRoom(serverMessage, newRoom, null, conn.identity, true); // NOT IGNORING SELF. CHECK SPEC IF THIS IS FINE
        }
      }
      String newRoomContents = getRoomContents(conn, newRoom);
      //System.out.println("JSON containing members of room this client just joined: "+newRoomContents);
      System.out.println(conn.identity + " moved from " + oldRoom + " to " + newRoom);
      conn.sendMessage(newRoomContents + "\n");
      // Thus the client who moves to a new room gets sent 2 JSON strings -- 1 is the message that he has moved rooms
      // (everyone else in new room also gets this string), and 2 is the current contents of the new room.
    }

    else {
      // Unsuccessful. Send a message to the client.
      JSONWriter jsonBuild = new JSONWriter();
      String serverMessage = jsonBuild.buildJSONJoinRoom(conn.identity, oldRoom, oldRoom);
      conn.sendMessage(serverMessage + "\n");
    }
  }



  // Method used for the RoomList protocol. Second and third parameter optional.
  protected synchronized String getRoomList(ServerConnection conn, boolean createModifiedList, String newRoomID) {
    List<String> roomContents = new ArrayList<String>();
    ArrayList<ArrayList<String>> roomInformation = new ArrayList<>();

    // Creation WAS successful, so create an array of all rooms (including the newly created one -- as it's valid).
    if (!createModifiedList) {
      for (Room r: currentRooms) {
        JSONWriter jsonBuild = new JSONWriter();
        String serverMessage = jsonBuild.buildJsonRoomInfo(r.getRoomName(), r.getRoomSize());
        roomContents.add(serverMessage);
      }
      // Wrap this information in a RoomList JSON and send it over to the client.
      JSONWriter jsonBuild = new JSONWriter();
      String roomList = jsonBuild.buildJsonRoomList(roomContents);
      // System.out.format(ANSI_BLUE+"Sending RoomListJSON: "+ANSI_RESET+"%s%n", roomList);
      conn.sendMessage(roomList + "\n");
    }

    // Else creation NOT successful, so make a list containing the current rooms MINUS the repeat room (or invalid name room) that tried to be created.
    else {
      for (Room r: currentRooms) {
        if (!r.getRoomName().equals(newRoomID)) {
          JSONWriter jsonBuild = new JSONWriter();
          String serverMessage = jsonBuild.buildJsonRoomInfo(r.getRoomName(), r.getRoomSize());
          roomContents.add(serverMessage);
        }
      }
      // Wrap this information in a RoomList JSON and send it over.
      JSONWriter jsonBuild = new JSONWriter();
      String roomList = jsonBuild.buildJsonRoomList(roomContents);
      System.out.format("%n"+"Sending "+"JSON string(s). Check below:%n");
      System.out.println("Note: Failed to create new room as it already exists or is invalidly named. Sending reduced JSON now.");
      System.out.format("Invalid room, reduced RoomList JSON: %s%n", roomList);
      conn.sendMessage(roomList + "\n");
    }
    return newRoomID;
  }


  private synchronized String getRoomContents(ServerConnection conn, String roomid) {
    // Navigate to the current room and retrieve room occupants.
    List<String> roomContents = new ArrayList<String>();
    String roomOwner = null;

    for (Room r: currentRooms) {
      if (r.getRoomName().equals(roomid)) {
        roomContents = r.getRoomContents();
        roomOwner = r.getRoomOwner();
      }
    }

    // Wrap this array into a RoomContents JSON
    JSONWriter jsonBuild = new JSONWriter();
    String roomContentsMsg = jsonBuild.buildJsonRoomContents(roomid, roomContents, roomOwner);
    // Return to the calling client
    return roomContentsMsg;
  }



  private synchronized void createNewRoom(ServerConnection conn, String newRoomID) {
    // Verify the new room id, and ensure new identity is alphanumeric and between 3 - 16 characters.
    boolean alreadyExists = false; // Check if the room already exists. Update the flag if it does.
    for (Room r: currentRooms) {
      if (r.getRoomName().equals(newRoomID)) {
        alreadyExists = true;
      }
    }

    // Check if the room exists or if it has an invalid name.
    if (alreadyExists || (!newRoomID.matches("[A-Za-z0-9]+") || (newRoomID.length() < 3) || ((newRoomID).length()) > 32)) {
      // Send a modified RoomList to the client so that they know that room creation was unsuccessful.
      getRoomList(conn, true, newRoomID);

      /** What is a modified RoomList and why are we making it?
       * Usually a normal RoomList returns the list of ALL current rooms.
       * However: if a room already exists, we need to modify the RoomList that is sent to the client.
       * As per what Austen has agreed with on the discussion board, if the room already exists then we need to remove
       * that room from the RoomList before it is sent back to the client that was creating the room.
       * The calling client will then iterate through the modified RoomList it receives. If the room they wanted to
       * create is NOT present in this modified RoomList then they'll know that room creation was unsuccessful.
       */
    }

    // Otherwise, handle room creation.
    else {
      currentRooms.add(new Room(newRoomID, conn.identity));
      // Send roomList to client to tell them that room creation was successful.
      getRoomList(conn, false, null);
    }
  }


  private synchronized void deleteRoom(ServerConnection conn, String roomid) {
    // Get index of the room to delete. If the user sent a bogus room then the getRoomIndex method will return -1.
    JSONWriter jsonBuild1 = new JSONWriter();
    int index = getRoomIndex(roomid);
    System.out.println("Note: The room requested ("+roomid+") has the following index: " +  index);

    if (index != -1) {
      // If the room exists AND the room owner is the same as the requesting client's identity then delete the room.
      if (currentRooms.get(index).getRoomOwner().equals(conn.identity)) {
        System.out.println("Note: Moving occupants of "+roomid+" to MainHall");
        for (ServerConnection c : currentConnections) {
          if (c.roomID.equals(roomid)) {
            joinRoom(c, roomid, "MainHall"); // Move everyone in the current room to MainHall.
          }
        }
        currentRooms.remove(index); // Delete the room.
        getRoomList(conn, false, null);
        /**
         * The above sends the updated list of rooms and their users only to the client. If room was deleted then room should not appear in the list.
         * However, despite the server sending this JSON, nothing will actually be printed out on the client's end except for 'Room xxx has been deleted',
         * because roomName never equals getRoomToCreate() (which equals "", because we're not in the status of creating a room), and so alreadyExistsOrInvalid will be left at true :)
         */
      }
      // Else the requesting client is NOT the owner and thus doesn't have permission to delete.
      else {
        getRoomList(conn, false, null);
      }
    }
    // Else the requested room does not exist.
    else {
      getRoomList(conn, false, null);
    }
  }


  private synchronized void quit(ServerConnection conn, String roomID) {
    // If the client is in a room, send a roomchange message to all clients within that room.
    if (!conn.roomID.equals("")) {
      // Send RoomChange message to all clients in the room
      JSONWriter jsonBuild = new JSONWriter();
      String serverMessage = jsonBuild.buildJSONJoinRoom(conn.identity, roomID, "");
      System.out.println(serverMessage);
      broadcastRoom(serverMessage, roomID, null, conn.identity, true);

      // Send roomChange JSON to the requesting client which will result in disconnect.
      //conn.sendMessage(serverMessage + "\n");
    }

    else {
      JSONWriter jsonBuild = new JSONWriter();
      String serverMessage = jsonBuild.buildJSONJoinRoom(conn.identity, "", "");
      conn.sendMessage(serverMessage + "\n");
    }
    // Disconnect the client.
    conn.close();
    this.currentConnections.remove(conn);
  }



  private synchronized void closeRooms(ServerConnection conn) {
    try {
      for (Iterator<Room> it = currentRooms.iterator(); it.hasNext(); ) {
        Room r = it.next();
        // While we're here, if the room has no owner AND no contents then delete it.
        if (r.getRoomOwner().equals("") && (r.getRoomContents().size() == 0) && !r.getRoomName().equals("MainHall")) {
          System.out.println("Peer to delete room: " + r.getRoomName() + " as it is empty with no owner.");
          it.remove();
        }
      }
    } catch (Exception e) {
      System.out.println("Exception raised when closing rooms. ");
    }
  }


  /**
   * All current clients have a 'ServerConnection' which is used to listen to each client.
   * Each client is identified by the String 'identity'
   * Each client belongs to a room (they are designated a roomID by default).
   */
  private class ServerConnection extends Thread {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private boolean connectionAlive = false;
    //private int port;
    private String roomID;
    private int roomIndex;
    private String identity;
    private boolean gracefulDisconnection = false;
    private boolean justBeenKicked = false;

    // Used for searchNetwork
    private String ipAddress;
    private int listenPort;

    public ServerConnection (Socket socket, String identity, String roomID) throws IOException {
      this.socket = socket;
      this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      this.writer = new PrintWriter(socket.getOutputStream());
      this.identity = identity;
      this.roomID = roomID;
    }

    @Override
    public void run() {
      // Manage the connection here
      connectionAlive = true;
      String msg;

      // Peer sends NewIdentity JSON to client to give it its initial username (e.g. guestXXXXX).
      JSONWriter jsonBuild = new JSONWriter();
      String newIDMessage = jsonBuild.buildJSONNewID("", identity);
      sendMessage(newIDMessage + "\n");

      // Welcome the user (show current rooms) and then move them to MainHall.
      welcome(identity, this); // Generates a welcome JSON string message and flushes it to the client, which will post it on the client's screen.

      while (connectionAlive) {
        try {
          // While the connection is alive we wait and listen to their socket, reading in JSON objects from the socket as they arrive.
          String in = reader.readLine();
          JSONReader read = new JSONReader();
          msg = read.readMSg(in);
          if (in != null) {
            JSONReader jRead = new JSONReader();
            jRead.readInput(in);
            String type = jRead.getJSONType();   // Extract the value from the 'type' key field.
            //System.out.format(ANSI_RED+"Received "+type+" JSON: "+ANSI_RESET+"%s%n", in);

            // The below if-else statements analyse the received JSON object type, and act accordingly.

            // When we receive a standard message we need to check if the sender is in a room.
            if (type.equals("message") && (!roomID.equals(""))) {
              // Again, a server can be a client. As such they should be able to read a message they receive
              // as long as they are in the same room as the sender.
              readMessage(roomID, jRead.getJSONContent(), identity);

              // Regardless, the server should broadcast that message to everyone else in the room
              broadcastRoom(msg, roomID, null, identity, false);     // "this" -> ignore ourselves in the broadcast
            }

            else if (type.equals("hostchange")) {
              this.ipAddress = jRead.getJSONIP();
              this.listenPort = Integer.valueOf(jRead.getJSONHost());
            }

            else if (type.equals("join")) {
              // System.out.format("JoinRoom JSON: %s%n", in);
              String newRoom = jRead.getJSONRoomId();
              String currentRoom = roomID;
              joinRoom(this, currentRoom, newRoom);
            }

            else if (type.equals("list")) {
              getRoomList(this, false, null);
            }

            else if (type.equals("listneighbors")) {
              String listNeighbors = getListNeighbors(this);
              sendMessage(listNeighbors + "\n");
            }

            else if (type.equals("searchnetwork")) {
              searchNetwork();
            }

            else if (type.equals("who")) {
              System.out.format("Who JSON: %s%n", in);
              String whoRoom = jRead.getJSONRoomId();
              boolean roomExists = false;
              // Check that the room they're inquiring about exists.
              for (Room r: currentRooms) {
                if (r.getRoomName().equals(whoRoom)) {
                  roomExists = true;
                }
              }
              // If the room does exist, send the room contents.
              if (roomExists) {
              String contents = getRoomContents(this, whoRoom);
              // System.out.format("%nSending "+"JSON string(s). Check below:%n");
              System.out.println("BroadcastRoom JSON: " + contents);
              sendMessage(contents + "\n");
              }
              // If it doesn't, send an error message.
              else {
                String whoErrorMessage = "The room you're inquiring about ("+whoRoom+") doesn't exist. Try again.";
                String serverMessageJSON = jsonBuild.buildJSON(whoErrorMessage, serverIdentity); // Replaced "Peer"
                // System.out.format("%nSending "+"JSON string(s). Check below:%n");
                System.out.println("WrongWho JSON: " + serverMessageJSON);
                this.sendMessage(serverMessageJSON + "\n");
              }
            }

            else if (type.equals("createroom")) {
              System.out.format("CreateRoom JSON: %s%n", in);
              String newRoomID = jRead.getJSONRoomId();
              createNewRoom(this, newRoomID);
            }

            else if (type.equals("delete")) {
              System.out.format("DeleteRoom JSON: %s%n", in);
              String roomToDelete = jRead.getJSONRoomId();
              deleteRoom(this, roomToDelete);
            }

            else if (type.equals("quit")) {
              quit(this, roomID);
            }

            else if (type.equals("migrationroom")) {
              String sender = jRead.getJSONMigrationSender();
              String roomName = jRead.getJSONMigrationRoomName();
              int totalRooms = Integer.parseInt(jRead.getJSONMigrationTotalRooms());

              handleMigratedRooms(sender, roomName, totalRooms);
            }

            // Scan through the rooms and delete rooms that have no owner AND are empty
            closeRooms(this);

          } else {
            //close();
            connectionAlive = false;
          }

        } catch (IOException | InterruptedException e) {
          //close();
          connectionAlive = false;
        }
      }

      if (!gracefulDisconnection) {
        // If client didn't disconnect via #quit then force close the connection.
        if (!justBeenKicked) {
          System.out.println("DEBUG: Someone abruptly disconnected from you.");
        }
        else {
          System.out.println("Client kicked and has terminated connection on their side.");
        }
        quit(this, roomID);
      }
    }

    public void sendMessage (String msg) {
      //System.out.format(ANSI_BLUE+"Sending JSON:"+ANSI_RESET+" %s", msg);
      writer.print(msg);
      writer.flush(); // Empty the buffer and send the data over the network.
    }

//    public void getList() {
//      List<String> roomContents = new ArrayList<String>();
//      getLocalRoomList();
//    }

    public void close() {
      try {
        // Traverse to the room that the client belongs to and remove client from that room.
        // Also, if they are the owner of any room then we remove their ownership.
        for (Iterator<Room> it = currentRooms.iterator(); it.hasNext(); ) {
          Room r = it.next();
          if (identity.equals(r.getRoomOwner())) {
            r.setRoomOwner("");
          }
          // While we're here, if the room has no owner AND no contents then delete it.
          if (r.getRoomOwner().equals("") && (r.getRoomContents().size() == 0) && !r.getRoomName().equals("MainHall")) {
            System.out.println("Peer to delete room: " + r.getRoomName());
            it.remove();
          }
        }

        if (!roomID.equals("")) {
          currentRooms.get(getRoomIndex(roomID)).removeUser(identity);
        }
        //disconnect(this);
        reader.close();
        writer.close();
        socket.close();
        this.roomID = "";
        gracefulDisconnection = true;

      } catch (IOException e) {
        //System.out.println(e.getMessage());
      }
    }
  }
}

