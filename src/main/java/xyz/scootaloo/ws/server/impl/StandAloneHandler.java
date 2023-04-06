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

    private void userOnline(String username, ServerWebSocket ws) {
        log.receiveLog("用户[%s]上线", username);
        sessions.put(username, ws);
    }

    private void userOffline(String username) {
        log.receiveLog("用户[%s]下线", username);
        sessions.remove(username);
    }
}
