package com.nvazquez.websocket.reverseproxy;

import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.extensions.DefaultExtension;
import org.java_websocket.protocols.Protocol;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.logging.Logger;

/**
 *                       -----------------------------
 *     WS CLIENT <---->  || serverSide - clientSide || <----> WS SERVER
 *                       -----------------------------
 */
public class WebSocketReverseProxy implements Runnable {

    private static WebSocketReverseProxyClientSide clientSide;
    private static WebSocketReverseProxyServerSide serverSide;

    private static Logger logger = Logger.getLogger(WebSocketReverseProxy.class.getName());

    private String ticket;

    private static final int SERVER_SIDE_PORT = 1234;
    private Draft_6455 draft;

    private Thread selectorthread;

    public void start() {
        if (selectorthread != null) {
            throw new IllegalStateException(getClass().getName() + " can only be started once.");
        }
        new Thread(this).start();
        selectorthread = Thread.currentThread();
    }

    public WebSocketReverseProxy(String ticket) {
        this.ticket = ticket;
    }

    private void initServerSide() {
        Protocol protocol = new Protocol("binary");
        DefaultExtension defaultExtension = new DefaultExtension();
        draft = new Draft_6455(Collections.singletonList(defaultExtension), Collections.singletonList(protocol));
        serverSide = new WebSocketReverseProxyServerSide(SERVER_SIDE_PORT, this, Collections.singletonList(draft));
        serverSide.start();
        logger.info("[REVERSE-PROXY] Server side started on port " + SERVER_SIDE_PORT);
    }

    @Override
    public void run() {
        initServerSide();
        boolean interrupted = false;
        while (!interrupted) {
            interrupted = selectorthread.isInterrupted();
        }
    }

    public void initClientSide() throws URISyntaxException {
        String wsUrl = "wss://10.10.2.2:443/ticket/" + this.ticket;
        URI uri = new URI(wsUrl);
        clientSide = new WebSocketReverseProxyClientSide(uri, this, draft);
        clientSide.connect();
        logger.info("[REVERSE-PROXY] Client side connected to " + wsUrl);
    }

    public void proxyMsgServerToClientSide(ByteBuffer msg) {
        try {
            if (clientSide == null) {
                initClientSide();
            }
            clientSide.receiveProxiedMsg(msg);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void proxyMsgClientToServerSide(ByteBuffer msg) {
        try {
            serverSide.receiveProxiedMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
