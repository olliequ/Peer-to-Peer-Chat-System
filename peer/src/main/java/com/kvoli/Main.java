package com.kvoli;



public class Main {
    public static void main(String[] args) {
        // Create a new peer instance
        Peer peer = new Peer();
        peer.handle();

//        try {
//            // Check args passed into server
//            if (args.length == 0) {
//                // Call peer with default port
//                System.out.println("Attempting to build peer on default port.");
//                Peer peer = new Peer();
//                peer.handle();
//            }
//            else if (args.length == 2) {
//                int port = Integer.parseInt(args[1]);
//                System.out.println("Attempting to build peer on port " + port);
//                Peer peer = new Peer(port);
//                peer.handle();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            System.out.println("ERROR: There was an issue parsing your input. Please try again.");
//        }

    }
}
