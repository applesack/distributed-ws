package xyz.scootaloo.ws.server;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.ServerWebSocket;
import xyz.scootaloo.ws.server.impl.DistributedHandler;
import xyz.scootaloo.ws.server.impl.StandAloneHandler;
import xyz.scootaloo.ws.util.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author AppleSack
 * @since 2023/04/06
 */
public abstract class WebSocketMessageHandler implements Handler<ServerWebSocket> {

    protected final Map<String, ServerWebSocket> sessions = new HashMap<>();
    protected final Vertx                        vertx;
    protected final Logger                       log;

    public WebSocketMessageHandler(Vertx vertx, int port) {
        this.vertx = vertx;
        this.log = new Logger(port);
    }

    public static WebSocketMessageHandler createStandalone(Vertx vertx, int port) {
        return new StandAloneHandler(vertx, port);
    }

    public static WebSocketMessageHandler createDistributed(Vertx vertx, int port) {
        return new DistributedHandler(vertx, port);
    }

}
