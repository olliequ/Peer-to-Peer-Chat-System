package com.kvoli;

import com.kvoli.base.*;


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Server {
  private boolean acceptConnections = false;
  public static int PORT = 4444;        // changed from 6379 to 4444
  private volatile List<ServerConnection> currentConnections = new ArrayList<>();
  private volatile List<Room> currentRooms = new ArrayList<>();
  private int guestCount = 0;                   // Used to identify new users. E.g. "guest5 has joined the server".
  public static final String ANSI_RED = "\u001B[31m";
  public static final String ANSI_BLUE = "\u001B[34m";
  public static final String ANSI_CYAN = "\u001B[36m";
  public static final String ANSI_GREEN = "\u001B[32m";
  public static final String ANSI_RESET = "\u001B[0m";

  public Server() {}

  public Server(int port) {
    this.PORT = port;
  }


  // Listen for new connections and create the initial room.
  protected void handle() {
    currentRooms.add(new Room("MainHall"));

    // Now handle connections to the server
    ServerSocket serverSocket;
    try {
      // We want to bind serverSocket to a port. That port should accept connections within an infinite loop.
      serverSocket = new ServerSocket(PORT);
      System.out.printf("Currently listening on port number %d \n", PORT);
      acceptConnections = true;           // Listen for connections -- whilst the server is up and alive.

      while (acceptConnections) {
        // Accepted a connection. Move them to the main hall room.
        Socket socket = serverSocket.accept(); // Generate new socket based off the encompassing ServerSocket -- accept it.
        System.out.println(ANSI_GREEN+"\nAccepted connection from client with port number: " + socket.getPort()+ANSI_RESET); // Port # of client.

        // Assign name
        guestCount += 1;
        String clientName = "Guest" + guestCount;

        // Perform operations with the socket (client)
        // Start a connection which will have its own thread of execution. Then we don't care about it anymore.
        // The connection will be able to handle itself.
        ServerConnection currentConnection = new ServerConnection(socket, clientName, "");
        currentConnection.start();
        connect(currentConnection);
      }

    } catch (IOException e) {
      System.out.println("Server exception occurred.");
      e.printStackTrace();
    }
  }

  // Broadcast the connection of a new user.
  private synchronized void connect(ServerConnection conn) {
    currentConnections.add(conn);
  }

  // Broadcast the disconnection of a new user.
  private synchronized void disconnect(ServerConnection conn) {
    broadcast(String.format("%d has left the chat.\n", conn.socket.getPort()), conn);
    currentConnections.remove(conn);
  }

  // Display a welcome message to the new user along with list of current rooms.
  private void welcome(String clientIdentity, ServerConnection conn) {
    String welcomeClient = "---> Welcome! You are connected as: "+ANSI_CYAN+clientIdentity+ANSI_RESET+"\n---> Here's a rundown on the currently active rooms:";
    String FromServerOrNot = "Yes"; // Make this a boolean instead?
    JSONWriter jsonBuild = new JSONWriter();   // Instantiate object that has method to build JSON string.
    String serverMessage = jsonBuild.buildJSON(welcomeClient, "Server"); // Calls method that builds the JSON String.
    System.out.format("%n"+ANSI_BLUE+"Sending "+"JSON string(s). Check below:%n"+ANSI_RESET);
    System.out.format("Welcome JSON String: %s%n", serverMessage);
    conn.sendMessage(serverMessage + ". \n");

    for (Room room: currentRooms) {
     // String ln = "\t\t---> "+room.getRoomName() + " has " + room.getRoomSize() + " guest(s) currently inside.";
     // String roomInfoJSON = jsonBuild.buildJSON(ln, "Server");
     // System.out.format("Printing Room JSON String: %s%n", roomInfoJSON);
      getRoomList(conn, false, null);
      //conn.sendMessage(roomInfoJSON);
      //conn.sendMessage("\n");
    }
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
          System.out.format(ANSI_BLUE+"%nSending "+"JSON string(s). Check below:%n"+ANSI_RESET);
          System.out.println("BroadcastRoom JSON: " + serverMessage);
          // Now broadcast the JSON string to everyone in the room.
          c.sendMessage(serverMessage + "\n");
        }
      }

      // If we're already passing in a JSON then we don't need to build a JSON message. Just broadcast it.
      else if (c.roomID.equals(roomID) && isJson) {
        if (ignored == null || !ignored.equals(c)) {
          // Broadcast the JSON string to everyone in the room.
          System.out.format(ANSI_BLUE+"%nSending "+"JSON string(s). Check below:%n"+ANSI_RESET);
          System.out.println("BroadcastRoom JSON: " + message);
          c.sendMessage(message + "\n");
        }
      }
    }
  }

  // Method to verify user identity (used for the changeIdentity method below)
  private synchronized boolean verifyIdentity(ServerConnection conn, String newIdentity) {
    // Make sure the new identity is unique
    for (ServerConnection c : currentConnections) {
      if (c.identity.equals(newIdentity)) {
        System.out.println("Failed to change identity. Already in use.");
        return false;
      }
    }
    // Ensure new identity is alphanumeric and between 3 - 16 characters.
    if (!newIdentity.matches("[A-Za-z0-9]+") || (newIdentity.length() < 3) || ((newIdentity).length()) > 16) {
      // Print server-side fail message
      System.out.println("Failed to change identity. Bad format.");
      return false;
    }
    return true;
  }

  private synchronized String changeIdentity(ServerConnection conn, String newIdentity, boolean isValid) {
    // If the new identity is invalid then we tell the client that we can't change their identity.
    if (!isValid) {
      JSONWriter jsonBuild = new JSONWriter();
      String serverMessage = jsonBuild.buildJSONNewID(conn.identity, conn.identity);
      conn.sendMessage(serverMessage + "\n");
      return serverMessage;
    }

    // Otherwise, change the identity of the client
    else {
      String oldID = conn.identity;
      conn.identity = newIdentity;

      // If this client is the owner of existing rooms then we need to update their owner status to reflect new identity.
      for (Room r: currentRooms) {
        if (r.getRoomOwner().equals(oldID)) {
          r.setRoomOwner(newIdentity);
        }
      }

      // Update client room roomContents with new identity.
      currentRooms.get(getRoomIndex(conn.roomID)).changeUserID(oldID, newIdentity);
      JSONWriter jsonBuild = new JSONWriter();
      String serverMessage = jsonBuild.buildJSONNewID(oldID, conn.identity);
      return serverMessage;
    }
  }

  // Method to allow a client to join a room.
  private synchronized void joinRoom(ServerConnection conn, String oldRoom, String newRoom) {
    // First check if the new 'room' is even valid.
    boolean newRoomIsValid = false;
    for (Room r: currentRooms) {
      if (r.getRoomName().equals(newRoom)) {
        newRoomIsValid = true;
        r.addUser(conn.identity); // Room is valid, so let's add the user to this new room.
        conn.roomID = newRoom;
        break;
      }
    }

    // Logic to handle new clients. By default, a new-joining client's 'old room' is just an empty string.
    if (newRoomIsValid && oldRoom.equals("")) {
      JSONWriter jsonBuild = new JSONWriter();
      String serverMessage = jsonBuild.buildJSONJoinRoom(conn.identity, oldRoom, newRoom);
      String newRoomContents = getRoomContents(conn, newRoom);
      broadcastRoom(serverMessage, "MainHall", null, conn.identity, true);
      conn.sendMessage(newRoomContents + "\n");
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
      System.out.println("JSON containing members of room this client just joined: "+newRoomContents);
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
  private synchronized void getRoomList(ServerConnection conn, boolean createModifiedList, String newRoomID) {
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
      System.out.format("%n"+ANSI_BLUE+"Sending "+"JSON string(s). Check below:%n"+ANSI_RESET);
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
      System.out.format("%n"+ANSI_BLUE+"Sending "+"JSON string(s). Check below:%n"+ANSI_RESET);
      System.out.println("Note: Failed to create new room as it already exists or is invalidly named. Sending reduced JSON now.");
      System.out.format("Invalid room, reduced RoomList JSON: %s%n", roomList);
      conn.sendMessage(roomList + "\n");
    }
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
//        String deleteErrorMessage = ANSI_RED+"You are not the owner of "+roomid+" and so do not have permission to delete it. Nice try, though!"+ANSI_RESET;
//        String deleteErrorMessageJSON = jsonBuild1.buildJSON(deleteErrorMessage, "Server");
//        System.out.format(ANSI_BLUE+"%nSending "+"JSON string(s). Check below:%n"+ANSI_RESET);
//        System.out.println("DeleteError JSON: " + deleteErrorMessageJSON);
//        conn.sendMessage(deleteErrorMessageJSON + "\n");
        getRoomList(conn, false, null);
      }
    }
    // Else the requested room does not exist.
    else {
//      String deleteErrorMessage = ANSI_RED+"The room you're trying to delete ("+roomid+") does not exist."+ANSI_RESET;
//      String deleteErrorMessageJSON = jsonBuild1.buildJSON(deleteErrorMessage, "Server");
//      System.out.format(ANSI_BLUE+"%nSending "+"JSON string(s). Check below:%n"+ANSI_RESET);
//      System.out.println("DeleteError JSON: " + deleteErrorMessageJSON);
//      conn.sendMessage(deleteErrorMessageJSON + "\n");
      getRoomList(conn, false, null);
    }
  }

  private synchronized void quit(ServerConnection conn, String roomID) {
    // Send RoomChange message to all clients in the room
    JSONWriter jsonBuild = new JSONWriter();
    String serverMessage = jsonBuild.buildJSONJoinRoom(conn.identity, roomID, "");
    System.out.println(serverMessage);
    broadcastRoom(serverMessage, roomID, null, conn.identity, true);

    // Send roomChange JSON to the requesting client which will result in disconnect.
    conn.sendMessage(serverMessage + "\n");
    conn.close();
  }


  private synchronized void closeRooms(ServerConnection conn) {
    try {
      for (Iterator<Room> it = currentRooms.iterator(); it.hasNext(); ) {
        Room r = it.next();
        // While we're here, if the room has no owner AND no contents then delete it.
        if (r.getRoomOwner().equals("") && (r.getRoomContents().size() == 0) && !r.getRoomName().equals("MainHall")) {
          System.out.println("Server to delete room: " + r.getRoomName() + " as it is empty with no owner.");
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
    private int port;
    private String roomID;
    private int roomIndex;
    private String identity;
    private boolean gracefulDisconnection = false;

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
      // Below tells the new client where it is. The line below that will inform the other clients. TODO: ***Not being received atm.
      String msg = ANSI_RED+"---> "+identity+", has entered "+currentRooms.get(roomIndex).getRoomName()+". Be nice!"+ANSI_RESET;
      broadcastRoom(msg, roomID, this, "Server", false);

      // Server sends NewIdentity JSON to client to give it its initial username (e.g. guestXXXXX).
      JSONWriter jsonBuild = new JSONWriter();
      String newIDMessage = jsonBuild.buildJSONNewID("", identity);
      sendMessage(newIDMessage + "\n");

      // Welcome the user (show current rooms) and then move them to MainHall.
      welcome(identity, this); // Generates a welcome JSON string message and flushes it to the client, which will post it on the client's screen.
      joinRoom(this, "", "MainHall");

      while (connectionAlive) {
        try {
          // While the connection is alive we wait and listen to their socket, reading in JSON objects as they arrive.
          String in = reader.readLine();
          JSONReader read = new JSONReader();
          msg = read.readMSg(in);
          if (in != null) {
            JSONReader jRead = new JSONReader();
            jRead.readInput(in);
            String type = jRead.getJSONType();   // Extract the value from the 'type' key field.
            System.out.format(ANSI_RED+"%nReceived "+"JSON string of type: %s. It is below:%n"+ANSI_RESET, type);

            /**
             * The below if-else statements analyse the received JSON object type, and act accordingly.
             */

            if (type.equals("message")) {
              //broadcastRoom(msg, roomID, this, identity, false);     // "this" -> ignore ourselves in the broadcast
              System.out.format("Message JSON: %s%n", in);
              broadcastRoom(msg, roomID, null, identity, false);     // "this" -> ignore ourselves in the broadcast
            }

            else if (type.equals("identitychange")) {
              System.out.format("Raw IC JSON: %s%n", in);
              String oldIdentity = identity;
              String newIdentity = jRead.getJSONIdentity();
              boolean isValidIdentity = verifyIdentity(this, newIdentity);
              String newIdentityMessage = changeIdentity(this, newIdentity, isValidIdentity);
              if (isValidIdentity) {
                System.out.format("Valid CI JSON: %s%n", newIdentityMessage);
                broadcastRoom(newIdentityMessage, roomID, null, "Server", true);
              }
            }

            else if (type.equals("join")) {
              System.out.format("JoinRoom JSON: %s%n", in);
              String newRoom = jRead.getJSONRoomId();
              System.out.println("Note: User is attempting to join '" + newRoom+"'.");
              String currentRoom = roomID;
              joinRoom(this, currentRoom, newRoom);
            }

            else if (type.equals("list")) {
              System.out.format("List JSON: %s%n", in);
              getRoomList(this, false, null);
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
              System.out.format(ANSI_BLUE+"%nSending "+"JSON string(s). Check below:%n"+ANSI_RESET);
              System.out.println("BroadcastRoom JSON: " + contents);
              sendMessage(contents + "\n");
              }
              // If it doesn't, send an error message.
              else {
                String whoErrorMessage = "The room you're inquiring about ("+whoRoom+") doesn't exist. Try again.";
                String serverMessageJSON = jsonBuild.buildJSON(whoErrorMessage, "Server");
                System.out.format(ANSI_BLUE+"%nSending "+"JSON string(s). Check below:%n"+ANSI_RESET);
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
        System.out.println("NOT GRACEFUL");
        quit(this, roomID);
      }
    }

    public void sendMessage (String msg) {
      writer.print(msg);
      writer.flush(); // Empty the buffer and send the data over the network.
    }

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
            System.out.println("Server to delete room: " + r.getRoomName());
            it.remove();
          }
        }

        currentRooms.get(getRoomIndex(roomID)).setRoomOwner("");
        currentRooms.get(getRoomIndex(roomID)).removeUser(identity);
        disconnect(this);
        reader.close();
        writer.close();
        socket.close();
        this.roomID = "NULL";
        gracefulDisconnection = true;

      } catch (IOException e) {
        //System.out.println(e.getMessage());
      }
    }
  }
}

