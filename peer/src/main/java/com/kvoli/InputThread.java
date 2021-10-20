package com.kvoli;

import com.kvoli.base.ClientPackets;
import com.kvoli.base.JSONWriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class InputThread extends Thread {
    private Peer peer;
    private PrintWriter writer;
    private BufferedReader reader;
    private Socket socket;
    private boolean getUserInput = true;
    //private String clientID;

    public InputThread(Peer peer) {
        this.peer = peer;
        this.socket = peer.socket;
    }

    /**
     * Currently only one command
     * Usage: #connect PORTNUMBER
     * IP address is hardcoded to make testing easier.
     */

    @Override
    public void run() {
        System.out.println("Welcome. Please issue a command. ");
        while (getUserInput) {
            JSONWriter jWrite = new JSONWriter();
            String text = "";

            try {
                Scanner keyboard = new Scanner(System.in);
                text = keyboard.nextLine();
            } catch (Exception e) {
                System.out.println("InputThread exception when getting user input.");
            }

            // If we've established a connection with another peer ('server') then build a PrintWriter
            // We cannot build this without establishing a connection.
            if (peer.connectionEstablishedWithServer == true) {
                writer = new PrintWriter(peer.ToConnectedPeer, true);
            }


            // Command parsing
            if (text.contains("#connect")) {
                String input = text.replaceAll("#connect", "");
                input = input.stripLeading();

                // TODO: destination IP is currently hardcoded to make testing easier.
                String destIP = "localhost";
                int destPort = Integer.parseInt(input);

                System.out.println("DEBUG: Trying to connect to " + destIP + " " + destPort);
                peer.connectToPeer(destIP, destPort);

            }

            else if (text.contains("#list") && peer.connectionEstablishedWithServer) {
                ClientPackets.List listRoom = new ClientPackets.List();
                this.peer.clientListCmdStatus = true;

                String msg = jWrite.buildListMsg(listRoom);
                writer.println(msg);
                writer.flush();






            }


        }


    }


}
