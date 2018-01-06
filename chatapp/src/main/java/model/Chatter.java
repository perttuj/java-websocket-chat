/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 *
 * @author Perttu.Jaaskelainen
 */
@Entity
public class Chatter {
    @Id
    private String username;
    private String password;
    
    public Chatter() {
        
    }
    
    public Chatter(String name) {
        this.username = name;
        this.password = "";
    }
    
    public Chatter(String name, String pass) {
        this.username = name;
        this.password = pass;
    }
    public String getName() {
        return this.username;
    }
    public boolean verify(String pass) {
        return this.password.equals(pass);
    }
}
