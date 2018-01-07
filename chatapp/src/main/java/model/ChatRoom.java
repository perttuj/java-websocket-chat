/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 *
 * @author Perttu.Jaaskelainen
 */
@Entity
public class ChatRoom implements Serializable {
    @Id
    private String roomName;
    private String owner;
    
    public ChatRoom() {
        
    }
    public ChatRoom(String name) {
        this.roomName = name;
        this.owner = null;
    }
    public ChatRoom(String name, String owner) {
        this.roomName = name;
        this.owner = owner;
    }
    public String getName() {
        return this.roomName;
    }
    public String getOwner() {
        return this.owner;
    }
}
