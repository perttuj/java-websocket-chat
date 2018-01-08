var websocket = new WebSocket("ws://localhost:8080/chatapp/actions");
websocket.onmessage = onMessage;
websocket.onclose = onClose;
websocket.onopen = onOpen;

function setStatus(status) {
    console.log(status);
}

function onOpen() {
    document.getElementById("allMessages").value += "Connected to server"
}

function onClose(event) {
    websocket = null;
    var reason;
    if (event.code == 1000)
            reason = "Normal closure, meaning that the purpose for which the connection was established has been fulfilled.";
        else if(event.code == 1001)
            reason = "An endpoint is \"going away\", such as a server going down or a browser having navigated away from a page.";
        else if(event.code == 1002)
            reason = "An endpoint is terminating the connection due to a protocol error";
        else if(event.code == 1003)
            reason = "An endpoint is terminating the connection because it has received a type of data it cannot accept (e.g., an endpoint that understands only text data MAY send this if it receives a binary message).";
        else if(event.code == 1004)
            reason = "Reserved. The specific meaning might be defined in the future.";
        else if(event.code == 1005)
            reason = "No status code was actually present.";
        else if(event.code == 1006)
           reason = "The connection was closed abnormally, e.g., without sending or receiving a Close control frame";
        else if(event.code == 1007)
            reason = "An endpoint is terminating the connection because it has received data within a message that was not consistent with the type of the message (e.g., non-UTF-8 [http://tools.ietf.org/html/rfc3629] data within a text message).";
        else if(event.code == 1008)
            reason = "An endpoint is terminating the connection because it has received a message that \"violates its policy\". This reason is given either if there is no other sutible reason, or if there is a need to hide specific details about the policy.";
        else if(event.code == 1009)
           reason = "An endpoint is terminating the connection because it has received a message that is too big for it to process.";
        else if(event.code == 1010) // Note that this status code is not used by the server, because it can fail the WebSocket handshake instead.
            reason = "An endpoint (client) is terminating the connection because it has expected the server to negotiate one or more extension, but the server didn't return them in the response message of the WebSocket handshake. <br /> Specifically, the extensions that are needed are: " + event.reason;
        else if(event.code == 1011)
            reason = "A server is terminating the connection because it encountered an unexpected condition that prevented it from fulfilling the request.";
        else if(event.code == 1015)
            reason = "The connection was closed due to a failure to perform a TLS handshake (e.g., the server certificate can't be verified).";
        else
            reason = "Unknown reason";
        
    document.getElementById("allMessages").value = "Error - connection closed, error code: " + event.code + ", description:\n" + reason;
}

function reconnect() {
    if (websocket === null) {
        websocket = new WebSocket("ws://localhost:8080/chatapp/actions");
    } else {
        document.getElementById("allMessages").value += "\nalready connected";
    }
}

function sendCommand(commandToSend) {
    if (websocket === null) {
        document.getElementById("allMessages").value = "not connected - you need to reconnect";
    } else {
        websocket.send(JSON.stringify(commandToSend));
    }
}

function sendMessage() {
    setStatus("sending");
    var msg = document.getElementById("messageToSend").value;
    var messageToSend = {
        action: "send",
        message: msg
    };
    document.getElementById("messageToSend").value = null;
    sendCommand(messageToSend);
}
function openChat() {
    var elem = document.getElementById("chatroomParticipants");
    var user = elem.options[elem.selectedIndex].text;
    var command = {
        action: "switchRoom",
        room: user
    };
    sendCommand(command);
}
function joinChat() {
    var list = document.getElementById("list");
    var room = list.options[list.selectedIndex].text;
    var command = {
        action: "switchRoom",
        room: room
    };
    setStatus(room);
    sendCommand(command);
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
    sendCommand(command);
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
    sendCommand(credentials);
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
function reload() {
    var request = {
        action: "reload"
    };
    sendCommand(request);
}
function connected (event) {
    var elem = document.getElementById("currentRoom");
    var user = document.getElementById("currentUser");
    var elem2 = document.getElementById("serverMessages");
    elem.innerHTML = "Current chatroom: '" + event.room + "'";
    user.innerHTML = "Current user: '" + event.user + "'";
    elem2.value += "\n" + "SERVER - " + event.message;
}
function displayAnnouncement(event) {
    var elem = document.getElementById("allMessages");
    elem.value += "\n" + "***" + event.message + "***";
}
function displayUsers(event) {
    var list = "";
    var array = event.users;
    for (var i = 0; i < array.length; i++) {
        list += "<option>" + array[i] + "</option>";
    }
    if (array.length === 0) {
        list = "<option>Empty</option>";
    }
    document.getElementById("chatroomParticipants").innerHTML = list;
}
function displaySwitch(event) {
    var room = document.getElementById("currentRoom");
    room.innerHTML = "Current room: " + event.room;
    var chat = document.getElementById("allMessages");
    chat.value = event.message;
}
function displayServerMessage(event) {
    var elem = document.getElementById("serverMessages");
    elem.value += "\nSERVER - " + event.message;
}
function displayUser (event) {
    var elem = document.getElementById("currentUser");
    elem.innerHTML = "Current user: " + event.user;
    displayServerMessage(event);
}
function onMessage(event) {
    setStatus("onMessage");
    var action = JSON.parse(event.data);
    if (action.action === "response") {
        appendMessage(action);
    } else if (action.action === "rooms") {
        displayLists(action);
    } else if (action.action === "connected") {
        connected(action);
    } else if (action.action === "users") {
        displayUsers(action);
    } else if (action.action === "announcement") {
        displayAnnouncement(action);
    } else if (action.action === "server") {
        displayServerMessage(action);
    } else if (action.action === "roomSwitch") {
        displaySwitch(action);
    } else if (action.action === "userInfo") {
        displayUser(action);
    }
}

function appendMessage(action) {
    var elem = document.getElementById("allMessages");
    elem.value += "\n" + action.user + " - " + action.message;
}

