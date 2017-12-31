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
    socket.send(JSON.stringify(messageToSend));
}
function login() {
    setStatus("logging in");
    var credentials = {
        action: "add"
    };
    socket.send(JSON.stringify(credentials));
}
function onMessage(event) {
    setStatus("onMessage");
    var action = JSON.parse(event.data);
    if (action.action === "send") {
        appendMessage(action);
    }
}

function appendMessage(action) {
    var elem = document.getElementById("allMessages");
    elem.innerHTML += "&#013;&#010;" + action.message;
}

function init() {
    
}

