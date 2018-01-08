/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package integration;

import java.util.List;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import model.ChatRoom;
import model.RegisteredChatter;

/**
 *
 * @author Perttu Jääskeläinen
 */
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
@Stateless
public class ChatDAO {
    @PersistenceContext(unitName = "chatPU")
    private EntityManager manager;
    
    public boolean addUser(RegisteredChatter user) {
        if (manager.find(RegisteredChatter.class, user.getName()) != null) {
            return false;
        }
        manager.persist(user);
        return true;
    }
    public boolean addRoom(ChatRoom room) {
        if (manager.find(ChatRoom.class, room.getName()) != null) {
            return false;
        }
        manager.persist(room);
        return true;
    }
    public RegisteredChatter getUser(Object PrimaryKey) {
        return manager.find(RegisteredChatter.class, PrimaryKey);
    }
    public List<String> getRooms() {
        Query query = manager.createQuery("SELECT r.roomName FROM ChatRoom r");
        return query.getResultList(); 
    }
}
