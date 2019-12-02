package core.messageType;

import core.game.Player;
import core.message.Message;

// represents results from last turn, as well as indicator to new turn
public class MSGNewTurn implements Message {
    private Player last_player;
    private Player next_player;
    private int new_points;
    private boolean skippedTurn;

    public MSGNewTurn(Player last_player, Player next_player,
                      int new_points, boolean skippedTurn) {

        this.last_player = last_player;
        this.next_player = next_player;
        this.new_points = new_points;
        this.skippedTurn = skippedTurn;
    }

    public int getNewPoints() {
        return new_points;
    }

    public boolean hasSkippedTurn() {
        return skippedTurn;
    }

    public Player getLastPlayer() {
        return last_player;
    }

    public Player getNextPlayer() {
        return next_player;
    }
}
