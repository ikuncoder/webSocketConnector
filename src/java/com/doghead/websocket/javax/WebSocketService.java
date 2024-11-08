package com.doghead.websocket.javax;

import lombok.extern.slf4j.Slf4j;
import javax.websocket.*;
import java.io.IOException;
import java.net.URI;


@ClientEndpoint
@Slf4j
public class WebSocketService {

    private Session userSession = null;
    private Long startTime = System.currentTimeMillis();


    public void openWebSocketConnection(String url) {
        try {
            URI uri = URI.create(url);
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, uri);
        } catch (Exception e) {
            throw new RuntimeException("Could not open WebSocket connection", e);
        }
    }

    @OnOpen
    public void onOpen(Session userSession) {
        System.out.println(Thread.currentThread().getName() + " Connection opened.");
        this.userSession = userSession;
    }

    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        System.out.println(Thread.currentThread().getName() + " Connection closed because: " + reason);
        this.userSession = null;
    }

    @OnMessage
    public void onMessage(String message) {
        System.out.println(Thread.currentThread().getName() + " 相隔" + (System.currentTimeMillis() - startTime) + "ms,Received: " + message);
        //do something with the message
    }


    public void sendWebSocketMessage(String msg) {
        if (userSession != null && userSession.isOpen()) {
            try {
                userSession.getBasicRemote().sendText(msg);
            } catch (IOException e) {
                System.err.println("Error sending message: " + e.getMessage());
            }
        } else {
            System.err.println("WebSocket session is not open,cannot send message.");
        }
    }

    public void closeWebSocketConnection() {
        if (userSession != null && userSession.isOpen()) {
            try {
                userSession.close();
            } catch (IOException e) {
                System.err.println("Error closing WebSocket connection: " + e.getMessage());
            }
        }
    }

}

