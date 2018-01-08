/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controller;

import integration.ChatDAO;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import model.ChatRoom;
import model.RegisteredChatter;

/**
 *
 * @author Perttu Jääskeläinen
 */
@Stateless
public class Controller {
    @EJB
    ChatDAO dao;
    
    public boolean addUser(RegisteredChatter user) {
        return dao.addUser(user);
    }
    public RegisteredChatter getUser(Object PrimaryKey) {
        return dao.getUser(PrimaryKey);
    }
    public List<String> getRooms() {
        return dao.getRooms();
    }
    public boolean addRoom(ChatRoom room) {
        return dao.addRoom(room);
    }
}
