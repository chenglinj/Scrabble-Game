package core.messageType;

import core.game.Player;
import core.message.Message;

public class MSGLogin implements Message {
    private Player player;

    public MSGLogin(Player player) {
        this.player = player;
    }

    public Player getPlayer() { return player; }
}
