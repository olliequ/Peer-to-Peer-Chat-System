package com.kvoli;

import java.io.*;
import java.net.Socket;

public class Client {
  private final String serverAddress;
  private final int serverPort;
  private Socket socket;
  private OutputStream serverOut;
  private InputStream serverIn;
  private BufferedReader reader;


  public Client(String serverAddress, int serverPort) {
    this.serverAddress = serverAddress;
    this.serverPort = serverPort;
  }

  public void handle(Client client) {
    if (client.connect())
    {
      boolean connected = true;
      System.out.println("Connection established to server.");
      new GetMessageThread(socket, this).start();
      new SendMessageThread(socket, this).start();

    }
    else {
      System.out.println("Could not establish connection to server.");
    }
  }


  private boolean connect() {
    try {
      this.socket = new Socket(serverAddress, serverPort);
      // System.out.println("Client port is " + socket.getLocalPort());
      this.serverOut = socket.getOutputStream();
      this.serverIn = socket.getInputStream();
      this.reader = new BufferedReader(new InputStreamReader(serverIn));

      return true;

    } catch (IOException e) {
      // e.printStackTrace();
    }
    return false;
  }


  private void close() {
    try {
      socket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}

