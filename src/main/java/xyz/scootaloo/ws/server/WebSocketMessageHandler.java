package xyz.scootaloo.ws.server;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;
import xyz.scootaloo.ws.server.impl.DistributedHandler;
import xyz.scootaloo.ws.server.impl.StandAloneHandler;
import xyz.scootaloo.ws.util.Logger;
import xyz.scootaloo.ws.vo.ChatReqMessage;
import xyz.scootaloo.ws.vo.ChatRespMessage;

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

    protected void handleP2P(ChatReqMessage req) {
        // 处理点对点消息
        for (String dist : req.destinations) {
            if (sessions.containsKey(dist)) {
                log.sendLog("接收p2p消息, 用户[%s]发送给[%s], 内容[%s]",
                        req.username, dist, req.message);
                var ws = sessions.get(dist);
                var resp = new ChatRespMessage();
                resp.source = req.username;
                resp.message = req.message;
                ws.writeTextMessage(JsonObject.mapFrom(resp).encode());
            }
        }
    }

    protected void handleBroadcast(ChatReqMessage req) {
        // 处理广播消息
        String users = sessions.keySet().toString();
        log.sendLog("接收广播消息, 由用户[%s]发起, 当前服务器内的用户[%s], 消息内容[%s]",
                req.username, users, req.message);
        for (Map.Entry<String, ServerWebSocket> entry : sessions.entrySet()) {
            var key = entry.getKey();
            if (key.equals(req.username)) {
                continue;
            }
            var session = entry.getValue();
            var resp = new ChatRespMessage();
            resp.source = req.username;
            resp.message = req.message;
            session.writeTextMessage(JsonObject.mapFrom(resp).encode());
        }
    }

    public static WebSocketMessageHandler createStandalone(Vertx vertx, int port) {
        return new StandAloneHandler(vertx, port);
    }

    public static WebSocketMessageHandler createDistributed(Vertx vertx, int port) {
        return new DistributedHandler(vertx, port);
    }

}
