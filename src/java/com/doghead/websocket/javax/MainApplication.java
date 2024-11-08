package com.doghead.websocket.javax;


public class MainApplication {
    public static void main(String[] args) {
        connect();
    }

    private static void connect() {
        String url = "";//your websocket url,for example，wss://xxxxx.com/websocket/**

        // 实例化WebSocketService
        WebSocketService webSocketService = new WebSocketService();

        // 打开WebSocket连接
        try {
            webSocketService.openWebSocketConnection(url);
            //do something after connect
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
