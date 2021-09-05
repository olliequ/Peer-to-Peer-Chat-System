package com.kvoli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvoli.base.Base;
import com.kvoli.base.ClientPackets;
import com.kvoli.base.JSONReader;


import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;


public class SendMessageThread extends Thread {
    private Client client;
    private PrintWriter writer;
    private Socket socket;

    public SendMessageThread(Socket socket, Client client) {
        try{
            OutputStream clientOut = socket.getOutputStream();
            writer = new PrintWriter(clientOut, true);
        } catch (IOException e) {
            System.out.println("Exception occurred during client output.");
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        boolean sendingMessages = true;

        while (sendingMessages) {
            ObjectMapper objectMapper = new ObjectMapper();

            Scanner keyboard = new Scanner(System.in);
            String text = keyboard.nextLine();

            // First parse the client input. Are they issuing a server command?
            // Server command IDENTITYCHANGE
            if (text.contains("#identitychange")) {
                // Remove the command and then wrap the new identity into a JSON.
                String identity = text.replaceAll("#identitychange", "");
                identity = identity.stripLeading();

                ClientPackets.IdentityChange identityChange = new ClientPackets.IdentityChange(identity);
                try {
                    String msg = objectMapper.writeValueAsString(identityChange);
                    System.out.println(msg);
                    writer.println(msg);
                    writer.flush();
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }

            // Server command JOIN
            // TODO
            else if (text.contains("#join")) {
                String newRoom = text.replaceAll("#join", "");
                newRoom = newRoom.stripLeading();

                ClientPackets.Join joinRoom = new ClientPackets.Join(newRoom);
                try {
                    String msg = objectMapper.writeValueAsString(joinRoom);
                    System.out.println(msg);
                    writer.println(msg);
                    writer.flush();
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }

            }

            // Else they aren't issuing a command. Assume it's a standard message.
            else {
                // Wrap this input into JSON
                ClientPackets.Message message = new ClientPackets.Message(text);
                try {
                    String x = objectMapper.writeValueAsString(message);
                    System.out.println(x);
                    writer.println(x);
                    writer.flush();

                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }

    }


}
