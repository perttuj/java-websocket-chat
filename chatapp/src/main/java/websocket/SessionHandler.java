/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package websocket;

import controller.Controller;
import java.io.IOException;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.websocket.Session;
import model.Chatter;

/**
 *
 * @author Perttu.Jaaskelainen
 */
@ApplicationScoped
public class SessionHandler {

    @Inject
    private Controller dao;

    private final Set<Session> sessions = new HashSet<>();
    private final Map<Session, Chatter> users = new HashMap<>();

    public void addSession(Session session) {
        Chatter usr = new Chatter(String.valueOf("Guest" + ((int) (Math.random() * 1000))));
        users.put(session, usr);
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
        sendToSession(session, obj);
        sendToAllSessions(newuser);
        sessions.add(session);
    }

    public void removeSession(Session session) {
        JsonObject msg = Json.createObjectBuilder()
                .add("action", "response")
                .add("user", "SERVER")
                .add("message", "user left: " + users.get(session).getName())
                .build();
        sessions.remove(session);
        removeUser(session);
        sendToAllSessions(msg);
    }

    public void loginUser(Session session, JsonObject message) {
        String username = message.getString("username");
        String password = message.getString("password");
        Chatter usr = dao.getUser(username);
        boolean verified = false;
        if (usr != null) {
            verified = usr.verify(password);
        }
        JsonObject msg;
        if (!verified) {
            msg = Json.createObjectBuilder()
                    .add("action", "response")
                    .add("user", "SERVER")
                    .add("message", "login failed")
                    .build();
        } else {
            msg = Json.createObjectBuilder()
                    .add("action", "response")
                    .add("user", "SERVER")
                    .add("message", "logged in as " + username)
                    .build();
        }
        sendToSession(session, msg);
        users.put(session, usr);
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
            users.put(session, user);
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
        users.remove(user);
    }

    public void sendMessage(Session session, JsonObject message) {
        JsonObject msg = Json.createObjectBuilder()
                .add("action", "response")
                .add("user", users.get(session).getName())
                .add("message", message.get("message"))
                .build();
        sendToAllSessions(msg);
    }

    private void sendToAllSessions(JsonObject message) {
        for (Session s : sessions) {
            sendToSession(s, message);
        }
    }

    private String getTime() {
        LocalTime time = LocalTime.now();
        String s
                = time.getHour() + ":"
                + time.getMinute() + ":"
                + time.getSecond() + " ";
        return s;
    }

    public void sendToSession(Session session, JsonObject message) {
        try {
            session.getBasicRemote().sendText(message.toString());
        } catch (IOException e) {
            sessions.remove(session);
        }
    }
}
