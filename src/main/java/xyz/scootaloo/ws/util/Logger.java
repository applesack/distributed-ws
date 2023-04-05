package xyz.scootaloo.ws.util;

/**
 * @author AppleSack
 * @since 2023/04/05
 */
public class Logger {

    public void info(String pattern, Object... args) {
        System.out.printf(pattern + "\n", args);
    }

}
