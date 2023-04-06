package xyz.scootaloo.ws.server;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;
import xyz.scootaloo.ws.util.Logger;
import xyz.scootaloo.ws.vo.ChatAct;
import xyz.scootaloo.ws.vo.ChatReqMessage;
import xyz.scootaloo.ws.vo.ChatRespMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author AppleSack
 * @since 2023/04/05
 */
public class WebSocketServer implements Handler<ServerWebSocket> {

    private final Vertx  vertx;
    private final int    port;
    private final Logger log;

    private final Map<String, ServerWebSocket> sessions = new HashMap<>();

    public WebSocketServer(Vertx vertx, int port) {
        this.vertx = vertx;
        this.port = port;
        this.log = new Logger(port);
    }

    @Override
    public void handle(ServerWebSocket ws) {
        ws.accept();
        final AtomicReference<String> userIdRef = new AtomicReference<>(null);
        ws.frameHandler(frame -> {
            if (frame.isText()) {
                handleTextFrame(ws, frame.textData(), userIdRef);
            }
        });
        ws.closeHandler(e -> {
            String id = userIdRef.get();
            if (id != null) {
                userOffline(id);
            }
        });
    }

    private void handleTextFrame(ServerWebSocket ws, String text, AtomicReference<String> ref) {
        var json = new JsonObject(text);
        var chatReq = json.mapTo(ChatReqMessage.class);
        var actType = ChatAct.valueOf(chatReq.act);
        switch (actType) {
            case LOGIN -> {
                ref.set(chatReq.username);
                userOnline(chatReq.username, ws);
            }
            case NORMAL -> {
            }
            case P2P -> {
                handleP2P(chatReq);
            }
            case BROADCAST -> {
                handleBroadcast(chatReq);
            }
        }
    }

    private void handleP2P(ChatReqMessage req) {
        // 处理点对点消息
        for (String dist : req.destinations) {
            if (sessions.containsKey(dist)) {
                var ws = sessions.get(dist);
                var resp = new ChatRespMessage();
                resp.source = req.username;
                resp.message = req.message;
                ws.writeTextMessage(JsonObject.mapFrom(resp).encode());
            }
        }
    }

    private void handleBroadcast(ChatReqMessage req) {
        // 处理广播消息
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

    private void userOnline(String username, ServerWebSocket ws) {
        log.receiveLog("用户[%s]上线", username);
        sessions.put(username, ws);
    }

    private void userOffline(String username) {
        log.receiveLog("用户[%s]下线", username);
        sessions.remove(username);
    }
}
