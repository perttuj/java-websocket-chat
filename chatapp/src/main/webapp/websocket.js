/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
window.onload = init;
var websocket = new WebSocket("ws://localhost:8080/chatapp/actions");
websocket.onmessage = onMessage;
websocket.onclose = onClose;
websocket.onopen = onOpen;

function setStatus(status) {
    document.getElementById("test").value = status;
}

function onOpen() {
    document.getElementById("allMessages").value += "Connected to server"
}

function onClose(event) {
    document.getElementById("allMessages").value = "Error - connection closed, error code: " + event.code;
}

function sendMessage() {
    setStatus("sending");
    var msg = document.getElementById("messageToSend").value;
    var messageToSend = {
        action: "send",
        message: msg
    };
    document.getElementById("messageToSend").value = null;
    websocket.send(JSON.stringify(messageToSend));
}
function joinChat() {
    var list = document.getElementById("list");
    var room = list.options[list.selectedIndex].text;
    var command = {
        action: "switchRoom",
        room: room
    }
    setStatus(room);
    websocket.send(JSON.stringify(command));
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
    websocket.send(JSON.stringify(command));
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
    websocket.send(JSON.stringify(credentials));
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
    websocket.send(JSON.stringify(request));
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
    var elem;
    if (action.user === "SERVER") {
        elem = document.getElementById("serverMessages");
    } else {
        elem = document.getElementById("allMessages");
    }
    elem.value += "\n" + action.user + " - " + action.message;
}

function init() {
    
}

