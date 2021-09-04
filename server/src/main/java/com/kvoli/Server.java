package com.kvoli;

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


  // Broadcast the connection of a new user.
  private synchronized void connect(ServerConnection conn) {
    // Broadcast the connection of a new user.
    broadcast(String.format("%d has joined the chat.\n", conn.socket.getPort()), null);
    currentConnections.add(conn);
  }

  // Broadcast the disconnection of a new user.
  private synchronized void disconnect(ServerConnection conn) {
    broadcast(String.format("%d has left the chat.\n", conn.socket.getPort()), conn);
    currentConnections.remove(conn);
  }


  // Display a welcome message to the new user along with list of current rooms.
  private void welcome(ServerConnection conn) {
    conn.sendMessage("Welcome to the server. The current rooms are: \n");

    for (Room room: currentRooms) {
      conn.sendMessage(room.getRoomName());
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
  private synchronized void broadcastRoom(String message, int roomID, ServerConnection ignored, String ID) {
    for (ServerConnection c : currentConnections) {
      if (c.roomID == roomID) {
        if (ignored == null || !ignored.equals(c)) {
          // We want to broadcast the client message to everyone else in the room.
          // First we need to build a JSON string out of the client message and append ID.
          JSONWriter jsonBuild = new JSONWriter();
          String serverMessage = jsonBuild.buildJSON(message, ID);
          System.out.println("SERVER: " + serverMessage);             // Optional: used for debugging

          // Now broadcast the JSON string to everyone in the room.
          c.sendMessage(serverMessage + "\n");
        }
      }
    }
  }


  private void createRoom(int roomID, String roomName) {
    currentRooms.add(new Room(roomID, roomName));
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
        currentRooms.get(0).addUser(socket.getPort());

        // Testing purposes: show contents of the current room
        //System.out.println(currentRooms.get(0).getRoomContents());

        // Perform operations with the socket (client)
        // Now that the socket is assigned to a room, their messages should only be restricted to that room.
        // Note: by default, new users are added to the Main Hall (roomID = 0).
        ServerConnection currentConnection = new ServerConnection(socket, socket.getPort(), 0);
        welcome(currentConnection);

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

    public ServerConnection(Socket socket, int port, int roomID) throws IOException {
      this.socket = socket;
      this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      this.writer = new PrintWriter(socket.getOutputStream());
      this.port = port;
      this.roomID = roomID;
    }

    @Override
    public void run() {
      // Manage the connection here
      connectionAlive = true;

      // While connection is alive we listen to their socket and broadcast their messages appropriately.
      while (connectionAlive) {
        try {
          String in = reader.readLine();
          JSONReader read = new JSONReader();
          String msg = read.readMSg(in);

          // We received a message. Broadcast it to everyone in the clients room.
          if (in != null) {
            String id = Integer.toString(socket.getPort());
            broadcastRoom(msg, roomID, this, id);     // "this" -> ignore ourselves in the broadcast

            // messageServer(this, msg , roomID, this, id);
            //broadcastRoom(String.format("%d: %s \n", socket.getPort(), in), roomID, this);

          } else {
            connectionAlive = false;
          }
        } catch (IOException e) {
          connectionAlive = false;
        }
      }
      // When someone has left we need to know. We'll know this because their connection won't be alive anymore.
      close();
    }

    public void sendMessage(String msg) {
      writer.print(msg);
      writer.flush();                                 // Empty the buffer and send the data over the network.
    }

    public void close() {
      // Close all streams
      try {
        disconnect(this);
        reader.close();
        writer.close();
        socket.close();
      } catch (IOException e) {
        System.out.println(e.getMessage());
      }
    }

    // TESTER METHOD to remove a client from a room. Used to test whether the broadcastRoom method actually works.
    // To be deleted.
    public void removeFromRoom() {
      this.roomID = -1;
    }
  }
}








// *****************************************************************************************************************
// *****************************************************************************************************************
// *****************************************************************************************************************
// ****************************************** NOT IN USE ***********************************************************
//  /**
//   * NOT CURRENTLY IN USE. Will delete soon but keeping it for now for reference.
//   * @param conn       The sender
//   * @param message    The senders message
//   * @param roomID     The senders room
//   * @param ignored
//   */
////  private synchronized void messageServer(ServerConnection conn, String message, int roomID, ServerConnection ignored, String ID) {
//    broadcastRoom(message, roomID, ignored, ID);
//
//
//    // REDUNDANT CODE - WILL DELETE SOON.
//    // Quick and dirty way for someone to remove themselves from a room.
//    // Very poor code but did the job for a quick test. Needs to be re-done.
//    if (message.contains("REMOVE")){
//      conn.removeFromRoom();
//      System.out.println(conn.port + " was removed from a room.");            // Printed server side
//    }
//    else {
//      broadcastRoom(message, roomID, ignored, ID);
//    }
//  }
