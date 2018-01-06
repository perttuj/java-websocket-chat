/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
window.onload = init;
var socket = new WebSocket("ws://localhost:8080/chatapp/actions");
socket.onmessage = onMessage;

function setStatus(status) {
    document.getElementById("test").value = status;
}

function sendMessage() {
    setStatus("sending");
    var msg = document.getElementById("messageToSend").value;
    var messageToSend = {
        action: "send",
        message: msg
    };
    document.getElementById("messageToSend").value = null;
    socket.send(JSON.stringify(messageToSend));
}
function joinChat() {
    
}
function register() {
    setStatus("registering");
    var user = document.getElementById("reguser").value;
    var pass = document.getElementById("regpass").value;
    var command = {
        action: "register",
        username: user,
        password: pass
    };
    document.getElementById("reguser").value = null;
    document.getElementById("regpass").value = null;
    socket.send(JSON.stringify(command));
}
function login() {
    setStatus("logging in");
    var usr = document.getElementById("loginuser").value;
    var pwd = document.getElementById("loginpass").value;
    var credentials = {
        action: "login",
        username: usr,
        password: pwd
    };
    document.getElementById("loginuser").value = null;
    document.getElementById("loginpass").value = null;
    socket.send(JSON.stringify(credentials));
}
function displayLists(lists) {
    var rooms = lists.roomsarray;
    var list = "";
    setStatus("displaying list");
    for (var i = 0; i < rooms.length; i++) {
        list += "<option>" + rooms[i] + "</option>";
    }
    document.getElementById("list").innerHTML = list;
}
function getRooms() {
    var request = {
        action: "rooms"
    };
    socket.send(JSON.stringify(request));
}
function onMessage(event) {
    setStatus("onMessage");
    var action = JSON.parse(event.data);
    if (action.action === "response") {
        appendMessage(action);
    } else if (action.action === "rooms") {
        displayLists(action);
    }
}

function getTime() {
    var d = new Date();
    return d.getHours() + ":" + d.getMinutes() + ":" + d.getSeconds() + " ";
}

function appendMessage(action) {
    var elem = document.getElementById("allMessages");
    elem.value += "\n" + action.user + " - " + action.message;
}

function init() {
    
}

