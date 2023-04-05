package xyz.scootaloo.ws;

import io.vertx.core.*;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import xyz.scootaloo.ws.server.WebSocketServer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author AppleSack
 * @since 2023/04/05
 */
public class Launcher extends AbstractVerticle {

    private static final Vertx vertx = Vertx.vertx();

    private static final String SERVER_COMPLETE = "server.complete";

    // 可用的ws服务端口
    private static final List<Integer> availablePorts = new ArrayList<>();

    public static void launch() {
        // 监听7000-7010共11个端口
        // 其中7000为控制服务端口，其他为websocket端口
        // 这些ws端口用于模拟集群服务，即验证当客户端随机连接其中一个服务，消息是否能共享至连接到其他服务的用户

        vertx.deployVerticle(new Launcher()).onFailure(e -> {
            if (e != null) {
                e.printStackTrace();
            }
            vertx.close();
        });
    }

    @Override
    public void start(Promise<Void> startPromise) {
        var bus = vertx.eventBus();
        bus.<JsonObject>consumer(SERVER_COMPLETE, message -> {
            var body = message.body();
            var sc = body.mapTo(ServerComplete.class);
            if (sc.success) {
                // 端口监听成功
                System.out.printf("[%d] <== %s\n", sc.port, "ok");
                availablePorts.add(sc.port);
            } else {
                // 端口监听失败
                System.out.printf("[%d] <== %s reason: %s\n", sc.port, "fail", sc.reason);
            }
        });

        var range = new Range(7000, 7010);

        // 启动一个控制服务
        var ctrlFut = launchControlServer(range.start);

        // 启动多个ws服务器
        var wsFut = launchWsServers(range.start + 1, range.end);

        ctrlFut.compose(ignore -> wsFut).onComplete(done -> {
            if (done.succeeded()) {
                startPromise.complete();
            } else {
                startPromise.fail(done.cause());
            }
        });
    }

    private static Future<HttpServer> launchControlServer(int port) {
        var server = vertx.createHttpServer();
        var router = Router.router(vertx);
        router.post("/getWsServerList").handler(ctx -> ctx.json(availableWSServerList()));
        router.route().handler(StaticHandler.create().setIndexPage("index.html"));
        server.requestHandler(router);
        return server.listen(port);
    }

    private static CompositeFuture launchWsServers(int start, int end) {
        List<Future<HttpServer>> futures = new ArrayList<>(end - start + 1);
        for (int port = start; port < end; port++) {
            futures.add(launchWsServer(port));
        }
        return CompositeFuture.join(futures.stream().map(f -> f.transform(done -> {
            if (done.succeeded()) {
                return Future.succeededFuture();
            } else {
                return Future.failedFuture(done.cause());
            }
        })).collect(Collectors.toList()));
    }

    private static Future<HttpServer> launchWsServer(int port) {
        var server = vertx.createHttpServer();
        server.webSocketHandler(new WebSocketServer(vertx, port));
        return server.listen(port).onComplete(done -> wrapAndSend(done, port));
    }

    private static void wrapAndSend(AsyncResult<HttpServer> done, int port) {
        var data = new ServerComplete();
        data.success = done.succeeded();
        data.port = port;
        if (!data.success && done.cause() != null) {
            data.reason = done.cause().getMessage();
        }
        var bus = vertx.eventBus();
        bus.send(SERVER_COMPLETE, JsonObject.mapFrom(data));
    }

    private static List<String> availableWSServerList() {
        var arr = new ArrayList<String>(availablePorts.size());
        for (Integer port : Launcher.availablePorts) {
            arr.add(joinUrl(port));
        }
        return arr;
    }

    private static String joinUrl(int port) {
        return "ws://localhost:" + port;
    }

    record Range(int start, int end) {
    }

    private static class ServerComplete {
        public boolean success;
        public int     port;
        public String  reason;
    }
}
