/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.id1212.chatapp.server;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 *
 * @author Perttu Jääskeläinen
 */
@ServerEndpoint("/push")
public class ChatEndpoint {
    private static final Set<Session> USERS = ConcurrentHashMap.newKeySet();
    @OnOpen
    public void onOpen(Session session) {
        
    }
    @OnClose
    public void onClose(Session session) {
        
    }
    public static void sendAll(String message) {
        synchronized(USERS) {
            for (Session s : USERS) {
                if (s.isOpen()) {
                    s.getAsyncRemote().sendText(message);
                }
            }
        }
    }
}
