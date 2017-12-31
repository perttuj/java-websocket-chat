/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package websocket;

import java.io.StringReader;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import model.User;

/**
 *
 * @author Perttu Jääskeläinen
 */
@ApplicationScoped
@ServerEndpoint("/actions")
public class WebSocketServer {
    @Inject
    private SessionHandler sessionHandler;
    @OnOpen
    public void onOpen(Session session) {
        sessionHandler.addSession(session);
    }
    @OnClose
    public void onClose(Session session) {
        sessionHandler.removeSession(session);
    }
    @OnError
    public void onError(Throwable error) {
        
    }
    @OnMessage
    public void handleMessage(String message, Session session) {
        
        try (JsonReader reader = Json.createReader(new StringReader(message))) {
            JsonObject jsonMessage = reader.readObject();
            String action = jsonMessage.getString("action");
            if (action.equals("add")) {
                // add user
                User user = new User(); 
                user.setName(String.valueOf(Math.random() * 10));
                sessionHandler.addUser(user);
            }
            if (action.equals("send")) {
                // send msg
                sessionHandler.sendToAllSessions(jsonMessage);
            }
            if (action.equals("remove")) {
                // remove user
            }
            
        }
    }
}
