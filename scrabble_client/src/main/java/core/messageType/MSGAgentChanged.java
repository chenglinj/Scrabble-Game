package core.messageType;

import core.game.Player;
import core.message.Message;

import java.util.Arrays;
import java.util.Collection;

public class MSGAgentChanged implements Message {
    private Collection<Player> players;
    private NewStatus status;
    private boolean hostLeft;

    public enum NewStatus {
        DISCONNECTED,
        JOINED
    }

    public MSGAgentChanged(NewStatus status, boolean hostLeft, Player... players) {
        super();
        this.status = status;
        this.players = Arrays.asList(players);
        this.hostLeft = hostLeft;
    }

    public MSGAgentChanged(NewStatus status, Collection<Player> players) {
        super();
        this.status = status;
        this.players = players;
    }

    public Collection<Player> getPlayers() {
        return players;
    }

    public NewStatus getStatus() {
        return status;
    }

    public boolean hasHostLeft() {
        return hostLeft;
    }
}
