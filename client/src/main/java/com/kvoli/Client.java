package com.kvoli;

import java.io.*;
import java.net.Socket;

public class Client {
  private final String serverAddress;
  private final int serverPort;
  //protected String Identity = "";
  protected String Identity = "1stEver";
  protected boolean ReadyToRock = false;
  protected String currentRoom = "Wrong room";
  protected Socket socket;
  protected OutputStream ToServer;
  protected InputStream FromServer;
  private BufferedReader reader;
  private boolean Connected = false;
  public static final String ANSI_GREEN = "\u001B[32m";
  public static final String ANSI_RESET = "\u001B[0m";

  protected String RequestedIdentity = "";
  protected boolean gotWelcome = false;
  protected String roomToCreate = "";
  protected String roomToDelete = "";
  protected boolean clientToQuit = false;
  protected boolean clientToCreateRoom = false;
  protected boolean clientListCmdStatus = false;
  protected boolean clientWantsToDelete = false;

  public Client(String serverAddress, int serverPort) {
    this.serverAddress = serverAddress;
    this.serverPort = serverPort;
  }

  public void handle(Client client) {
    if (client.connect())
    {
      this.Connected = true;
      System.out.println(ANSI_GREEN+"\nConnection successfully established to the server.\n"+ ANSI_RESET);
      new GetMessageThread(this).start();
      new SendMessageThread(this).start();
    }
    else {
      System.out.println("Could not establish connection to server.");
    }
  }

  private boolean connect() {
    try {
      this.socket = new Socket(serverAddress, serverPort);
      this.ToServer = socket.getOutputStream();
      this.FromServer = socket.getInputStream();
      this.reader = new BufferedReader(new InputStreamReader(FromServer));
      return true;
    } catch (IOException e) {
        e.printStackTrace();
    }
    return false;
  }

  public String getIdentity() {
      return this.Identity;
  }

  public void setIdentity(String identity) {
    this.Identity = identity;
  }

  public String getRequestedIdentity() {
    return this.RequestedIdentity;
  }
  public void setRequestedIdentity(String requestedIdentity) {
    this.RequestedIdentity = requestedIdentity;
  }

  public boolean getWelcomeStatus() {
    return this.gotWelcome;
  }
  public void setWelcomeStatus(boolean welcome) {
    this.gotWelcome = welcome;
  }

  public String getRoomToCreate() {
    return this.roomToCreate;
  }
  public void setRoomToCreate(String roomID) {
    this.roomToCreate = roomID;
  }

  public boolean getReadyToRock() {
    return this.ReadyToRock;
  }
  public void setReadyToRock(boolean readyOrNa) {
    this.ReadyToRock = readyOrNa;
  }

  public String getCurrentRoom() {
    return this.currentRoom;
  }
  public void setCurrentRoom(String CurrentRoom) {
    this.currentRoom = CurrentRoom;
  }

  public String getRoomToDelete() {
    return this.roomToDelete;
  }
  public void setRoomToDelete(String room) {
    this.roomToDelete = room;
  }

  public boolean getClientToQuit() {
    return this.clientToQuit;
  }
  public void setClientToQuit(boolean quitStatus) {
    this.clientToQuit = quitStatus;
  }

  protected boolean getClientToCreateRoom() {
    return this.clientToCreateRoom;
  }
  protected void setClientToCreateRoom(boolean toCreate) {
    this.clientToCreateRoom = toCreate;
  }

  protected boolean getListCommandStatus() {
    return this.clientListCmdStatus;
  }
  protected void setListCommandStatus(boolean status) {
    this.clientListCmdStatus = status;
  }

  protected boolean getDeleteStatus() {
    return this.clientWantsToDelete;
  }
  protected void setDeleteStatus(boolean status) {
    this.clientWantsToDelete = status;
  }


  protected void close() {
    try {
      System.out.println("Disconnected from "+socket.getInetAddress());
      socket.close();
      System.exit(0);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}

