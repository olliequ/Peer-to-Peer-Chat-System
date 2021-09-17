# COMP90015 Project 1 Repository 

Name: Sahil Tandon (SAHILT), Student ID: 1186079

Name: Oliver Quarm (oquarm), Student ID: 834844

# At a glance
The program consists of three main folders: base, client and server. Below is a quick rundown.

## Base    
###Consists of:
- ClientPackets (list of client protocols)
- ServerPackets (list of server protocols)
- JSONReader (used to read JSON objects via Jackson)
- JSONWriter (used to write JSON objects)

## Client
###Consists of:
- ClientMain (takes in args and is used to spawn a new instance of Client)
- Client
- GetMessageThread (used by client to receive messages from server)
- SendMessageThread (used by client to parse user messages. Sends messages to server.)

## Server
###Consists of:
- Main (takes in args and is used to spawn a new instance of Server)
- Server
- Room (used by Server to create new Room objects)


## Usage
### Server
$> java -jar chatserver.jar [-p port]

### Client
$> java -jar chatclient.jar hostname [-p port]