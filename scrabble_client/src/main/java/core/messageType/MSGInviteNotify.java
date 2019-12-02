package core.messageType;

import core.message.Message;

public class MSGInviteNotify implements Message {
    private String lobbyName;

    public MSGInviteNotify(String lobbyName) {
        this.lobbyName = lobbyName;
    }

    public String getLobbyName() {
        return lobbyName;
    }
}
