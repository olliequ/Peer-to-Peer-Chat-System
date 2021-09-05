package com.kvoli;

import com.fasterxml.jackson.databind.JsonNode;
import com.kvoli.base.*;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
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
  private int roomID = 0;
  private int guestCount = 0;                   // Used to identify new users. E.g. "guest5 has joined the server".


  // Broadcast the connection of a new user.
  private synchronized void connect(ServerConnection conn) {
    // Broadcast the connection of a new user. TEMPORARILY DISABLED
    //broadcast(String.format("%d has joined the chat.\n", conn.socket.getPort()), null);
    currentConnections.add(conn);
  }

  // Broadcast the disconnection of a new user.
  private synchronized void disconnect(ServerConnection conn) {
    broadcast(String.format("%d has left the chat.\n", conn.socket.getPort()), conn);
    currentConnections.remove(conn);
  }


  // Display a welcome message to the new user along with list of current rooms.
  private void welcome(String clientIdentity, ServerConnection conn) {
    //conn.sendMessage("Welcome to the server. The current rooms are: \n");
    //String json = "{\"type\":\"welcome\",\"content\":\"james-bond007\"}";
    String welcomeClient = "Connected to localhost as " + clientIdentity;

    JSONWriter jsonBuild = new JSONWriter();
    String serverMessage = jsonBuild.buildJSON(welcomeClient, "Server");
    //System.out.println(serverMessage);
    conn.sendMessage(serverMessage + ". \n");

    for (Room room: currentRooms) {
      // String ln = "This is a test string";
      String ln = room.getRoomName() + " has " + room.getRoomSize() + " guests.";
      String roomInfo = jsonBuild.buildJSON(ln, "Server");
      System.out.println(roomInfo);
      conn.sendMessage(roomInfo);
      conn.sendMessage("\n");
    }
  }


  // Broadcast a server message (CONNECT/DISCONNECT) to everyone in a room except the ignored person (usually yourself).
  // A bit redundant and can be merged with 'broadcastRoom' method down below...
  private synchronized void broadcast(String message, ServerConnection ignored) {
    for (ServerConnection c : currentConnections) {
      // If not ignored, send message
      if (ignored == null || !ignored.equals(c)) {
        c.sendMessage(message);
      }
    }
  }


  /**
   * Method to broadcast to users within the same room as the messenger.
   * @param message   User message
   * @param roomID    The roomID to broadcast the message to
   * @param ignored
   */
  private synchronized void broadcastRoom(String message, int roomID, ServerConnection ignored, String ID, boolean isJson) {
    for (ServerConnection c : currentConnections) {
      if (c.roomID == roomID && isJson == false) {
        if (ignored == null || !ignored.equals(c)) {
          // We want to broadcast the client message to everyone else in the room.
          // First we need to build a JSON string out of the client message and append ID.
          JSONWriter jsonBuild = new JSONWriter();
          String serverMessage = jsonBuild.buildJSON(message, ID);
          //System.out.println("SERVER: " + serverMessage);             // Optional: used for debugging

          // Now broadcast the JSON string to everyone in the room.
          c.sendMessage(serverMessage + "\n");
        }
      }

      // If we're already passing in a JSON then we don't need to build a JSON message. Just broadcast it.
      else if (c.roomID == roomID && isJson == true) {
        if (ignored == null || !ignored.equals(c)) {
          // Broadcast the JSON string to everyone in the room.
          c.sendMessage(message + "\n");
        }
      }
    }
  }


  // One way direct message from server to one and only one client.
  // SOMEWHAT REDUNDANT FOR NOW.
//  private synchronized void directMessage(String message, int roomID, ServerConnection conn) {
////    JSONWriter jsonBuild = new JSONWriter();
////    String serverMessage = jsonBuild.buildJSON(message, ID);
//    conn.sendMessage(message + "\n");
//  }


  private void createRoom(int roomID, String roomName) {
    currentRooms.add(new Room(roomID, roomName));
  }


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
    if (!isValid) {
      // Send fail message to client
      JSONWriter jsonBuild = new JSONWriter();
      String serverMessage = jsonBuild.buildJSONNewID(conn.identity, conn.identity);
      conn.sendMessage(serverMessage + "\n");
      return serverMessage;
    }

    else {
      // Change identity of client.
      String oldID = conn.identity;
      conn.identity = newIdentity;

      // Update client room roomContents with new identity.
      currentRooms.get(conn.roomID).changeUserID(oldID, newIdentity);
      JSONWriter jsonBuild = new JSONWriter();
      String serverMessage = jsonBuild.buildJSONNewID(oldID, conn.identity);
      return serverMessage;
    }
  }


  // TODO
  private synchronized void joinRoom(ServerConnection conn, String currentRoom, String newRoom) {
    // First check if the new 'room' is even valid
    for (Room r: currentRooms) {
      if (!r.getRoomName().equals(newRoom)) {
        System.out.println("This room doesn't exist.");
        break;
      }
      else {
        // Remove the client from their current room.

        // Move the client to the new room.
      }
    }
  }



  // Listen for new connections and create the initial room
  protected void handle() {
    // First create the main hall
    createRoom(roomID, "Main Hall");
    roomID += 1;

    // For testing purposes I created a second room
    createRoom(roomID, "Second room");
    roomID += 1;

    // Now handle connections to the server
    ServerSocket serverSocket;
    try {
      // We want to bind serverSocket to a port. That port should accept connections within an infinite loop.
      serverSocket = new ServerSocket(PORT);
      System.out.printf("Listening on port %d \n", PORT);
      acceptConnections = true;                                   // Listen for connections

      while (acceptConnections) {
        // Accepted a connection. Move them to the main hall room.
        Socket socket = serverSocket.accept();
        System.out.println("Accepted connection from socket: " + socket.getPort());


        // Testing purposes: show contents of the current room
        //System.out.println(currentRooms.get(0).getRoomContents());

        // Perform operations with the socket (client)
        // Now that the socket is assigned to a room, their messages should only be restricted to that room.
        // Note: by default, new users are added to the Main Hall (roomID = 0).
        guestCount += 1;
        String clientName = "guest" + guestCount;
        currentRooms.get(0).addUser(clientName);

        ServerConnection currentConnection = new ServerConnection(socket, clientName, 0);
        //welcome(currentConnection);

        // Start a connection which will have its own thread of execution. Then we don't care about it anymore.
        // The connection will be able to handle itself.
        currentConnection.start();
        connect(currentConnection);
      }

    } catch (IOException e) {
      System.out.println("Server exception occurred.");
      e.printStackTrace();
    }
  }


  /**
   * All current clients have a 'ServerConnection' which is used to listen to each client.
   * At a low level, each client is identified by their port number.
   * TODO: Make it so that clients can assign their own identifier.
   * Each client belongs to a room (they are designated a roomID).
   */
  private class ServerConnection extends Thread {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private boolean connectionAlive = false;
    private int port;
    private int roomID;
    private String identity;

    public ServerConnection(Socket socket, String identity, int roomID) throws IOException {
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
      //String identity = Integer.toString(socket.getPort());
      welcome(identity, this);
      String msg = identity + " moves to " + currentRooms.get(roomID).getRoomName();
      broadcastRoom(msg, roomID, null, "Server", false);     // "this" -> ignore ourselves in the broadcast

      // While connection is alive we listen to their socket and broadcast their messages appropriately.
      while (connectionAlive) {
        try {
          String in = reader.readLine();
          JSONReader read = new JSONReader();
          msg = read.readMSg(in);

          // Listen to incoming packets and pass them to the appropriate server method.
          if (in != null) {
            // Read the JSON string
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(in);
            String type = jsonNode.get("type").asText();
            System.out.println(type);

            // Here we have conditionals to interpret the message type and act accordingly.
            // For MESSAGE
            if (type.equals("message")) {
              broadcastRoom(msg, roomID, this, identity, false);     // "this" -> ignore ourselves in the broadcast
            }

            // For IDENTITYCHANGE
            else if (type.equals("identitychange")) {
              String oldIdentity = identity;
              String newIdentity = jsonNode.get("identity").asText();
              boolean isValidIdentity = verifyIdentity(this, newIdentity);
              String newIdentityMessage = changeIdentity(this, newIdentity, isValidIdentity);

              if (isValidIdentity) {
                broadcastRoom(newIdentityMessage, roomID, null, "Server", true);
              }
            }


            // For JOIN
            // TODO: HANDLE JOIN ROOM LOGIC (SEE METHOD)
            else if (type.equals("join")) {
              String newRoom = jsonNode.get("roomid").asText();
              System.out.println("User to join " + newRoom);
              int currentRoom = roomID;
              //joinRoom(this, roomID, newRoom);
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
        currentRooms.get(roomID).removeUser(identity);
        disconnect(this);
        reader.close();
        writer.close();
        socket.close();
        this.roomID = -1;

      } catch (IOException e) {
        System.out.println(e.getMessage());
      }
    }

  }
}
