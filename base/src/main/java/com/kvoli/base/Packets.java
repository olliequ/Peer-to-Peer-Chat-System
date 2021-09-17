package com.kvoli.base;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Class not used. See ClientPackets and ServerPackets
 */


// Better but not exactly idiomatic - move stuff out of the static classes and it's good.
public class Packets {

  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME,
      property = "type"
  )
  @JsonSubTypes({
      @JsonSubTypes.Type(value = IdentityChange.class, name = "identitychange"),
      @JsonSubTypes.Type(value = Join.class, name = "join")
  })


  public static class ToServer {
  }

  @JsonTypeName("identitychange")
  public static class IdentityChange extends ToServer {
    public String identity;
  }

  @JsonTypeName("join")
  public static class Join extends ToServer {
    public String roomid;
  }

  @JsonTypeName("message")
  public static class Message extends ToServer {
    public String content;

    public Message(String content) {
      this.content = content;
    }
  }

}
