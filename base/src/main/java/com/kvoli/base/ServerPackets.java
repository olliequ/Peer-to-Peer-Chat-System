package com.kvoli.base;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.ObjectMapper;


/** TODO LIST: SERVER COMMANDS
 * roomchange
 * roomcontents
 * roomlist
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
    public static class Message{
        public String identity;
        public String content;

        public Message()
        {
        }

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

        public NewIdentity()
        {
        }

        public NewIdentity(String former, String identity) {
            this.former = former;
            this.identity = identity;
        }
    }

    // TODO
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


    // TODO
    @JsonTypeName("roomcontents")
    public static class RoomContents {
        public String roomID;
        public String owner;
        // IDENTITIES IS A JSON ARRAY BUT I DON'T KNOW HOW TO REPRESENT THIS IN JAVA....
    }



}
