package com.kvoli;



public class Main {
    public static void main(String[] args) {
        int defaultPort = 4444;

        try {
            // If no ports are provided then the "server" port should be 4444 AND the default port is emphemeral.
            // Using port 0 tells the OS to randomly allocate a port.
            if (args.length == 0) {
                System.out.println("Attempting to build peer on server (listening) port 4444 and emphemeral port");
                Peer peer = new Peer(defaultPort, 0);
                peer.handle();
            }

            //  If both ports are provided
            else if (args.length == 2) {
                int port = Integer.parseInt(args[0]);
                int localport = Integer.parseInt(args[1]);
                System.out.println("Attempting to build peer on port " + port);
                Peer peer = new Peer(port, localport);
                peer.handle();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR: There was an issue parsing your input. Please try again.");
            System.out.println("Example usage:   #java -jar chatpeer.jar 6000 7000");
            System.out.println();
        }
    }
}
