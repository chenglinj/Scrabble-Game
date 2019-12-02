package core.messageType;

import core.message.Message;

import java.util.Collection;

public class MSGInviteRequest implements Message {
    private Collection<String> players;

    public MSGInviteRequest(Collection<String> players) {
        this.players = players;
    }

    public Collection<String> getPlayers() {
        return players;
    }
}
