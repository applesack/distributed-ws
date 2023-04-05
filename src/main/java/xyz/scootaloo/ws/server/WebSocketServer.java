package xyz.scootaloo.ws.server;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.ServerWebSocket;

import java.util.HashMap;
import java.util.Map;

/**
 * @author AppleSack
 * @since 2023/04/05
 */
public class WebSocketServer implements Handler<ServerWebSocket> {

    private final Vertx vertx;
    private final int   port;

    private final Map<Integer, ServerWebSocket> sessions = new HashMap<>();

    public WebSocketServer(Vertx vertx, int port) {
        this.vertx = vertx;
        this.port = port;
    }

    @Override
    public void handle(ServerWebSocket ws) {
        ws.accept();
        ws.frameHandler(frame -> {

        });
        ws.closeHandler(e -> {

        });
    }
}
