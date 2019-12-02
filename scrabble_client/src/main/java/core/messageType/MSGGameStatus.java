package core.messageType;

import core.game.LiveGame;
import core.message.Message;

public class MSGGameStatus implements Message {
    public enum GameStatus {
        STARTED,
        ENDED
    }

    private GameStatus status;
    private LiveGame gameData;

    public MSGGameStatus(GameStatus status, LiveGame gameData) {
        this.status = status;
        this.gameData = gameData;
    }

    public GameStatus getGameStatus() {
        return status;
    }

    public LiveGame getGameData() { return gameData; }
}
