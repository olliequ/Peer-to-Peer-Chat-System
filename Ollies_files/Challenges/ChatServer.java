/*
 * Copyright (c)  2021, kvoli
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */


// a chat server that can accept multiple connections on port 6379
// upon receiving a message, we broadcast to all other connected clients.

/*
 * server------- client
 *  |
 *  |
 *  [listen] <----[syn]
 *  |         synack
 *  [recv syn] ---> [recv synack]
 *            <----
 *            ack
 *
 *  |               |
 *    [established]
 *  */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

// support leaving/joining without causing an exception
public class ChatServer {
  private boolean alive = false;
  public static final int PORT = 6379;
  private List<ChatConnection> connectionList = new ArrayList<>();

  public static void main(String[] args) {
    ChatServer server = new ChatServer();
    server.handle();
  }

  private void leave(ChatConnection conn) {
    synchronized (connectionList) {
      connectionList.remove(conn);
    }
    broadcast(String.format("%d has left the chat\n", conn.socket.getPort()), conn);
  }

  private void join(ChatConnection conn) {
    synchronized (connectionList) {
      connectionList.add(conn);
    }
    broadcast(String.format("%d has joined the chat\n", conn.socket.getPort()), null);
  }

  private void broadcast(String message, ChatConnection ignored) {
    synchronized (connectionList) {
      System.out.println(message);
      for (ChatConnection c : connectionList) {
        if (ignored == null || !ignored.equals(c))
          c.sendMessage(message);
      }
    }
  }

  private void handle() {
    ServerSocket serverSocket;
    try {
      serverSocket = new ServerSocket(PORT);
      System.out.printf("listening on port %d\n", PORT);
      alive = true;
      while (alive) {
        Socket socket = serverSocket.accept();
        // do some stuff here with socket
        ChatConnection connection = new ChatConnection(socket);
        connection.start();
        join(connection);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private class ChatConnection extends Thread {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private boolean connectionAlive = false;

    public ChatConnection(Socket socket) throws IOException {
      this.socket = socket;
      this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      this.writer = new PrintWriter(socket.getOutputStream());
    }

    @Override
    public void run() {
      connectionAlive = true;
      while (connectionAlive) {
        try {
          String in  = reader.readLine();
          if (in != null) {
            broadcast(String.format("%d: %s\n", socket.getPort(), in), this);
          } else {
            connectionAlive = false;
          }
        } catch (IOException e) {
          connectionAlive = false;
        }
      }
      close();
    }

    public void close() {
      try {
        leave(this);
        socket.close();
        reader.close();
        writer.close();
      } catch (IOException e) {
        System.out.println(e.getMessage());
      }
    }

    public void sendMessage(String message) {
      writer.print(message);
      writer.flush();
    }
  }
}
