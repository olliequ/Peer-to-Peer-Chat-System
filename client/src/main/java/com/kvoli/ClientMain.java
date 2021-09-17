package com.kvoli;



public class ClientMain {
    public static void main(String[] args) {
        int defaultPort = 4444;

        try {
            // Check args passed into server
            if (args.length == 0) {
                System.out.println("Error: Please provide a server address.");
            }
            else if (args.length == 1) {
                String address = args[0];
                System.out.println("Attempting to form connection with server " + address  + " default port " + defaultPort);
                Client client = new Client("localhost", defaultPort);
                client.handle(client);
            }
            else if (args.length == 2) {
                String address = args[0];
                int port = Integer.parseInt(args[1]);
                System.out.println("Attempting to form connection with server " + address  + " port " + port);
                Client client = new Client("localhost", port);
                client.handle(client);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR: There was an issue parsing your input. Please try again.");
        }



//        Client client = new Client("localhost", 4444);
//        client.handle(client);
    }
}
