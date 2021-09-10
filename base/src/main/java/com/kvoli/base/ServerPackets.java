package com.kvoli.base;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;


/** TODO LIST: SERVER COMMANDS
 * roomcontents
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
        public String FromServerOrNot = "True";

        public Message() {}

        public Message(String identity, String content, String FromServerOrNot) {
            this.identity = identity;
            this.content = content;
            this.FromServerOrNot = FromServerOrNot;
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
        public String type = "roomlist";
        public String roomid;
        public int count;

        public RoomInfo() {}
        public RoomInfo(String roomid, int count) {
            this.roomid = roomid;
            this.count = count;
        }
    }

    // TODO
    @JsonTypeName("roomcontents")
    public static class RoomContents {
        public String roomid;
        public String owner;

    }
}
