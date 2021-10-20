package com.kvoli;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class InputThread extends Thread {
    private Peer peer;
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
            String text = "";

            try {
                Scanner keyboard = new Scanner(System.in);
                text = keyboard.nextLine();
            } catch (Exception e) {
                System.out.println("InputThread exception when getting user input.");
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


        }


    }


}
