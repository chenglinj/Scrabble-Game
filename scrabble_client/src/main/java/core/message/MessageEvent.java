package core.message;

import core.game.Player;

public interface MessageEvent<T extends Message> {
    public MessageWrapper[] onMsgReceive(T recMessage, Player sender);
}
