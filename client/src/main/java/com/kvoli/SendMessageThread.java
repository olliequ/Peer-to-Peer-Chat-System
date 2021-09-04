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

            // Wrap this input into JSON
            ClientPackets.Message message = new ClientPackets.Message(text);
            try {
                String x = objectMapper.writeValueAsString(message);
                writer.println(x);
                writer.flush();

            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

        }


    }

}
