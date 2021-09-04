package com.kvoli;

import com.kvoli.base.Base;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

public class GetMessageThread extends Thread {
    private Client client;
    private BufferedReader reader;
    private Socket socket;

    public GetMessageThread(Socket socket, Client client) {
        this.client = client;
        this.socket = socket;

        try {
            InputStream serverIn = socket.getInputStream();
            reader = new BufferedReader(new InputStreamReader(serverIn));
        } catch (IOException e) {
            System.out.println("Exception occurred when getting message from server.");
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        boolean gettingMessages = true;
        while (gettingMessages) {
            try {
                String in = reader.readLine();
                if (in != null) {
                    System.out.println(in);
                }

            } catch (IOException e) {
                System.out.println("Exception occurred when reading message.");
                e.printStackTrace();
                try {
                    socket.close();
                } catch (IOException ex) {
                    System.out.println("Exception occurred when closing socket.");
                    ex.printStackTrace();
                }
                break;
            }
        }
    }
}
