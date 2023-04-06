package xyz.scootaloo.ws.server;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.ServerWebSocket;
import xyz.scootaloo.ws.config.Config;

/**
 * @author AppleSack
 * @since 2023/04/05
 */
public class WebSocketServer implements Handler<ServerWebSocket> {

    private final WebSocketMessageHandler handler;

    public WebSocketServer(Vertx vertx, int port) {
        if (Config.getMode().startsWith("s")) {
            handler = WebSocketMessageHandler.createStandalone(vertx, port);
        } else {
            handler = WebSocketMessageHandler.createDistributed(vertx, port);
        }
    }

    @Override
    public void handle(ServerWebSocket ws) {
        handler.handle(ws);
    }

}
