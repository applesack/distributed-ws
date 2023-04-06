package xyz.scootaloo.ws.vo;

/**
 * @author AppleSack
 * @since 2023/04/05
 */
public enum ChatAct {

    LOGIN,
    NORMAL,
    P2P,
    BROADCAST;

    public static ChatAct valueOf(int type) {
        switch (type) {
            case 0 -> {
                return ChatAct.LOGIN;
            }
            case 1 -> {
                return ChatAct.P2P;
            }
            case 2 -> {
                return ChatAct.BROADCAST;
            }
            case 3 -> {
                return ChatAct.NORMAL;
            }
            default -> throw new IllegalStateException("Unexpected value: " + type);
        }
    }
}
