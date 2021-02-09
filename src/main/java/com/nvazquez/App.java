package com.nvazquez;

import com.nvazquez.websocket.reverseproxy.WebSocketReverseProxy;

/**
 * Start the reverse proxy
 *
 */
public class App {
    public static void main( String[] args ) {
        String ticket = "1f19af19cf4f576";
        WebSocketReverseProxy reverseProxy = new WebSocketReverseProxy(ticket);
        reverseProxy.start();
    }
}
