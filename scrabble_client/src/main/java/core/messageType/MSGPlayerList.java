package core.messageType;

import core.message.Message;

import java.util.Collection;

public class MSGPlayerList implements Message {
    private Collection<String> playerNames;

    public MSGPlayerList(Collection<String> names) {
        this.playerNames = names;
    }

    public Collection<String> getPlayerNames() {
        return playerNames;
    }
}
