package core.messageType;

import core.game.GameRules;
import core.message.Message;

import java.awt.*;
import java.util.*;
import java.util.List;

import static core.game.LiveGame.NUM_COLS;
import static core.game.LiveGame.NUM_ROWS;

/**
 * From client: From a client, the client.listeners records their decision
 * if they think the string is a word or not.
 *
 * From client.listeners: Broadcasts to all players the verdict via accepted.
 */
public class MSGGameVote implements Message {
    private GameRules.Orientation orient;
    private boolean accepted;

    public MSGGameVote(GameRules.Orientation orient, boolean accepted) {
        this.orient = orient;
        this.accepted = accepted;
    }

    public GameRules.Orientation getOrient() {
        return orient;
    }

    public boolean isAccepted() {
        return accepted;
    }
}
