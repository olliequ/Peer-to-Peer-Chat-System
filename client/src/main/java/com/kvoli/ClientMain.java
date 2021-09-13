package com.kvoli;



public class ClientMain {
    public static void main(String[] args) {
        Client client = new Client("localhost", 4444);
        client.handle(client);
    }
}
