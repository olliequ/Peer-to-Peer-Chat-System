package com.kvoli;



public class Main {
    public static void main(String[] args) {
        int defaultPort = 4444;

        try {
            // Example: java -jar chatpeer.jar
            // If no ports are provided then the "server" port should be 4444 AND the default port is emphemeral.
            // Using port 0 tells the OS to randomly allocate a port.
            if (args.length == 0) {
                try {
                    System.out.println("Attempting to build peer with listening port 4444 and emphemeral outbound port.");
                    Peer peer = new Peer(defaultPort, 0);
                    peer.handle();
                }
                catch (Exception e) {
                    System.out.println("Error when building peer. Please ensure your arguments are correct. ");
                    System.out.println("Example execution:      java -jar chatpeer.jar");
                }
            }

            // Example: java -jar chatpeer.jar -p 5000 OR java -jar chatpeer.jar -i 13333
            else if (args.length == 2) {
                // -p: listening port
                if (args[0].equals("-p")) {
                    try {
                        int port = Integer.parseInt(args[1]);
                        System.out.println("Note: outbound port not provided. Using ephemeral port as outbound port.");
                        System.out.println("Building peer with listening port " + port + " and emphemeral outbound port.");
                        System.out.println();
                        Peer peer = new Peer(port, 0);
                        peer.handle();
                    } catch (Exception e) {
                        System.out.println("The port number after -p is not an integer. Try again.");
                        System.out.println("Example execution:      java -jar chatpeer.jar -p 5000");
                        System.out.println();
                    }
                }
                // -i: outbound port
                else if (args[0].equals("-i")) {
                    try {
                        // Use default port in place of a missing -p
                        int outboundPort = Integer.parseInt(args[1]);
                        System.out.println("Note: listening port not provided. Using default port as listening port.");
                        System.out.println("Building peer with listening port " + defaultPort + " and outbound port " + outboundPort);
                        System.out.println();
                        Peer peer = new Peer(defaultPort, outboundPort);
                        peer.handle();
                    } catch (Exception e) {
                        System.out.println("The port number after -i is not an integer. Try again.");
                        System.out.println("Example execution:      java -jar chatpeer.jar -i 13333");
                        System.out.println();
                    }
                }
            }


            else if (args.length == 4) {
                //System.out.println(args[0] + " " + args[1] + " " + args[2] + " " + args[3]);

                // Example: java -jar chatpeer.jar -p 5000 -i 13333
                if (args[0].equals("-p") && args[2].equals("-i")) {
                    try {
                        int listeningPort = Integer.parseInt(args[1]);
                        int outboundPort = Integer.parseInt(args[3]);
                        System.out.println("Building peer with listening port: " + listeningPort + " and outgoing port " + outboundPort);
                        Peer peer = new Peer(listeningPort, outboundPort);
                        peer.handle();
                    } catch (Exception e) {
                        System.out.println("One of your port numbers is not an integer. ");
                        System.out.println("Example execution:      java -jar chatpeer.jar -p 5000 -i 13333");
                        System.out.println();
                    }
                }

                // Example: java -jar chatpeer.jar -i 13333 -p 5000
                if (args[0].equals("-i") && args[2].equals("-p")) {
                    try {
                        int listeningPort = Integer.parseInt(args[3]);
                        int outboundPort = Integer.parseInt(args[1]);
                        System.out.println("Building peer with listening port: " + listeningPort + " and outgoing port " + outboundPort);
                        Peer peer = new Peer(listeningPort, outboundPort);
                        peer.handle();
                    } catch (Exception e) {
                        System.out.println("One of your port numbers is not an integer. ");
                        System.out.println("Example execution:      java -jar chatpeer.jar -i 13333 -p 5000");
                        System.out.println();
                    }
                }




            }
            
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR: There was an issue parsing your input. Please try again.");
            System.out.println("Example usage:   #java -jar chatpeer.jar 6000 7000");
            System.out.println();
        }
    }
}
