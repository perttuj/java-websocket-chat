/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package websocket;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import javax.json.JsonObject;
import javax.websocket.Session;
import model.User;

/**
 *
 * @author Perttu.Jaaskelainen
 */
@ApplicationScoped
public class SessionHandler {
    private final Set<Session> sessions = new HashSet<>();
    private final Set<User> users = new HashSet<>();
    
    public void addSession(Session session) {
        sessions.add(session);
    }
    public void removeSession(Session session) {
        sessions.remove(session);
    }
    public void addUser(User user) {
        users.add(user);
    }
    public void removeUser(User user) {
        users.remove(user);
    }
    public void sendToAllSessions(JsonObject message) {
        for (Session session : sessions) {
            sendToSession(session, message);
        }
    }
    private void sendToSession(Session session, JsonObject message) {
        try {
            session.getBasicRemote().sendText(message.toString());
        } catch (IOException e) {
            sessions.remove(session);
        }
    }
}
