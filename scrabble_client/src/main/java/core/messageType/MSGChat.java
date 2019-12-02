package core.messageType;

import core.game.Player;
import core.message.Message;

public class MSGChat implements Message {
    private String text;
    private Player sender;
    private String lobbyName;

    public MSGChat(String text, Player sender, String lobbyName) {
        this.text = text;
        this.sender = sender;
        this.lobbyName = lobbyName;
    }

    public String getChatMsg() {
        return text;
    }
    public Player getSender() { return sender; }

    public String getLobbyName() { return lobbyName; }
}
