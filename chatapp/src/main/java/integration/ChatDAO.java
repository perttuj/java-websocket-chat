/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package integration;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import model.Chatter;

/**
 *
 * @author Perttu Jääskeläinen
 */
@TransactionAttribute(TransactionAttributeType.MANDATORY)
@Stateless
public class ChatDAO {
    @PersistenceContext(unitName = "chatPU")
    private EntityManager manager;
    
    public boolean addUser(Chatter user) {
        if (manager.find(Chatter.class, user.getName()) != null) {
            return false;
        }
        manager.persist(user);
        return true;
    }
    public Chatter getUser(Object PrimaryKey) {
        return manager.find(Chatter.class, PrimaryKey);
    }
}
