package com.kvoli;

import com.kvoli.base.*;


import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Peer {
  private PrintWriter writer;

  protected Socket socket;
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

  private boolean acceptConnections = false;

  // As a server we maintain a list of connections and a list of rooms we are aware of.
  private volatile List<ServerConnection> currentConnections = new ArrayList<>();
  private volatile List<Room> currentRooms = new ArrayList<>();
  public static final String ANSI_RED = "\u001B[31m";
  public static final String ANSI_BLUE = "\u001B[34m";
  public static final String ANSI_CYAN = "\u001B[36m";
  public static final String ANSI_GREEN = "\u001B[32m";
  public static final String ANSI_YELLOW = "\u001B[33m";
  public static final String ANSI_RESET = "\u001B[0m";
  public final int outgoingPort = 54000;

  // Old code from A1
  public static int PORT = 4444;

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
      currentRooms.add(new Room("tr", serverIdentityInetAddress.toString()));

      // The peers port will accept incoming connections within an infinite loop.
      while (acceptConnections) {
        // Accepted a connection from a peer. Generate new socket based off the encompassing ServerSocket -- accept it.
        Socket socket = serverSocket.accept();

        // Note that the port number we received is the clients OUTGOING port.
        System.out.println(ANSI_CYAN+"\n---> Accepted connection from another peer who's using their port number: "+ANSI_RESET+ socket.getPort());

        // The connected peer's identity is a combination of their IP address and their outgoing port number
        String addressOfPeer = socket.getInetAddress().toString();
        int portOfPeer = socket.getPort();
        String clientIdentity = addressOfPeer + ':' + portOfPeer;
        System.out.println("\t- The identity of the peer that just connected to you is: " + clientIdentity);
        System.out.format("\t- Connected off of you on: %s:%d%n", socket.getLocalAddress(), socket.getLocalPort());

        // Each peer that connects to this peer will have its own thread of execution.
        // The connection will be able to handle itself.
        ServerConnection currentConnection = new ServerConnection(socket, clientIdentity, "");
        currentConnection.start();
        connect(currentConnection);
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
  protected synchronized void connectToPeer(String destIP, int destPort, int outGooingPort) {
    try {
      // Attempt to establish connection to the destination IP and Port
      // System.out.format("Connected to: %s, %d%n", this.destSocket.getInetAddress(), this.destSocket.getPort());
      this.destSocket = new Socket(destIP, destPort, serverIdentityInetAddress, outGooingPort);
      this.ToConnectedPeer = destSocket.getOutputStream();
      this.FromConnectedPeer = destSocket.getInputStream();
      this.reader = new BufferedReader(new InputStreamReader(FromConnectedPeer));
      new GetMessageThread(this).start();
      System.out.format("Connected to: %s:%d%n", this.destSocket.getInetAddress(), this.destSocket.getPort());
      System.out.format("Connected from: %s:%d%n", this.destSocket.getLocalAddress(), this.destSocket.getLocalPort());

    } catch (IOException e) {
      System.out.println("Couldn't connect to peer.");
      e.printStackTrace();
    }
  }



  protected synchronized void searchNetwork() {
    // The peer should crawl over all other peers that are available to it. We connect to the first peer that is
    // connected to us (e.g. peer B), then we ask peer B to hand over all the peers that are connected to it.

    // ArrayList containing the #list from each peer
    ArrayList<List<String>> peerLists = new ArrayList<>();

    // ArrayList containing the #listneighbors from each peer
    ArrayList<String> peerNeighbors = new ArrayList<String>();

    // Iterate over all peers that are connected to us.
    for (ServerConnection c : currentConnections) {
      // Connect to this peer using a separate socket.
      System.out.println("Listening port of that peer is : " + c.listenPort);
      System.out.println("That peers IP is: " + c.ipAddress);
      connectToPeer(c.ipAddress, c.listenPort, 6000); // TODO: Change this 6000.

      writer = new PrintWriter(ToConnectedPeer, true);
      JSONWriter jWrite = new JSONWriter();

      // Ask this peer to hand over its rooms via the List command.
      ClientPackets.List listRoom = new ClientPackets.List();
      String msg = jWrite.buildListMsg(listRoom);
      writer.println(msg);
      writer.flush();

      // Right now it prints to terminal. Need to figure out how to store as value.


      // Ask this peer to hand over its neighbours via the ListNeighbours request.
    }
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
    System.out.println("Your local room list is: ");
    for (Room r: currentRooms) {
      //System.out.println(r.getRoomOwner());
      System.out.println(r.getRoomName() + " with " + r.getRoomSize() + " users");
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
        neighbors.add(c.identity);
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
    String serverMessage = jsonBuild.buildJSON(idOfClient, serverIdentityInetAddress.toString()); // Calls method that builds the JSON String.
    //System.out.format("%n"+"Sending "+"JSON string(s). Check below:%n");
    System.out.format(ANSI_BLUE+"Sending Welcome JSON:"+ANSI_RESET+" %s%n", serverMessage);
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
      System.out.println(serverMessage);
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
      System.out.format("%n"+"Sending "+"JSON string(s). Check below:%n");
      System.out.format("RoomListJSON: %s%n", roomList);
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
      System.out.println("CASE 1");
      // Send RoomChange message to all clients in the room
      JSONWriter jsonBuild = new JSONWriter();
      String serverMessage = jsonBuild.buildJSONJoinRoom(conn.identity, roomID, "");
      System.out.println(serverMessage);
      broadcastRoom(serverMessage, roomID, null, conn.identity, true);

      // Send roomChange JSON to the requesting client which will result in disconnect.
      //conn.sendMessage(serverMessage + "\n");
    }

    else {
      System.out.println("CASE 2");
      JSONWriter jsonBuild = new JSONWriter();
      String serverMessage = jsonBuild.buildJSONJoinRoom(conn.identity, "", "");
      conn.sendMessage(serverMessage + "\n");
    }
    // Disconnect the client.
    conn.close();
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
            System.out.format(ANSI_RED+"Received "+type+" JSON: "+ANSI_RESET+"%s%n", in);

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
              System.out.format("JoinRoom JSON: %s%n", in);
              String newRoom = jRead.getJSONRoomId();
              String currentRoom = roomID;
              joinRoom(this, currentRoom, newRoom);
            }

            else if (type.equals("list")) {
              System.out.format("List JSON: %s%n", in);
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
              System.out.format("%nSending "+"JSON string(s). Check below:%n");
              System.out.println("BroadcastRoom JSON: " + contents);
              sendMessage(contents + "\n");
              }
              // If it doesn't, send an error message.
              else {
                String whoErrorMessage = "The room you're inquiring about ("+whoRoom+") doesn't exist. Try again.";
                String serverMessageJSON = jsonBuild.buildJSON(whoErrorMessage, "Peer");
                System.out.format("%nSending "+"JSON string(s). Check below:%n");
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
              System.out.format("Quit JSON: %s%n", in);
              quit(this, roomID);
            }

            // Scan through the rooms and delete rooms that have no owner AND are empty
            closeRooms(this);

          } else {
            //close();
            connectionAlive = false;
          }

        } catch (IOException e) {
          //close();
          connectionAlive = false;
        }
      }

      if (!gracefulDisconnection) {
        // If client didn't disconnect via #quit then force close the connection.
        System.out.println("DEBUG: Someone abruptly disconnected from you.");
        quit(this, roomID);
      }
    }

    public void sendMessage (String msg) {
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

