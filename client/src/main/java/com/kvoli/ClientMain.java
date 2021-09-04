package com.kvoli;

import com.kvoli.base.Base;

public class ClientMain {
    public static void main(String[] args) {
        Client client = new Client("localhost", 6379);
        client.handle(client);
    }
}
