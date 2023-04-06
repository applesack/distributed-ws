package xyz.scootaloo.ws.server.impl;

import io.vertx.core.Vertx;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;
import xyz.scootaloo.ws.server.WebSocketMessageHandler;
import xyz.scootaloo.ws.vo.ChatAct;
import xyz.scootaloo.ws.vo.ChatReqMessage;
import xyz.scootaloo.ws.vo.ChatRespMessage;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author AppleSack
 * @since 2023/04/06
 */
public class StandAloneHandler extends WebSocketMessageHandler {

    public StandAloneHandler(Vertx vertx, int port) {
        super(vertx, port);
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
            log.sendLog("接收p2p消息, 用户[%s]发送给[%s], 内容[%s]",
                    req.username, dist, req.message);
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

    private void userOnline(String username, ServerWebSocket ws) {
        log.receiveLog("用户[%s]上线", username);
        sessions.put(username, ws);
    }

    private void userOffline(String username) {
        log.receiveLog("用户[%s]下线", username);
        sessions.remove(username);
    }
}
