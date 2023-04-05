package xyz.scootaloo.ws.server;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.ServerWebSocket;
import xyz.scootaloo.ws.util.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author AppleSack
 * @since 2023/04/05
 */
public class WebSocketServer implements Handler<ServerWebSocket> {

    private final Vertx  vertx;
    private final int    port;
    private final Logger log;

    private final Map<Integer, ServerWebSocket> sessions = new HashMap<>();

    public WebSocketServer(Vertx vertx, int port) {
        this.vertx = vertx;
        this.port = port;
        this.log = new Logger(port);
    }

    @Override
    public void handle(ServerWebSocket ws) {
        ws.accept();
        final AtomicInteger userId = new AtomicInteger(0);
        ws.frameHandler(frame -> {
            if (frame.isText()) {
                userId.set(1);
            }
        });
        ws.closeHandler(e -> {
            int id = userId.get();
            if (id != 0) {
                userOffline(id);
            }
        });
    }

    private void userOnline(int userId) {

    }

    private void userOffline(int userId) {
        log.receiveLog("用户[%d]下线", userId);
        sessions.remove(userId);
    }
}
