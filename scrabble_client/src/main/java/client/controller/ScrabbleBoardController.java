package client.controller;

import client.ClientMain;
import client.Connections;
import core.game.Board;
import core.game.LiveGame;
import core.messageType.MSGNewTurn;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import client.boardUI.BoardPane;

import java.awt.*;
import java.net.URL;
import java.util.ResourceBundle;

public class ScrabbleBoardController implements Initializable {
    @FXML
    private Button btnSubmit;
    @FXML
    private Button btnPass;
    @FXML
    private Button btnClear;
    @FXML
    private Button btnVote;
    @FXML
    private HBox hbox;
    @FXML
    public BoardPane boardPane;
    @FXML
    private Label lblTurn;
    @FXML
    private Label lblScore;
    @FXML
    private Label lblPing;

    private Board board;

    public ScrabbleBoardController(Board board) {
        this.board = board;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        boardPane.setBoard(board);

        btnSubmit.setFocusTraversable(false);
        btnPass.setFocusTraversable(false);
        btnClear.setFocusTraversable(false);
        btnVote.setFocusTraversable(false);

        btnClear.disableProperty().bind(boardPane.enabledProperty());
        btnSubmit.disableProperty().bind(boardPane.enabledProperty());
        btnPass.disableProperty().bind(boardPane.enabledProperty().not());

        btnPass.setOnMouseClicked(e ->
                Connections.getListener().sendGamePass());

        btnSubmit.setOnMouseClicked(e -> {
            if (!boardPane.enabledProperty().get()) {
                Point p = boardPane.chosenCellProperty().get();
                Connections.getListener().sendGameMove(p, board.get(p));
            }
        });

        btnClear.setOnMouseClicked(e -> {
            if (boardPane.chosenCellProperty().get() != null)
                board.empty(boardPane.chosenCellProperty().get());
        });

        // display approx ping between client-server
        Connections.getListener().pingMSProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue observable, Number oldValue, Number newValue) {
                Platform.runLater(() ->
                        lblPing.setText(String.format("Ping: %.1f ms", newValue)));
            }
        });
    }

    public void updateUI(MSGNewTurn msg, int myScore) {
        lblTurn.setText(String.format("It is player %s's turn", msg.getNextPlayer().getName()));
        lblScore.setText("Your Score: " + myScore);

        boolean isMyTurn = msg.getNextPlayer().equals(Connections.playerProperty().get());
        hbox.disableProperty().set(!isMyTurn);
        boardPane.enabledProperty().set(isMyTurn);
        boardPane.chosenCellProperty().set(null);
    }
}
