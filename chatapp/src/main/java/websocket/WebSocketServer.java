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

/**
 *  Class for handling initial contact with a user
 * @author Perttu Jääskeläinen
 */
@ApplicationScoped
@ServerEndpoint("/actions")
public class WebSocketServer {
    @Inject
    private SessionHandler sessionHandler;
    
    /**
     * Called when a user initiates a websocket connection
     * @param session   session that opened a connection
     */
    @OnOpen
    public void onOpen(Session session) {
        sessionHandler.addSession(session);
    }
    /**
     * Called when a users connection is closed
     * @param session   session that is closed
     */
    @OnClose
    public void onClose(Session session) {
        sessionHandler.removeSession(session);
    }
    /**
     * Called when a error occurs in the connection
     * @param error error that happened
     * @throws Exception    exception that is thrown
     */
    @OnError
    public void onError(Throwable error) throws Exception {
        throw new Exception(error);
    }
    /**
     * Called when a user sends text through the socket
     * @param message   the message that is received
     * @param session   session that sent the message
     */
    @OnMessage
    public void handleMessage(String message, Session session) {
        try (JsonReader reader = Json.createReader(new StringReader(message))) {
            JsonObject jsonObject = reader.readObject();
            String action = jsonObject.getString("action");
            switch (action) {
                case "login":
                    sessionHandler.loginUser(session, jsonObject);
                    break;
                case "send":
                    sessionHandler.sendMessage(session, jsonObject);
                    break;
                case "remove":
                    sessionHandler.removeSession(session);
                    break;
                case "register":
                    sessionHandler.registerUser(session, jsonObject);
                    break;
                case "reload":
                    sessionHandler.reload(session);
                    break;
                case "switchRoom":
                    sessionHandler.switchRoom(session, jsonObject);
                    break;
                default:
                    JsonObject msg = Json.createObjectBuilder()
                            .add("action", "response")
                            .add("user", "SERVER")
                            .add("message", "ACTION NOT SUPPORTED")
                            .build();
                    sessionHandler.sendToSession(session, msg);
                    break;
            }
        }
    }
}
