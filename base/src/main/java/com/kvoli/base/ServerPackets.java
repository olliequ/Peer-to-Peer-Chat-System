package com.kvoli.base;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;


/**
 * This class contains the list of outbound Server Protocols used by the server in Server.java
 */

public class ServerPackets {

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            property = "type"
    )
    @JsonSubTypes({
            @JsonSubTypes.Type(value = Packets.IdentityChange.class, name = "identitychange"),
            @JsonSubTypes.Type(value = Packets.Join.class, name = "join")
    })

    @JsonTypeName("message")
    public static class Message {
        public String identity;
        public String content;

        public Message() {}

        public Message(String identity, String content) {
            this.identity = identity;
            this.content = content;
        }
    }

    @JsonTypeName("newidentity")
    public static class NewIdentity{
        public String type = "newidentity";
        public String former;
        public String identity;

        public NewIdentity() {}

        public NewIdentity(String former, String identity) {
            this.former = former;
            this.identity = identity;
        }
    }

    @JsonTypeName("roomchange")
    public static class RoomChange {
        public String type = "roomchange";
        public String identity;
        public String former;
        public String roomid;

        public RoomChange() {
        }

        public RoomChange(String identity, String former, String roomid) {
            this.identity = identity;
            this.former = former;
            this.roomid = roomid;
        }
    }

    @JsonTypeName("roomlist")
    public static class RoomList {
        public String type = "roomlist";
        public List<String> rooms;

        public RoomList(List<String> rooms)  {
            this.rooms = rooms;
        }

    }

    @JsonTypeName("roominfo")
    public static class RoomInfo {
        // public String type = "roomlist";
        public String roomid;
        public int count;

        public RoomInfo() {}
        public RoomInfo(String roomid, int count) {
            this.roomid = roomid;
            this.count = count;
        }
    }

    @JsonTypeName("roomcontents")
    public static class RoomContents {
        public String type = "roomcontents";
        public String roomid;
        public List<String> identities;
        public String owner;

        public RoomContents(String roomid, List<String> identities, String owner) {
            this.roomid = roomid;
            this.identities = identities;
            this.owner = owner;
        }
    }


    @JsonTypeName("neighbors")
    public static class Neighbors {
        public String type = "neighbors";
        public List<String> neighbors;

        public Neighbors(List<String> neighbors) {
            this.neighbors = neighbors;
        }
    }


    @JsonTypeName("migrationroom")
    public static class MigrationRoom {
        public String type = "migrationroom";
        public String sender;
        public String roomName;
        public int totalRooms;

        public MigrationRoom(String sender, String roomName, int totalRooms) {
            this.sender = sender;
            this.roomName = roomName;
            this.totalRooms = totalRooms;
        }
    }

    @JsonTypeName("migrationidentity")
    public static class MigrationIdentity {
        public String type = "migrationidentity";
        public String sender;
        public String identity;
        public String roomName;
        public int totalIdentities;

        public MigrationIdentity(String sender, String identity, String roomName, int totalIdentities) {
            this.sender = sender;
            this.identity = identity;
            this.roomName = roomName;
            this.totalIdentities = totalIdentities;
        }
    }
}
