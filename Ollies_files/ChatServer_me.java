// a chat server that can accept multiple connections on port 6379
// upon receiving a message, broadcast it to all other connected clients
// support leaving/joining without causing an exception

package Ollies_files;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ChatServer_me {
    private boolean alive = false;
    public static final int PORT = 6379;
    private List<ChatConnection> connectionList = new ArrayList<>();

    public static void main(String[] args) {
        ChatServer_me server = new ChatServer_me();
        server.handle();
    }

    private void leave(ChatConnection conn) {
        broadcast(String.format("%c has left the chat\n", conn.socket.getPort()), conn);
        connectionList.remove(conn);
    }

    private void join(ChatConnection conn) {
        broadcast(String.format("%c has joined the chat\n", conn.socket.getPort()), null);
        connectionList.add(conn);
    }

    private void broadcast(String message, ChatConnection ignored) {
        System.out.println(message);
        for (ChatConnection c : connectionList) {
            if (ignored == null || !ignored.equals(c))
            c.sendMessage(message);
            // given some connection, when sendMessage is called on, on this connection, that connection should the
            // write `message` into its output stream
        }
    }
    // Multiple threads are gonna call the above broadcast() and leave() methods, called from the threaded class
    // inside the run(). And they're all using shared data structure 'connectionList'. If you're in middle of
    // broadcasting message and connection drops, and the handler for that connection (which is in a thread) decides to
    // remove it from the list... now iterating over object in list which is no longer valid as buffers are all closed.
    // So we only want one process to be able to edit shared data structure at once. Java luckily has inbuilt
    // concurrency control datastructures. We can `synchronize` methods so that only one thread can use them at a time.
    // But still not ideal. Solution - we only want one person in the list at any one time.
    //

    private void handle() {
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.printf("Listening on port %d.\n", PORT);
            alive = true;
            while (alive) {
                // once connection to client is established, a socket for it is spun off
                Socket socket = serverSocket.accept();
                // Encapsulate above and start new thread?
                ChatConnection connection = new ChatConnection(socket);
                // Each connection should handle itself - using a thread? Start the connection
                // and then we don't care about it anymore.
                connection.start();
                join(connection); // Added *after* the thread's been started - to be safe.
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ChatConnection extends Thread {
        // inner class can access parent class's variables and methods (even if private)
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
            // manage the connection here.
            // We want to read what someone writes, and then do something with it.
            connectionAlive = true;
            while(connectionAlive) {
                try {
                    String in = reader.readLine();
                    // > types some message -> broadcast out to everyone else
                    if (in != null) {
                        broadcast(String.format("%d: %s\n", socket.getPort(), in), this);
                        // This format prefix's each message with individual port number that we're on, remember we've
                        // handed it off, so it's not 6379 anymore, because the socket is now seperate.
                    } else {
                        connectionAlive = false;
                    }
                } catch (IOException e) {
                    connectionAlive = false;
                    e.printStackTrace();
                }
            }
            close(); // Cleans everything up and calls leave method. When leave() is called we want to know
                     // that person has exited.
        }

        private void close() { // Close all our streams
            try {
                leave(this);
                reader.close();
                writer.close();
                socket.close();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }

        private void sendMessage(String message) {
            writer.print(message);
            writer.flush(); // Ensures the buffer is emptied and the previous bits inside are sent over the network.
        }
    }
}

