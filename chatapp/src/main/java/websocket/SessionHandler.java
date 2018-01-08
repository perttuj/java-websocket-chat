/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package websocket;

import controller.Controller;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.websocket.Session;
import model.RegisteredChatter;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import model.ChatRoom;
import model.Chatter;
import model.GuestChatter;

/**
 *
 * @author Perttu.Jaaskelainen
 */
@ApplicationScoped
public class SessionHandler {

    @Inject
    private Controller controller;
    
    private final String DEFAULT_CHATROOM = "home";

    // 1-1 mapping, avoiding double storage of RegisteredChatter- objects, instead
    // mapping an ID to each thisUser
    private final Map<Session, Long> sessionUserID    = new HashMap<>();
    private final Map<Session, GuestChatter> guests = new HashMap<>();
    private final Map<Long, Session> userIDSession = new HashMap<>();
    private final Map<Long, RegisteredChatter> registeredUsers = new HashMap<>();
    
    // Hashmap of all active chatrooms, containing a set of active sessions
    private final Map<String, Set<Session>> chatRooms = new HashMap<>();
    private final Set<Session> sessions = new HashSet<>();
    
    @PostConstruct
    void init() {
        controller.addRoom(new ChatRoom(DEFAULT_CHATROOM));
        controller.addRoom(new ChatRoom("TEST"));
        List<String> rooms = controller.getRooms();
        for (String s : rooms) {
            chatRooms.put(s, new HashSet<Session>());
        } 
        /*
        chatRooms.put("home", new HashSet<Session>());
        chatRooms.put("english", new HashSet<Session>()); */
    }
    private JsonObjectBuilder newServerResponseBuilder() {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("action", "response");
        builder.add("user", "SERVER");
        return builder;
    }
    private JsonObjectBuilder newServerConnectedBuilder() {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("action", "connected");
        return builder;
    }
    public void reload(Session session) {
        getRooms(session);
        getUsers(session);
    }
    private void getUsers(Session session) {
        Set<Long> set = registeredUsers.keySet();
        JsonArrayBuilder jsonarray = Json.createArrayBuilder();
        for (long l : set) {
            jsonarray.add(registeredUsers.get(l).getName());
        }
        JsonObject obj = Json.createObjectBuilder()
                .add("action", "users")
                .add("users", jsonarray.build())
                .build();
        sendToSession(session, obj);
    }
    /**
     * Gets all the rooms and sends them to the specified session
     * @param session   session to send all available rooms to
     */
    private void getRooms(Session session) {
        Set<String> rooms = chatRooms.keySet();
        JsonArrayBuilder jsonarray = Json.createArrayBuilder();
        for(String s : rooms) {
            jsonarray.add(s);
        }
        JsonArray arr = jsonarray.build();
        JsonObject obj = Json.createObjectBuilder()
                .add("action", "rooms")
                .add("roomsarray", arr)
                .build();
        sendToSession(session, obj);
    }
    public void switchRoom(Session session, JsonObject info) {
        JsonObjectBuilder builder = newServerResponseBuilder();
        Long LongID = sessionUserID.get(session);
        if (LongID == null) {
            builder.add("message", "unregistered users cannot switch rooms");
            sendToSession(session, builder.build());
            return;
        }
        long ID = LongID.longValue();
        RegisteredChatter thisUser = registeredUsers.get(ID);
        String room = thisUser.getRoom();
        String newRoom = info.getString("room");
        
        if (room.equals(newRoom)) {
            builder.add("message", "already chatting in room " + newRoom);
            sendToSession(session, builder.build());
            return;
            
        }
        Set<Session> chattersInRoom = chatRooms.get(room);
        chattersInRoom.remove(session);
        
        // three messages to be sent,
        // 1, to the thisUser switching rooms
        // 2, to the registeredUsers in the previous room
        // 3, to the registeredUsers in the new room OR 
        //   the notified thisUser when creating a new room
        
        JsonObjectBuilder builder2 = newServerResponseBuilder();
        JsonObjectBuilder builder3 = newServerResponseBuilder();
        
        // if room exists, send messages to affected rooms
        if (chatRooms.containsKey(newRoom)) {
            thisUser.setRoom(newRoom);
            
            builder.add("message", "joined room '" + newRoom + "'");
            builder2.add("message", "user '" + thisUser.getName() + "' switched rooms");
            builder3.add("message", "user '" + thisUser.getName() + "' joined the room");
            
            sendToSession(session, builder.build());
            sendToRoom(room, builder2.build());
            sendToRoom(newRoom, builder3.build());
            chatRooms.get(newRoom).add(session);
        } else {
            // if room is non-existant, the request is for opening a new room
            // with a randomized roomname and inviting a thisUser - where
            // the thisUser to invite is stored in the 'newRoom' variable
            RegisteredChatter   otherUser   = controller.getUser(newRoom);
            if (otherUser == null || thisUser == null) {
                builder.add("message", "both users need to be registered to open new chat rooms");
                sendToSession(session, builder.build());
                return;
            }
            Set<Session> set = new HashSet<>();
            set.add(session);
            String nextRoom;
            do {
                int roomNumber = (int) (Math.random() * 1000);
                nextRoom = "Room" + roomNumber;
            } while (chatRooms.get(nextRoom) != null);
            
            ChatRoom nRoom = new ChatRoom(nextRoom, thisUser.getName());
            controller.addRoom(nRoom);
            chatRooms.put(nextRoom, set);
            
            builder.add("message", "opened new room '" + nextRoom + "', user " + newRoom + " has been notified");
            builder2.add("message", "user " + thisUser.getName() + " switched rooms");
            builder3.add("message", "user " + thisUser.getName() + " has started a new room '" + nextRoom + "' and invited you");
            
            thisUser.setRoom(newRoom);
            sendToSession(session, builder.build());
            sendToRoom(room, builder2.build());
            long otherUserID = otherUser.getID();
            sendToSession(userIDSession.get(otherUserID), builder3.build());
        }
    }
    /**
     * Called by the server when a client establishes a connection to the server
     * @param session   the session that has connected
     */
    public void addSession(Session session) {
        GuestChatter guest = new GuestChatter(String.valueOf("Guest" + ((int) (Math.random() * 1000))), DEFAULT_CHATROOM);
        JsonObject obj = newServerConnectedBuilder()
                .add("message", "Chatting in " + DEFAULT_CHATROOM + ", know as: " + guest.getName())
                .add("room", DEFAULT_CHATROOM)
                .build();
        JsonObject newuser = newServerResponseBuilder()
                .add("message", "'" + guest.getName() + "' joined")
                .build();
        sendToSession(session, obj);
        sendToAllSessions(newuser);
        sessions.add(session);
        guests.put(session, guest);
        chatRooms.get(DEFAULT_CHATROOM).add(session);
    }
    /**
     * Remove a session from the application, called when 
     * the thisUser shuts down the application
     * @param session   session that quit the application
     */
    public void removeSession(Session session) {
        JsonObjectBuilder msg = newServerResponseBuilder();
        sessions.remove(session);
        Long ID = sessionUserID.get(session);
        Chatter user;
        if (ID == null) {
            user = guests.get(session);
            chatRooms.get(DEFAULT_CHATROOM).remove(session);
            msg.add("message", "'" + user.getName()+ "' left");
            guests.remove(session);
            sendToRoom(DEFAULT_CHATROOM, msg.build());
            return;
        }
        user = registeredUsers.get(ID);
        String room = user.getRoom();
        chatRooms.get(room).remove(session);
        msg.add("message", "'" + user.getName()+ "' left");
        sendToRoom(room, msg.build());
    }
    
    /**
     * Called by the WebSocketServer when a thisUser wants to login
     * @param session   the session that wants to login
     * @param message   JsonObject containing information, like credentials.
     */
    public void loginUser(Session session, JsonObject message) {
        JsonObjectBuilder msg = newServerResponseBuilder();
        if (sessionUserID.get(session) != null) {
            msg.add("message", "already logged in, logout first");
            sendToSession(session, msg.build());
            return;
        }
        String username = message.getString("username");
        String password = message.getString("password");
        GuestChatter guest  = guests.get(session);
        RegisteredChatter usr = controller.getUser(username);
        
        boolean verified = false, loggedIn = false;
        if (usr != null) {
            verified = usr.verify(password);
            loggedIn = userIDSession.get(usr.getID()) != null;
        } else {
            msg.add("message", "username '" + username + "' does not exist");
            sendToSession(session, msg.build());
            return;
        }
        if (!verified || loggedIn) {
            String response = loggedIn ? "already logged in" : "incorrect username/password";
            msg.add("message", "login failed - " + response);
            sendToSession(session, msg.build());
        } else {
            msg.add("message", "logged in as " + username);
            JsonObject announcement = newServerResponseBuilder()
                    .add("message", "'" + guest.getName() + "' is now known as '" + username + "'")
                    .build();
            sendToSession(session, msg.build());
            guests.remove(session);
            sendToRoom(guest.getRoom(), announcement);
            long ID = usr.getID();
            registeredUsers.put(ID, usr);
            sessionUserID.put(session, ID);
            userIDSession.put(ID, session);
        }
    }
    /**
     * Called by the WebSocketServer when a thisUser wants to register a new thisUser
     * @param session   the session that wants to register a new thisUser
     * @param message   information received from the thisUser, including credentials
     */
    public void registerUser(Session session, JsonObject message) {
        String username = message.getString("username");
        String password = message.getString("password");

        RegisteredChatter user = controller.getUser(username);
        JsonObject msg;

        if (user != null) {
            msg = newServerResponseBuilder()
                .add("message", "user '" + username + "' already exists")
                .build();
        } else {
            user = new RegisteredChatter(username, password);
            user.setRoom(DEFAULT_CHATROOM);
            msg = newServerResponseBuilder()
                .add("message", "registered '" + username + "'")
                .build();
            controller.addUser(user);
        }
        sendToSession(session, msg);
    }

    /**
     * Called by WebSocketServer when a thisUser wants to send a message
     * @param session   Session that wants to send the message
     * @param message   Message to be sent to all registeredUsers in the same chatroom
     */
    public void sendMessage(Session session, JsonObject message) {
        Long ID = sessionUserID.get(session);
        Chatter user;
        if (ID == null) {
            user = guests.get(session);
        } else {
            user = registeredUsers.get(ID);
        }
        JsonObject msg = Json.createObjectBuilder()
                .add("action", "response")
                .add("user", user.getName())
                .add("message", message.get("message"))
                .build();
        String room = user.getRoom();
        if (room != null) {
            sendToRoom(room, msg);
        } else {
            msg = newServerResponseBuilder()
                .add("message", "message failed, not registered to a chat room")
                .build();
            sendToSession(session, msg);
        }
    }
    /**
     * Send a message to all registeredUsers in a chat room
     * @param room  the room to send message to
     * @param msg   message to send to the room
     */
    private void sendToRoom(String room, JsonObject msg) {
        Set<Session> set = chatRooms.get(room);
        for (Session s : set) {
            sendToSession(s, msg);
        }
    }
    /**
     * Send a message to all active thisUser sessions
     * @param message   the message to send to all registeredUsers
     */
    private void sendToAllSessions(JsonObject message) {
        for (Session s : sessions) {
            sendToSession(s, message);
        }
    }

    /**
     * Send a message to a specific session, done by accessing its basic remote
     * @param session   Session to send message to
     * @param message   Message to be sent to session
     */
    public void sendToSession(Session session, JsonObject message) {
        try {
            session.getBasicRemote().sendText(message.toString());
        } catch (IOException e) {
            removeSession(session);
        }
    }
}
