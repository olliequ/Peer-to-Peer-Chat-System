package com.kvoli;

//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvoli.base.Base;
import com.kvoli.base.ClientPackets;
import com.kvoli.base.JSONReader;
import com.kvoli.base.JSONWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.io.Console;


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
            // ObjectMapper objectMapper = new ObjectMapper();
            JSONWriter jWrite = new JSONWriter();
            ParentClientID = this.client.getIdentity();
            String text = "";

//            try {
//                Thread.sleep(500);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            Console con = System.console();
//            text = con.readLine("["+this.client.getCurrentRoom()+"] "+this.client.getIdentity()+"> ");

            try {
                Scanner keyboard = new Scanner(System.in);
                text = keyboard.nextLine();
            }
            catch (Exception e) {
            }

            // First we parse the client scanner input. Are they issuing a server command?

            // Client command IDENTITYCHANGE
            if (text.contains("#identitychange")) {
                // Remove the command and then wrap the new identity into a JSON.
                String identity = text.replaceAll("#identitychange", "");
                identity = identity.stripLeading(); // Tom
                this.client.setRequestedIdentity(identity);

                ClientPackets.IdentityChange identityChange = new ClientPackets.IdentityChange(identity);
                String msg = jWrite.buildIdentityChangeMsg(identityChange);
                writer.println(msg);
                writer.flush();
            }

            // Client command JOIN
            else if (text.contains("#join")) {
                String newRoomMsg = text.replaceAll("#join", "");
                newRoomMsg = newRoomMsg.stripLeading();

                ClientPackets.Join joinRoom = new ClientPackets.Join(newRoomMsg);
                String msg = jWrite.buildJoinMsg(joinRoom);
                writer.println(msg);
                writer.flush();

            }

            // Client command LIST
            else if (text.contains("#list")) {
                String listMsg = text.replaceAll("#list", ""); // These 2 lines not needed as listMsg isn't an argument?
                listMsg = listMsg.stripLeading();
                ClientPackets.List listRoom = new ClientPackets.List();
                this.client.setListCommandStatus(true);                 // Store variable that we made a list command

                String msg = jWrite.buildListMsg(listRoom);
                writer.println(msg);
                writer.flush();

            }

            else if (text.contains("#createroom")) {
                String createRoomMsg = text.replaceAll("#createroom", "");
                createRoomMsg = createRoomMsg.stripLeading();
                this.client.setClientToCreateRoom(true);
                ClientPackets.CreateRoom createRoom = new ClientPackets.CreateRoom(createRoomMsg);

                String msg = jWrite.buildCreateRoomMsg(createRoom);
                writer.println(msg);
                writer.flush();
                if (createRoomMsg.equals("")) {
                    this.client.setRoomToCreate("EmptyString");                 // Update client variable.
                }
                else {
                    this.client.setRoomToCreate(createRoomMsg);                 // Update client variable.
                }
            }

            // Client command: #who. The server returns a roomcontents JSON.
            else if (text.contains("#who")) {
                String createWhoMsg = text.replaceAll("#who", "");
                createWhoMsg = createWhoMsg.stripLeading();
                ClientPackets.Who who = new ClientPackets.Who(createWhoMsg);
                String msg = jWrite.buildWhoMsg(who);
                writer.println(msg);
                writer.flush();

            }

            // Client command: #quit
            else if (text.contains("#quit")) {
                String quit = text.replaceAll("#quit", "");
                quit = quit.stripLeading();
                this.client.setClientToQuit(true);                  // Tell getMessageThread that we want to leave.
                ClientPackets.Quit quitMsg = new ClientPackets.Quit();      // No param needed

                String msg = jWrite.buildQuitMsg(quitMsg);
                writer.println(msg);
                writer.flush();

            }

            else if (text.contains("#delete")) {
                String delete = text.replaceAll("#delete", "");
                delete = delete.stripLeading();
                ClientPackets.Delete deleteMsg = new ClientPackets.Delete(delete);

                this.client.setRoomToDelete(delete);
                this.client.setDeleteStatus(true);
//                System.out.println("DEBUG SEND " + delete + " " + this.client.getLocalRoomList().contains(delete));
//                for (String room : this.client.getLocalRoomList()) {
//                    System.out.println("ROOM " + room);
//                }

                if (this.client.getLocalRoomList().contains(delete)) {
                    this.client.setRoomInLocalRoomList(true);
                } else {
                    this.client.setRoomInLocalRoomList(false);
//                    System.out.println("ROOM NOT IN LOCAL ");
                }



                //System.out.println("The room to delete is " + roomToDelete);

                String msg = jWrite.buildDeleteMessage(deleteMsg);
                writer.println(msg);
                writer.flush();

            }

            // Else they aren't issuing a command. Assume it's a standard message.
            else {
                if (!text.equals("")) {
                    // Wrap this input into JSON.
                    ClientPackets.Message message = new ClientPackets.Message(text);
                    String msg = jWrite.buildMessage(message);
                    writer.println(msg);      // Send `msg` to the writer, and flush to actually send over the network.
                    writer.flush();         // Why doesn't flushing this to the server not also make appear on own screen -- as it's still going to serverInputStream!

                }
                else if (text.equals("") && this.client.getReadyToRock()) {
                    System.out.println(ANSI_YELLOW+"---> You can't send nothing! Try something else."+ANSI_RESET);
                }
            }
        }
    }
}
