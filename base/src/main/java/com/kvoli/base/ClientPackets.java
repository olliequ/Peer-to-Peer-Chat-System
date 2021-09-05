package com.kvoli.base;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;

/** TODO LIST: CLIENT COMMANDS
 * join
 * list
 * who
 * createroom
 * delete
 * quit
 */



public class ClientPackets {
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
        public String content;

        public Message()
        {}

        public Message(String content) {
            this.content = content;
        }
    }

    @JsonTypeName("identitychange")
    public static class IdentityChange{
        // Jackson wouldn't put the above JsonTypeName into the actual JSON so I had to do it manually.
        public String type = "identitychange";
        public String identity;

        public IdentityChange()
        {}

        public IdentityChange(String identity) {
            this.identity = identity;
        }
    }

    // TODO
    @JsonTypeName("list")
    public static class List {
        // TODO
    }

    // TODO
    @JsonTypeName("join")
    public static class Join {
        public String type = "join";
        public String roomid;

        public Join() {}

        public Join(String roomid) {
            this.roomid = roomid;
        }
    }

    // TODO
    @JsonTypeName("who")
    public static class Who {
        public String type = "who";
        public String roomid;

        public Who() {}

        public Who(String roomid) {
            this.roomid = roomid;
        }

    }


}
