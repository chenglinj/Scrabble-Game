package client;

import client.controller.VoteScreenForm;
import client.util.StageUtils;
import core.game.Board;
import client.controller.ChatBoxController;
import client.controller.ScoreBoxController;
import com.anchorage.docks.node.DockNode;
import com.anchorage.docks.stations.DockStation;
import com.anchorage.system.AnchorageSystem;
import core.game.Player;
import core.game.GameRules;
import core.game.LiveGame;
import core.message.MessageEvent;
import core.message.MessageWrapper;
import core.messageType.*;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import client.controller.ScrabbleBoardController;
import javafx.stage.StageStyle;

import java.awt.*;
import java.io.IOException;
import java.util.Map;

public class GameWindow {
    private ChatBoxController chatBox;
    private ScrabbleBoardController scrabbleBoard;
    private ScoreBoxController scoreBoard;
    private DockStation station;

    private final Board board;
    private final Stage mainStage;
    private Stage popupStage;

    private class GUIEvents {
        // chat message
        MessageEvent<MSGChat> chatEvent = new MessageEvent<MSGChat>() {
            @Override
            public MessageWrapper[] onMsgReceive(MSGChat recMessage, Player sender) {
                chatBox.appendText(String.format("%s said:\t%s\n",
                        recMessage.getSender().getName(), recMessage.getChatMsg()), Color.BLACK);
                return null;
            }
        };

        // received move from player
        MessageEvent<MSGGameAction> actionEvent = new MessageEvent<MSGGameAction>() {
            @Override
            public MessageWrapper[] onMsgReceive(MSGGameAction recMessage, Player sender) {
                Platform.runLater(() -> {
                    Point p = recMessage.getMoveLocation();

                    board.set(p.x, p.y, recMessage.getLetter());

                    Map<GameRules.Orientation, String> strMap =
                            GameRules.getValidOrientations(board, p);

                    scrabbleBoard.boardPane.chosenCellProperty().set(p);
                    popupVoteScreen(
                            strMap.get(GameRules.Orientation.HORIZONTAL),
                            strMap.get(GameRules.Orientation.VERTICAL));
                });

                return null;
            }
        };

        MessageEvent<MSGNewTurn> newTurnEvent = new MessageEvent<MSGNewTurn>() {
            @Override
            public MessageWrapper[] onMsgReceive(MSGNewTurn recMessage, Player sender) {
                Platform.runLater(() -> updateTurn(recMessage));
                return null;
            }
        };

        MessageEvent<MSGGameStatus> gameEndEvent = new MessageEvent<MSGGameStatus>() {
            @Override
            public MessageWrapper[] onMsgReceive(MSGGameStatus recMessage, Player sender) {
                Platform.runLater(() -> ClientMain.endApp("Game has ended. \nThe winner is " + scoreBoard.getWinner() + ".\nApp will now close." + "\n"));
                return null;
            }
        };

        MessageEvent<MSGAgentChanged> playerLeftEvent = new MessageEvent<MSGAgentChanged>() {
            @Override
            public MessageWrapper[] onMsgReceive(MSGAgentChanged recMessage, Player sender) {
                Platform.runLater(() ->
                        ClientMain.endApp("A player has disconnected from the game. App will now close."));
                return null;
            }
        };
    }

    private GUIEvents events;

    public GameWindow(LiveGame initGame) throws IOException {
        board = new Board(initGame.getBoard().getNumRows(),
                initGame.getBoard().getNumColumns());

        mainStage = prepareUI(initGame);

        // add events to client listener
        events = new GUIEvents();
        Connections.getListener().getEventList().addEvents(
                events.chatEvent,
                events.actionEvent,
                events.newTurnEvent,
                events.gameEndEvent,
                events.playerLeftEvent);
    }

    public void show() {
        mainStage.show();
    }

    private Stage prepareUI(LiveGame initGame) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ScrabbleBoard.fxml"));
        scrabbleBoard = new ScrabbleBoardController(board);
        loader.setController(scrabbleBoard);

        station = AnchorageSystem.createStation();

        DockNode node1 = null;
        node1 = AnchorageSystem.createDock("Game Board", loader.load());
        node1.dock(station, DockNode.DockPosition.LEFT);

        node1.closeableProperty().set(false);
        node1.floatableProperty().set(false);

        Parent chat_root = null;
        chatBox = new ChatBoxController();
        loader = new FXMLLoader(getClass().getResource("/ChatBox.fxml"));
        loader.setController(chatBox);
        DockNode node2 = AnchorageSystem.createDock("Chat", loader.load());
        node2.dock(station, DockNode.DockPosition.BOTTOM, 0.75);

        node2.closeableProperty().set(false);

        // add score table;
        loader = new FXMLLoader(this.getClass().getResource("/ScoreBox.fxml"));
        scoreBoard = new ScoreBoxController(initGame.getScores());
        loader.setController(scoreBoard);
        DockNode node3 = null;
        node3 = AnchorageSystem.createDock("Scores", loader.load());

        node3.closeableProperty().set(false);

        node3.dock(station, DockNode.DockPosition.RIGHT, 0.8);

        AnchorageSystem.installDefaultStyle();

        Stage ret = new Stage();
        Scene scene = new Scene(station, 800, 700);
        ret.setTitle(String.format("[%s] Scrabble Game (Lobby - %s)",
                Connections.playerProperty().get(),
                Connections.getListener().getLobbyName()));
        ret.setScene(scene);

        // TODO: Temporary fix
        ret.setOnCloseRequest(e -> System.exit(0));

        // update UI and show window
        updateTurn(new MSGNewTurn(initGame.getCurrentTurn(), initGame.getCurrentTurn(),
                0, false));

        return ret;
    }


    public void updateTurn(MSGNewTurn msg) {
        closePopup();

        // inform last player's move
        String txtAppend = "";
        int scoreDiff = msg.getNewPoints() - scoreBoard.scores.get(msg.getLastPlayer());
        if (msg.hasSkippedTurn()) {
            txtAppend = String.format("%s has skipped turn.", msg.getLastPlayer());
        } else if (scoreDiff != 0) {
            txtAppend = String.format("%s has earned %d point"
                    + (scoreDiff == 1 ? "." : "s."), msg.getLastPlayer(), scoreDiff);
        }

        if (!txtAppend.isEmpty())
            chatBox.appendText(txtAppend + "\n", Color.DARKCYAN);

        // update scores first, then display it on UI
        scoreBoard.updateScore(msg.getLastPlayer(), msg.getNewPoints());
        scrabbleBoard.updateUI(msg,
                scoreBoard.scores.get(Connections.playerProperty().get()));
    }

    public void popupVoteScreen(String hor_str, String ver_str) {
        FXMLLoader loader = new FXMLLoader(this.getClass().getResource("/VoteScreen.fxml"));
        VoteScreenForm voteForm = new VoteScreenForm();
        loader.setController(voteForm);

        popupStage = new Stage(StageStyle.UNDECORATED);
        popupStage.setOpacity(0.96);
        popupStage.initOwner(station.getScene().getWindow());
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.setTitle("Voting Time");

        try {
            Scene voteScene = new Scene(loader.load());
            voteScene.setFill(Color.TRANSPARENT);
            popupStage.setScene(voteScene);
        } catch (IOException e) {
            e.printStackTrace();
        }

        voteForm.displayStrings(hor_str, ver_str);
        popupStage.setOnShown(e ->
                StageUtils.centreStage((Stage)station.getScene().getWindow(), popupStage));
        popupStage.show();
    }

    public void closePopup() {
        if (popupStage != null)
            popupStage.close();
    }
}
