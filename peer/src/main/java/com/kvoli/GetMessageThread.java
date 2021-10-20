package com.kvoli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class GetMessageThread extends Thread {
    private Peer peer;
    private BufferedReader reader;
    private Socket socket;
    boolean getPeerMessages = true;

    public GetMessageThread(Peer peer) {
        this.peer = peer;
        this.socket = peer.socket;
        reader = new BufferedReader(new InputStreamReader(peer.FromConnectedPeer));
    }

    @Override
    public void run() {
        while (getPeerMessages) {
            try {
                String in = reader.readLine();
                if (in != null) {
                    System.out.println(in);
                }
            } catch (IOException e) {
                System.out.println("GetMessageThread exception when retrieving messages from peer");
            }
        }
    }

}
