package com.kvoli.base;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.ObjectMapper;

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
        {
        }

        public Message(String content) {
            this.content = content;
        }

    }


}
