package xyz.scootaloo.ws.config;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;


/**
 * @author AppleSack
 * @since 2023/04/06
 */
public class Config {

    private static final String CONFIG_FILE     = "config";
    private static final String FILE_FORMAT     = "json";
    private static final String RABBIT_URL_KEY  = "rabbit-url";
    private static final String RABBIT_USER_KEY = "rabbit-user";
    private static final String RABBIT_PASS_KEY = "rabbit-pass";
    private static final String MODE_KEY        = "mode";

    private static String rabbitUrl;
    private static String rabbitUser;
    private static String rabbitPass;
    private static String mode;

    public static Future<Void> load(Vertx vertx) {
        var fs = vertx.fileSystem();
        var defFile = getConfigFile(null);
        return fs.exists(defFile).compose(exists -> {
            if (!exists) {
                System.out.println("默认配置文件未找到");
                return Future.succeededFuture();
            }
            return fs.readFile(defFile);
        }).compose(buff -> {
            // 如果文件不存在，那么buff为空
            if (buff == null) {
                return Future.succeededFuture();
            }

            var json = new JsonObject(buff);
            if (json.containsKey("active")) {
                var profile = json.getString("active");
                System.out.printf("正在使用%s配置文件\n", profile);
                return fs.readFile(getConfigFile(profile));
            }
            return Future.succeededFuture();
        }).compose(profileBuffer -> {
            // 如果文件不存在，那么buff为空
            if (profileBuffer == null) {
                return Future.succeededFuture();
            }

            var json = new JsonObject(profileBuffer);
            if (json.containsKey(RABBIT_URL_KEY)) {
                rabbitUrl = json.getString(RABBIT_URL_KEY);
            }
            if (json.containsKey(RABBIT_USER_KEY)) {
                rabbitUser = json.getString(RABBIT_USER_KEY);
            }
            if (json.containsKey(RABBIT_PASS_KEY)) {
                rabbitPass = json.getString(RABBIT_PASS_KEY);
            }
            if (json.containsKey(MODE_KEY)) {
                mode = json.getString(MODE_KEY, "");
            }
            return Future.succeededFuture();
        });
    }

    private static String getConfigFile(String profile) {
        if (profile == null) {
            return CONFIG_FILE + "." + FILE_FORMAT;
        }
        return CONFIG_FILE + "-" + profile + "." + FILE_FORMAT;
    }

    public static String getRabbitUrl() {
        return rabbitUrl;
    }

    public static String getRabbitUser() {
        return rabbitUser;
    }

    public static String getRabbitPass() {
        return rabbitPass;
    }

    public static String getMode() {
        return mode;
    }
}
