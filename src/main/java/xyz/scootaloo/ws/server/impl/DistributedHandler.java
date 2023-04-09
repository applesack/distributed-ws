package xyz.scootaloo.ws.server.impl;

import com.rabbitmq.client.Address;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;
import io.vertx.rabbitmq.QueueOptions;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQMessage;
import io.vertx.rabbitmq.RabbitMQOptions;
import xyz.scootaloo.ws.config.Config;
import xyz.scootaloo.ws.server.WebSocketMessageHandler;
import xyz.scootaloo.ws.vo.ChatAct;
import xyz.scootaloo.ws.vo.ChatReqMessage;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author AppleSack
 * @since 2023/04/06
 */
public class DistributedHandler extends WebSocketMessageHandler {

    private final String exchange = "ex.d.ws";

    private final RabbitMQClient client;

    public DistributedHandler(Vertx vertx, int port) {
        super(vertx, port);
        var options = new RabbitMQOptions();
        options.setUser(Config.getRabbitUser());
        options.setPassword(Config.getRabbitPass());
        options.setAddresses(List.of(Address.parseAddress(Config.getRabbitUrl())));

        // 重连策略
        options.setAutomaticRecoveryEnabled(false);
        options.setReconnectAttempts(Integer.MAX_VALUE);
        options.setReconnectInterval(500);

        client = RabbitMQClient.create(vertx, options);

        // 声明一个扇形交换机和一个队列，并让队列和交换机绑定
        final String queue = "q.d.ws." + port;

        client.addConnectionEstablishedCallback(promise -> {
            // 持久化，不自动删除
            client.exchangeDeclare(exchange, "fanout", true, false)
                    // 不持久化, 独占，自动删除
                    .compose(v -> client.queueDeclare(queue, false, true, true))
                    .compose(dok -> client.queueBind(dok.getQueue(), exchange, ""))
                    .onComplete(promise);
        });

        client.start(sok -> {
            if (sok.succeeded()) {
                // 自动确认
                client.basicConsumer(queue, new QueueOptions().setAutoAck(true), done -> {
                    if (done.succeeded()) {
                        var consumer = done.result();
                        System.out.println("消费队列为" + queue + "的消息");
                        consumer.handler(this::consume);
                    } else {
                        System.out.println("消费队列失败");
                        done.cause().printStackTrace();
                    }
                });
            } else {
                System.out.println(port + "mq 连接失败");
                sok.cause().printStackTrace();
            }
        });
    }

    @Override
    public void handle(ServerWebSocket ws) {
        final AtomicReference<String> ref = new AtomicReference<>();
        ws.handler(body -> {
            var chatReq = new JsonObject(body).mapTo(ChatReqMessage.class);
            var chatType = ChatAct.valueOf(chatReq.act);
            if (chatType == ChatAct.LOGIN) {
                log.receiveLog("用户[%s]上线", chatReq.username);
                sessions.put(chatReq.username, ws);
                ref.set(chatReq.username);
            } else {
                asyncSendMessage(body);
            }
        });
        ws.closeHandler(v -> {
            String username = ref.get();
            if (username != null) {
                log.receiveLog("用户[%s]下线", username);
                sessions.remove(username);
            }
        });
    }

    private void consume(RabbitMQMessage message) {
        var body = message.body();
        var chatReq = new JsonObject(body).mapTo(ChatReqMessage.class);
        var chatType = ChatAct.valueOf(chatReq.act);
        if (chatType == ChatAct.P2P) {
            // 点对点
            handleP2P(chatReq);
        } else {
            // 广播
            handleBroadcast(chatReq);
        }
    }

    private void asyncSendMessage(Buffer body) {
        client.basicPublish(exchange, "", body, done -> {
        });
    }
}
