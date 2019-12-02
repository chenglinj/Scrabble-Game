package core.messageType;

import core.game.Lobby;
import core.message.Message;

import java.util.Collection;
import java.util.Map;

public class MSGLobbyList implements Message {
    public Map<String, Lobby> lobbies;

    public MSGLobbyList(Map<String, Lobby> lobbies) {
        this.lobbies = lobbies;
    }

    public Map<String, Lobby> getLobbies() {
        return lobbies;
    }
}
