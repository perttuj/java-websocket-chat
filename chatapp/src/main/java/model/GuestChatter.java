/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

/**
 *
 * @author Perttu Jääskeläinen
 */
public class GuestChatter implements Chatter {
    
    private String username;
    private String room;
    private long ID;
    
    public GuestChatter(String name, String room) {
        this.username = name;
        this.room = room;
        this.ID = (int) (Math.random() * 1000);
    }
    public String getRoom() {
        return this.room;
    }
    public String getName() {
        return this.username;
    }
}
