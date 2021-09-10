package com.kvoli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.kvoli.base.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
  private boolean acceptConnections = false;
  public static final int PORT = 6379;
  private volatile List<ServerConnection> currentConnections = new ArrayList<>();
  private volatile List<Room> currentRooms = new ArrayList<>();
  private int guestCount = 0;                   // Used to identify new users. E.g. "guest5 has joined the server".
  public static final String ANSI_RED = "\u001B[31m";
  public static final String ANSI_BLUE = "\u001B[34m";
  public static final String ANSI_CYAN = "\u001B[36m";
  public static final String ANSI_GREEN = "\u001B[32m";
  public static final String ANSI_RESET = "\u001B[0m";

  // Listen for new connections and create the initial room.
  protected void handle() {
    // First create the main hall. For testing purposes I made a second room since we can't create rooms yet.
    createRoom("MainHall");
    createRoom("test");

    // Now handle connections to the server
    ServerSocket serverSocket;
    try {
      // We want to bind serverSocket to a port. That port should accept connections within an infinite loop.
      serverSocket = new ServerSocket(PORT);
      System.out.printf("Currently listening on port number %d%n", PORT);
      acceptConnections = true;           // Listen for connections -- whilst server still up and alive.

      while (acceptConnections) {
        // Accepted a connection. Move them to the main hall room.
        Socket socket = serverSocket.accept(); // Generate new socket based off the encompassing ServerSocket -- accept it.
        System.out.println(ANSI_GREEN+"\nAccepted connection from client with port number: " + socket.getPort() + "."+ANSI_RESET); // Port # of client.

        // Perform operations with the socket (client)
        // Now that the socket is assigned to a room, their messages should only be restricted to that room.
        // Note: by default, new users are added to the Main Hall (roomID = "MainHall").
        guestCount += 1;
        String clientName = "Guest" + guestCount;
        currentRooms.get(0).addUser(clientName); // Go to 0 because always land on MainHall.

        // Start a connection which will have its own thread of execution. Then we don't care about it anymore.
        // The connection will be able to handle itself.
        ServerConnection currentConnection = new ServerConnection(socket, clientName, "MainHall");
        currentConnection.start();
        connect(currentConnection);
      }

    } catch (IOException e) {
      System.out.println("Server exception occurred.");
      e.printStackTrace();
    }
  }

  // Method to create a room
  private void createRoom(String roomID) {
    currentRooms.add(new Room(roomID));
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
    String FromServerOrNot = "Yes";
    JSONWriter jsonBuild = new JSONWriter();   // Instantiate object that has method to build JSON string.
    String serverMessage = jsonBuild.buildJSON(welcomeClient, clientIdentity); // Calls method that builds the JSON String.
    System.out.format("%n"+ANSI_BLUE+"Sending "+"JSON string(s). Check below:%n"+ANSI_RESET);
    System.out.format("Welcome JSON String: %s%n", serverMessage);
    conn.sendMessage(serverMessage);
    conn.sendMessage("\n");

    for (Room room: currentRooms) {
      String RoomInfo = "\t\t- "+room.getRoomName() + " has " + room.getRoomSize() + " guest(s) currently inside.";
      String roomInfoJSON = jsonBuild.buildJSON(RoomInfo, "Server");
      System.out.format("Printing Room JSON String: %s%n", roomInfoJSON);
      conn.sendMessage(roomInfoJSON);
      conn.sendMessage("\n");
    }

    JSONWriter jsonBuild_1 = new JSONWriter();   // Instantiate object that has method to build JSON string.
    String howToType = "---> You've landed in MainHall. Type something next to the prompt below to send a message!";
    String howToTypeJSON = jsonBuild_1.buildJSON(howToType, "Server"); // Calls method that builds the JSON String.
    System.out.format("howToType JSON String: %s%n", howToTypeJSON);
    conn.sendMessage(howToTypeJSON);
    conn.sendMessage("\n");
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
    return 0;
  }

  // Old method. A bit redundant but still in use.
  // Broadcast a server message (CONNECT/DISCONNECT) to everyone in a room except the ignored person (usually yourself).
  private synchronized void broadcast(String message, ServerConnection ignored) {
    for (ServerConnection c : currentConnections) {
      // If not ignored, send message
      if (ignored == null || !ignored.equals(c)) {
        c.sendMessage(message);
        c.sendMessage("\n");
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
          c.sendMessage(serverMessage);
          c.sendMessage("\n");
        }
      }

      // If we're already passing in a JSON then we don't need to build a JSON message. Just broadcast it.
      else if (c.roomID.equals(roomID) && isJson) {
        if (ignored == null || !ignored.equals(c)) {
          // Broadcast the JSON string to everyone in the room.
          System.out.format(ANSI_BLUE+"%nSending "+"JSON string(s). Check below:%n"+ANSI_RESET);
          System.out.println("BroadcastRoom JSON: " + message);
          c.sendMessage(message);
          c.sendMessage("\n");
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
      // Send fail message to client
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
      conn.sendMessage("\n");
      return serverMessage;
    }

    // Otherwise, change the identity of the client
    else {
      String oldID = conn.identity;
      conn.identity = newIdentity;

      // Update client room roomContents with new identity.
      currentRooms.get(getRoomIndex(conn.roomID)).changeUserID(oldID, newIdentity);
      JSONWriter jsonBuild = new JSONWriter();
      String serverMessage = jsonBuild.buildJSONNewID(oldID, conn.identity);
      // System.out.format("CI JSON String: %s%n", serverMessage);
      return serverMessage;
    }
  }


  // Method to allow a client to join a room
  private synchronized void joinRoom(ServerConnection conn, String oldRoom, String newRoom) {
    // First check if the new 'room' is even valid
    boolean isValid = false;
    for (Room r: currentRooms) {
      if (r.getRoomName().equals(newRoom)) {
        // Room is valid. Add user to this new room.
        isValid = true;
        r.addUser(conn.identity);
        conn.roomID = newRoom;
        break;
      }
    }

    // Logic to remove a client from their old room
    if (isValid) {
      // Jump to the 'old' room and remove client from it.
      for (Room r: currentRooms) {
        if (r.getRoomName().equals(oldRoom)) {
          r.removeUser(conn.identity);

          // Send message to everyone in the old room that the client is leaving.
          JSONWriter jsonBuild = new JSONWriter();
          String serverMessage = jsonBuild.buildJSONJoinRoom(conn.identity, oldRoom, newRoom);
          broadcastRoom(serverMessage, oldRoom, conn, conn.identity, true);
          // Send message to everyone in the new room that the client has joined
          broadcastRoom(serverMessage, newRoom, conn, conn.identity, true);
        }
      }
      // TODO: If client changing to MainHall, server to send RoomContents msg to client (for MainHall) and
      // TODO: ...RoomList message after the RoomChange message.
    }
    else {
      // Unsuccessful. Send a message to the client.
      JSONWriter jsonBuild = new JSONWriter();
      String serverMessage = jsonBuild.buildJSONJoinRoom(conn.identity, oldRoom, oldRoom);
      conn.sendMessage(serverMessage + "\n");
    }
  }

  // Method used for the RoomList protocol
  private synchronized void getRoomList(ServerConnection conn) {
    List<String> roomContents = new ArrayList<String>();

    // Iterate over each room
    ArrayList<ArrayList<String>> roomInformation = new ArrayList<>();

    for (Room r: currentRooms) {
      JSONWriter jsonBuild = new JSONWriter();
      String serverMessage = jsonBuild.buildJsonRoomInfo(r.getRoomName(), r.getRoomSize());
      roomContents.add(serverMessage);
    }

    // Wrap this information in a RoomList json
    JSONWriter jsonBuild = new JSONWriter();
    String roomList = jsonBuild.buildJsonRoomList(roomContents);
    System.out.println(roomList);

    // Send to client
    conn.sendMessage(roomList + "\n");

  }

  // TODO: Method for the Quit protocol.
  private synchronized void quit(ServerConnection conn, String roomID) {
    // Send RoomChange message to all clients in the room
    JSONWriter jsonBuild = new JSONWriter();
    String serverMessage = jsonBuild.buildJSONJoinRoom(conn.identity, roomID, "");
    broadcastRoom(serverMessage, roomID, conn, conn.identity, true);

    // Remove user from the current room (handled by the close() method).
    conn.close();
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
      String msg = ANSI_RED+"---> "+identity+", has entered "+currentRooms.get(roomIndex).getRoomName()+". Be nice!"+ANSI_RESET; // Tells new client where it is. The line below will inform the other clients.
      broadcastRoom(msg, roomID, this, "Server", false); // Broadcasts initial entrance message to all other clients.
      welcome(identity, this); // Generates a welcome JSON string message and flushes it to the client, which will post it on the client's screen.

      // While connection is alive we listen to their socket and broadcast their messages appropriately.
      while (connectionAlive) {
        try {
          // Once we enter this loop we wait for the corresponding client to flush us a message.
          String in = reader.readLine();
          JSONReader read = new JSONReader();
          msg = read.readMSg(in);

          // Listen to incoming packets and pass them to the appropriate server method.
          if (in != null) {
            // Read the JSON string
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(in);
            String type = jsonNode.get("type").asText();
            System.out.format(ANSI_RED+"%nReceived "+"JSON string of type: %s. It is below:%n"+ANSI_RESET, type);

            // Here we have conditionals to interpret the message type and act accordingly.
            if (type.equals("message")) {
              System.out.format("Incoming Message JSON String: %s%n", in);
              broadcastRoom(msg, roomID, null, identity, false);     // "this" -> ignore ourselves in the broadcast
            }
            else if (type.equals("identitychange")) {
              String oldIdentity = identity;
              String newIdentity = jsonNode.get("identity").asText();
              boolean isValidIdentity = verifyIdentity(this, newIdentity);
              String newIdentityMessage = changeIdentity(this, newIdentity, isValidIdentity);
              if (isValidIdentity) {
                System.out.format("CI JSON String: %s%n", newIdentityMessage);
                broadcastRoom(newIdentityMessage, roomID, null, "Server", true);
              }
            }
            else if (type.equals("join")) {
              String newRoom = jsonNode.get("roomid").asText();
              System.out.println("User to join " + newRoom);
              String currentRoom = roomID;
              joinRoom(this, currentRoom, newRoom);
            }
            else if (type.equals("list")) {
              getRoomList(this);
            }
            else if (type.equals("quit")) {
              quit(this, roomID);
            }
          } else {
            //close();
            connectionAlive = false;
          }
        } catch (IOException e) {
          //close();
          connectionAlive = false;
        }
      }
      close();                                        // Close connections of those that left
    }

    public void sendMessage(String msg) {
      writer.print(msg);
      writer.flush();                                 // Empty the buffer and send the data over the network.
    }

    public void close() {
      try {
        // Traverse to the room the client belongs to and remove client from that room
        currentRooms.get(getRoomIndex(roomID)).removeUser(identity);
        disconnect(this);
        reader.close();
        writer.close();
        socket.close();
        this.roomID = "NULL";

      } catch (IOException e) {
        System.out.println(e.getMessage());
      }
    }

  }
}
