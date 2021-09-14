package com.kvoli;



public class Main {
    public static void main(String[] args) {

        try {
            // Check args passed into server
            if (args.length == 0) {
                // Call server with default port
                System.out.println("Attempting to build server on default port.");
                Server server = new Server();
                server.handle();
            }
            else if (args.length == 2) {
                int port = Integer.parseInt(args[1]);
                System.out.println("Attempting to build server on port " + port);
                Server server = new Server(port);
                server.handle();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR: There was an issue parsing your input. Please try again.");
        }


        //int port = Integer.parseInt(args[1]);
       // System.out.println("Attempting to build server on port " + port);


    }
}
