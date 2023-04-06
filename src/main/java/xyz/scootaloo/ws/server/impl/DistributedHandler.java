package xyz.scootaloo.ws.server.impl;

import io.vertx.core.Vertx;
import io.vertx.core.http.ServerWebSocket;
import xyz.scootaloo.ws.server.WebSocketMessageHandler;

/**
 * @author AppleSack
 * @since 2023/04/06
 */
public class DistributedHandler extends WebSocketMessageHandler {
    public DistributedHandler(Vertx vertx, int port) {
        super(vertx, port);
    }
    @Override
    public void handle(ServerWebSocket serverWebSocket) {

    }
}
