package com.kvoli;

import com.kvoli.base.Base;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


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
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode jsonNode = objectMapper.readTree(in);
                    String type = jsonNode.get("type").asText();

                    // Received MESSAGE from server
                    if (type.equals("message")) {
                        String content = jsonNode.get("content").asText();
                        String identity = jsonNode.get("identity").asText();
                        System.out.println(identity + ": " + content);
                    }

                    // Received NEWIDENTITY from server
                    else if (type.equals("newidentity")) {
                        String former = jsonNode.get("former").asText();
                        String identity = jsonNode.get("identity").asText();
                        if (former.equals(identity))  {
                            System.out.println("Requested identity invalid or in use.");
                        }
                        else {
                            System.out.println(former + " is now " + identity);
                        }
                    }

                    // Received ROOMCHANGE from server
                    // TODO: Handle ROOMCHANGE
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
