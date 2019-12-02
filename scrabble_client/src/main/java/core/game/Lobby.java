package core.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// TODO: Concurrency issues?
public class Lobby {
    private Player owner;
    private transient LiveGame gameSession;
    private transient List<Player> players;
    private String descript;

    private transient int turnNumber = 0;

    public Lobby(Player owner, String descript) {
        this.owner = owner;
        this.players = new ArrayList<>();
        this.descript = descript;
        players.add(owner);
    }

    public void prepareGame() {
        gameSession = new LiveGame(players);
        gameSession.setCurrentTurn(players.get(turnNumber));
    }

    public Player getOwner() {
        return owner;
    }

    public void addPlayer(Player player) {
        synchronized (players) {
            players.add(player);
        }
    }

    public boolean removePlayer(Player player) {
        synchronized (players) {
            if (player.equals(owner)) {
                return true;
            }
            players.remove(player);
            return players.size() == 0;
        }
    }

    public Collection<Player> getPlayers() {
        return players;
    }

    public String getDescription() { return descript; }

    public LiveGame getGameSession() {
        return gameSession;
    }

    // this is of course under the assumption one player can host one game
    @Override
    public boolean equals(Object obj) {
        return obj instanceof Lobby && ((Lobby)obj).owner.equals(this.owner);
    }

    @Override
    public int hashCode() {
        return owner.hashCode();
    }


}
