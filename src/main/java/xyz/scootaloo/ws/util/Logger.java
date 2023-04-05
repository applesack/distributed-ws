package xyz.scootaloo.ws.util;

/**
 * @author AppleSack
 * @since 2023/04/05
 */
public class Logger {

    private static final int receive = 1;
    private static final int send    = 2;

    private final int port;

    public Logger(int port) {
        this.port = port;
    }

    public void sendLog(String pattern, Object... args) {
        consoleLog(send, pattern, args);
    }

    public void receiveLog(String pattern, Object... args) {
        consoleLog(receive, pattern, args);
    }

    private void consoleLog(int type, String pattern, Object... args) {
        String receiveMark = "<==";
        String sendMark = "==>";
        String offlineMark = "<=>";
        String mark;
        if (type == receive) {
            mark = receiveMark;
        } else {
            mark = type == send ? sendMark : offlineMark;
        }

        pattern = String.format("[%d] %s %s", port, mark, pattern);
        System.out.printf(pattern + "\n", args);
    }

}
