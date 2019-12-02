package core.messageType;

import core.message.Message;

public class MSGJoinLobby implements Message {
    private String lobbyName;
    // if description isn't empty, it's assumed player is trying to create game
    private String description;

    public MSGJoinLobby(String lobbyName) {
        this.lobbyName = lobbyName;
        this.description = null;
    }

    public MSGJoinLobby(String lobbyName, String description) {
        this(lobbyName);
        this.description = description;
    }

    public String getLobbyName() {
        return lobbyName;
    }

    public String getDescription() {
        return description;
    }
}
