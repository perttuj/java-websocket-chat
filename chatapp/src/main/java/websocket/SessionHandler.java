/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package websocket;

import controller.Controller;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
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

/**
 *
 * @author Perttu.Jaaskelainen
 */
@ApplicationScoped
public class SessionHandler {

    @Inject
    private Controller dao;

    // 1-1 mapping
    private final Map<Chatter, Session> userSessions = new HashMap<>();
    private final Map<Session, Chatter> sessionUsers = new HashMap<>();
    
    private final Map<String, Set<Session>> chatRooms = new HashMap<>();
    private final Set<Session> sessions = new HashSet<>();
    
    @PostConstruct
    void init() {
        chatRooms.put("home", new HashSet<Session>());
        chatRooms.put("english", new HashSet<Session>());
    }
    public void getRooms(Session session) {
        Set<String> rooms = chatRooms.keySet();
        Set<Session> activeClients = sessionUsers.keySet();
        JsonArrayBuilder jsonarray = Json.createArrayBuilder();
        for(String s : rooms) {
            jsonarray.add(s);
        }
        for (Session s : activeClients) {
            if (session == s) continue;
            jsonarray.add(sessionUsers.get(s).getName());
        }
        JsonArray arr = jsonarray.build();
        JsonObject obj = Json.createObjectBuilder()
                .add("action", "rooms")
                .add("roomsarray", arr)
                .build();
        sendToSession(session, obj);
    }
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
        sessionUsers.put(session, usr);
        userSessions.put(usr, session);
        sendToSession(session, obj);
        sendToAllSessions(newuser);
        sessions.add(session);
        chatRooms.get("home").add(session);
        for (Session s : sessions) {
            getRooms(s);
        }
    }

    public void removeSession(Session session) {
        JsonObject msg = Json.createObjectBuilder()
                .add("action", "response")
                .add("user", "SERVER")
                .add("message", "user " + sessionUsers.get(session).getName() + " left")
                .build();
        sessions.remove(session);
        removeUser(session);
        sendToAllSessions(msg);
    }

    public void loginUser(Session session, JsonObject message) {
        String username = message.getString("username");
        String password = message.getString("password");
        Chatter usr = dao.getUser(username);
        boolean verified = false, loggedIn = false;
        if (usr != null) {
            verified = usr.verify(password);
            loggedIn = userSessions.get(usr) != null;
        }
        JsonObject msg;
        if (!verified || loggedIn) {
            String response = loggedIn ? "already logged in" : "verification failed";
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
                    .add("message", "" + sessionUsers.get(session).getName() + " is now known as " + username)
                    .build();
            sendToSession(session, msg);
            sendToAllSessions(announcement);
            userSessions.remove(sessionUsers.get(session));
            sessionUsers.put(session, usr);
            userSessions.put(usr, session);
        }
    }

    public void registerUser(Session session, JsonObject message) {
        String username = message.getString("username");
        String password = message.getString("password");

        Chatter user = dao.getUser(username);
        JsonObject msg;

        if (user != null) {
            msg = Json.createObjectBuilder()
                    .add("action", "response")
                    .add("user", "SERVER")
                    .add("message", "user '" + username + "' already exists")
                    .build();
        } else {
            user = new Chatter(username, password);
            sessionUsers.put(session, user);
            msg = Json.createObjectBuilder()
                    .add("action", "response")
                    .add("user", "SERVER")
                    .add("message", "registered '" + username + "'")
                    .build();
            dao.addUser(user);
        }
        sendToSession(session, msg);
    }

    public void removeUser(Session user) {
        userSessions.remove(sessionUsers.get(user));
        sessionUsers.remove(user);
    }

    public void sendMessage(Session session, JsonObject message) {
        JsonObject msg = Json.createObjectBuilder()
                .add("action", "response")
                .add("user", sessionUsers.get(session).getName())
                .add("message", message.get("message"))
                .build();
        sendToAllSessions(msg);
    }

    private void sendToAllSessions(JsonObject message) {
        for (Session s : sessions) {
            sendToSession(s, message);
        }
    }

    public void sendToSession(Session session, JsonObject message) {
        try {
            session.getBasicRemote().sendText(message.toString());
        } catch (IOException e) {
            sessions.remove(session);
        }
    }
}
