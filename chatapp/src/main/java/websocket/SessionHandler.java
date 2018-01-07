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
import model.Chatter;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import model.ChatRoom;

/**
 *
 * @author Perttu.Jaaskelainen
 */
@ApplicationScoped
public class SessionHandler {

    @Inject
    private Controller controller;

    // 1-1 mapping
    private final Map<Chatter, Session> userSession = new HashMap<>();
    private final Map<Session, Chatter> sessionUser = new HashMap<>();
    
    private final Map<String, Set<Session>> chatRooms = new HashMap<>();
    private final Set<Session> sessions = new HashSet<>();
    
    @PostConstruct
    void init() {
        controller.addRoom(new ChatRoom("test"));
        List<String> rooms = controller.getRooms();
        for (String s : rooms) {
            chatRooms.put(s, new HashSet<Session>());
        } 
        /*
        chatRooms.put("home", new HashSet<Session>());
        chatRooms.put("english", new HashSet<Session>()); */
    }
    /**
     * Gets all the rooms and sends them to the specified session
     * @param session   session to send all available rooms to
     */
    public void getRooms(Session session) {
        Set<String> rooms = chatRooms.keySet();
        Set<Session> activeClients = sessionUser.keySet();
        JsonArrayBuilder jsonarray = Json.createArrayBuilder();
        for(String s : rooms) {
            jsonarray.add(s);
        }
        for (Session s : activeClients) {
            if (session == s) continue;
            jsonarray.add(sessionUser.get(s).getName());
        }
        JsonArray arr = jsonarray.build();
        JsonObject obj = Json.createObjectBuilder()
                .add("action", "rooms")
                .add("roomsarray", arr)
                .build();
        sendToSession(session, obj);
    }
    public void switchRoom(Session session, JsonObject info) {
        
        Chatter user = sessionUser.get(session);
        String room = user.getRoom();
        
        Set<Session> chattersInRoom = chatRooms.get(room);
        chattersInRoom.remove(session);
        
        String newRoom = info.getString("room");
        
        // three messages to be sent,
        // 1, to the user switching rooms
        // 2, to the users in the previous room
        // 3, to the users in the new room OR the notified user
        JsonObjectBuilder builder = Json.createObjectBuilder();
        JsonObjectBuilder builder2 = Json.createObjectBuilder();
        JsonObjectBuilder builder3 = Json.createObjectBuilder();
        
        builder.add("action", "response");
        builder.add("user", "SERVER");
        
        builder2.add("action", "response");
        builder2.add("user", "SERVER");
        
        builder3.add("action", "response");
        builder3.add("user", "SERVER");
        
        
        if (chatRooms.containsKey(newRoom)) {
            user.setRoom(newRoom);
            builder.add("message", "joined room '" + newRoom + "'");
            builder2.add("message", "user " + user.getName() + " left the room");
            builder3.add("message", "user " + user.getName() + " joined the room");
            
            sendToSession(session, builder.build());
            sendToRoom(room, builder2.build());
            sendToRoom(newRoom, builder3.build());
            chatRooms.get(newRoom).add(session);
            sessionUser.put(session, user);
        } else {
            Chatter thisUser = controller.getUser(user.getName());
            Chatter otherUser = controller.getUser(newRoom);
            if (otherUser == null || thisUser == null) {
                builder.add("message", "both users need to be registered to open new chat rooms");
                sendToSession(session, builder.build());
                return;
            }
            Set<Session> set = new HashSet<>();
            set.add(session);
            int roomNumber = (int) (Math.random() * 1000);
            String nextRoom = "Room" + roomNumber;
            ChatRoom nRoom = new ChatRoom(nextRoom, thisUser.getName());
            controller.addRoom(nRoom);
            chatRooms.put(nextRoom, set);
            builder.add("message", "opened new room '" + nextRoom + "', user " + newRoom + " has been notified");
            builder2.add("message", "user " + user.getName() + " left the room");
            builder3.add("message", "user " + user.getName() + " has started a new room '" + nextRoom + "' and invited you");
            
            user.setRoom(newRoom);
            sessionUser.put(session, user);
            userSession.put(user, session);
            sendToSession(session, builder.build());
            sendToRoom(room, builder2.build());
            sendToSession(userSession.get(otherUser), builder3.build());
        }
    }
    /**
     * Called by the server when a client establishes a connection to the server
     * @param session   the session that has connected
     */
    public void addSession(Session session) {
        Chatter usr = new Chatter(String.valueOf("Guest" + ((int) (Math.random() * 1000))));
        JsonObject obj = Json.createObjectBuilder()
                .add("action", "response")
                .add("user", "SERVER")
                .add("message", "logged in as: " + usr.getName())
                .build();
        JsonObject newuser = Json.createObjectBuilder()
                .add("action", "response")
                .add("user", "SERVER")
                .add("message", "user joined: " + usr.getName())
                .build();
        usr.setRoom("home");
        sessionUser.put(session, usr);
        userSession.put(usr, session);
        sendToSession(session, obj);
        sendToAllSessions(newuser);
        sessions.add(session);
        chatRooms.get("home").add(session);
        for (Session s : sessions) {
            getRooms(s);
        }
    }
    /**
     * Remove a session from the application, called when 
     * the user shuts down the application
     * @param session   session that quit the application
     */
    public void removeSession(Session session) {
        JsonObject msg = Json.createObjectBuilder()
                .add("action", "response")
                .add("user", "SERVER")
                .add("message", "user " + sessionUser.get(session).getName() + " left")
                .build();
        sessions.remove(session);
        removeUser(session);
        for (String room : chatRooms.keySet()) {
            Set<Session> set = chatRooms.get(room);
            if (set.contains(session)) {
                set.remove(session);
                break;
            }
        }
        sendToRoom(sessionUser.get(session).getRoom(), msg);
    }
    /**
     * Called by the WebSocketServer when a user wants to login
     * @param session   the session that wants to login
     * @param message   JsonObject containing information, like credentials.
     */
    public void loginUser(Session session, JsonObject message) {
        String username = message.getString("username");
        String password = message.getString("password");
        Chatter usr = controller.getUser(username);
        boolean verified = false, loggedIn = false;
        if (usr != null) {
            verified = usr.verify(password);
            loggedIn = userSession.get(usr) != null;
        }
        JsonObject msg;
        if (!verified || loggedIn) {
            String response = loggedIn ? "already logged in" : "incorrect username/password";
            msg = Json.createObjectBuilder()
                    .add("action", "response")
                    .add("user", "SERVER")
                    .add("message", "login failed - " + response)
                    .build();
            sendToSession(session, msg);
        } else {
            msg = Json.createObjectBuilder()
                    .add("action", "response")
                    .add("user", "SERVER")
                    .add("message", "logged in as " + username)
                    .build();
            JsonObject announcement = Json.createObjectBuilder()
                    .add("action", "response")
                    .add("user", "SERVER")
                    .add("message", "" + sessionUser.get(session).getName() + " is now known as " + username)
                    .build();
            sendToSession(session, msg);
            sendToAllSessions(announcement);
            userSession.remove(sessionUser.get(session));
            sessionUser.put(session, usr);
            userSession.put(usr, session);
        }
    }
    /**
     * Called by the WebSocketServer when a user wants to register a new user
     * @param session   the session that wants to register a new user
     * @param message   information received from the user, including credentials
     */
    public void registerUser(Session session, JsonObject message) {
        String username = message.getString("username");
        String password = message.getString("password");

        Chatter user = controller.getUser(username);
        JsonObject msg;

        if (user != null) {
            msg = Json.createObjectBuilder()
                    .add("action", "response")
                    .add("user", "SERVER")
                    .add("message", "user '" + username + "' already exists")
                    .build();
        } else {
            user = new Chatter(username, password);
            sessionUser.put(session, user);
            msg = Json.createObjectBuilder()
                    .add("action", "response")
                    .add("user", "SERVER")
                    .add("message", "registered '" + username + "'")
                    .build();
            controller.addUser(user);
        }
        sendToSession(session, msg);
    }
    /**
     * Called when a users connection is closed (onClose())
     * @param user  the user session that is terminated
     */
    public void removeUser(Session user) {
        Chatter chatter = sessionUser.get(user);
        chatRooms.get(chatter.getRoom()).remove(user);
        userSession.remove(chatter);
        sessionUser.remove(user);
    }

    /**
     * Called by WebSocketServer when a user wants to send a message
     * @param session   Session that wants to send the message
     * @param message   Message to be sent to all users in the same chatroom
     */
    public void sendMessage(Session session, JsonObject message) {
        JsonObject msg = Json.createObjectBuilder()
                .add("action", "response")
                .add("user", sessionUser.get(session).getName())
                .add("message", message.get("message"))
                .build();
        Chatter user = sessionUser.get(session);
        String room = user.getRoom();
        if (room != null) {
            sendToRoom(room, msg);
        } else {
            msg = Json.createObjectBuilder()
                    .add("action", "response")
                    .add("user", "SERVER")
                    .add("message", "message failed, not registered to a chat room")
                    .build();
            sendToSession(session, msg);
        }
    }
    /**
     * Send a message to all users in a chat room
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
     * Send a message to all active user sessions
     * @param message   the message to send to all users
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
            removeUser(session);
        }
    }
}
