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
    public String ParentClientID = "";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_RESET = "\u001B[0m";

    public SendMessageThread(Client client) {
        this.client = client;
        this.socket = client.socket;
        writer = new PrintWriter(client.ToServer, true);
    }

    @Override
    public void run() {
        boolean sendingMessages = true;


        while (sendingMessages) {
            ObjectMapper objectMapper = new ObjectMapper();
            ParentClientID = this.client.getIdentity();
            String text = "";

                Scanner keyboard = new Scanner(System.in);
                text = keyboard.nextLine();

//            if (this.client.getWelcomeStatus() == true) {
//                System.out.print(ParentClientID + ": ");
//                Scanner keyboard = new Scanner(System.in);
//                text = keyboard.nextLine();
//            }

            // First parse the client input. Are they issuing a server command?
            // Client command IDENTITYCHANGE
            if (text.contains("#identitychange")) {
                // Remove the command and then wrap the new identity into a JSON.
                String identity = text.replaceAll("#identitychange", "");
                identity = identity.stripLeading(); // Tom
                this.client.setRequestedIdentity(identity);
                ClientPackets.IdentityChange identityChange = new ClientPackets.IdentityChange(identity);
                try {
                    String msg = objectMapper.writeValueAsString(identityChange);
                    //System.out.format("IC JSON string flushed to Server: %s", msg);
                    writer.println(msg);
                    writer.flush();             // Why not printing to all clients? writer is
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }

            // Client command JOIN
            else if (text.contains("#join")) {
                String newRoomMsg = text.replaceAll("#join", "");
                newRoomMsg = newRoomMsg.stripLeading();

                ClientPackets.Join joinRoom = new ClientPackets.Join(newRoomMsg);
                try {
                    String msg = objectMapper.writeValueAsString(joinRoom); // Make a JSON object called `msg`.
                    System.out.println(msg);
                    writer.println(msg);
                    writer.flush();
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }

            // Client command LIST
            else if (text.contains("#list")) {
                String listMsg = text.replaceAll("#list", ""); // These 2 lines not needed as listMsg isn't an argument?
                listMsg = listMsg.stripLeading();
                ClientPackets.List listRoom = new ClientPackets.List();

                try {
                    String msg = objectMapper.writeValueAsString(listRoom);
                    writer.println(msg);
                    writer.flush();
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }

            else if (text.contains("#createroom")) {
                String createRoomMsg = text.replaceAll("#createroom", "");
                createRoomMsg = createRoomMsg.stripLeading();
                this.client.setClientToCreateRoom(true);
                ClientPackets.CreateRoom createRoom = new ClientPackets.CreateRoom(createRoomMsg);
                try {
                    String msg = objectMapper.writeValueAsString(createRoom);
                    //System.out.println(msg);
                    writer.println(msg);
                    writer.flush();
                    if (createRoomMsg.equals("")) {
                        this.client.setRoomToCreate("EmptyString");                 // Update client variable.
                    }
                    else {
                        this.client.setRoomToCreate(createRoomMsg);                 // Update client variable.
                    }
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }

            // Client command: #who
            // Server to return: roomcontents
            else if (text.contains("who")) {
                String createWhoMsg = text.replaceAll("#who", "");
                createWhoMsg = createWhoMsg.stripLeading();

                ClientPackets.Who who = new ClientPackets.Who(createWhoMsg);
                try {
                    String msg = objectMapper.writeValueAsString(who);
                    writer.println(msg);
                    writer.flush();
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }


            // Client command: #quit
            else if (text.contains("#quit")) {
                String quit = text.replaceAll("#quit", "");
                quit = quit.stripLeading();
                this.client.setClientToQuit(true);                  // Tell getMessageThread that we want to leave.

                ClientPackets.Quit quitMsg = new ClientPackets.Quit();

                try {
                    String msg = objectMapper.writeValueAsString(quitMsg);
                    writer.println(msg);
                    writer.flush();
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }


            else if (text.contains("#delete")) {
                String delete = text.replaceAll("#delete", "");
                delete = delete.stripLeading();
                ClientPackets.Delete deleteMsg = new ClientPackets.Delete(delete);

                this.client.setRoomToDelete(delete);
                //System.out.println("The room to delete is " + roomToDelete);

                try {
                    String msg = objectMapper.writeValueAsString(deleteMsg);
                    writer.println(msg);
                    writer.flush();
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }


            // Else they aren't issuing a command. Assume it's a standard message.
            else {
                if (!text.equals("")) {
                    // Wrap this input into JSON.
                    ClientPackets.Message message = new ClientPackets.Message(text);
                    try {
                        String x = objectMapper.writeValueAsString(message);
                        //System.out.println(x);
                        writer.println(x);      // Send `x` to the writer, and flush to actually send over the network.
                        writer.flush();         // Why doesn't flushing this to the server not also make appear on own screen -- as it's still going to serverInputStream!

                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                }
                else if (text.equals("") && this.client.getReadyToRock()) {
                    System.out.println(ANSI_YELLOW+"---> You can't send nothing! Try something else."+ANSI_RESET);
                }
            }
        }


    }


}
