/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controller;

import integration.ChatDAO;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import model.Chatter;

/**
 *
 * @author Perttu Jääskeläinen
 */
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
@Stateless
public class Controller {
    @EJB
    ChatDAO dao;
    
    public boolean addUser(Chatter user) {
        return dao.addUser(user);
    }
    public Chatter getUser(Object PrimaryKey) {
        return dao.getUser(PrimaryKey);
    }
    
}
