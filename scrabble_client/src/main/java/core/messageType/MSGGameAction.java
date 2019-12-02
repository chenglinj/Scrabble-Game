package core.messageType;

import core.message.Message;

import java.awt.*;

public class MSGGameAction implements Message {
    // move is considered a pass if both moveLocation & letter are null
    private Point moveLocation;
    private Character letter;

    public MSGGameAction(Point moveLocation, Character letter) {
        super();
        this.moveLocation = moveLocation;
        this.letter = letter;
    }

    public Point getMoveLocation() {
        return moveLocation;
    }

    public Character getLetter() {
        return letter;
    }
}
