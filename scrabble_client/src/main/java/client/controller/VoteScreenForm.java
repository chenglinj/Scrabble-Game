package client.controller;

import client.Connections;
import core.game.GameRules;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.util.ResourceBundle;

public class VoteScreenForm implements Initializable {
    @FXML
    private Label horWord;
    @FXML
    private Label verWord;
    @FXML
    private Button horAccept;
    @FXML
    private Button horReject;
    @FXML
    private Button verAccept;
    @FXML
    private Button verReject;
    @FXML
    private GridPane grdVote;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        horAccept.visibleProperty().bind(Bindings.isEmpty(horWord.textProperty()).not());
        horReject.visibleProperty().bind(Bindings.isEmpty(horWord.textProperty()).not());

        verAccept.visibleProperty().bind(Bindings.isEmpty(verWord.textProperty()).not());
        verReject.visibleProperty().bind(Bindings.isEmpty(verWord.textProperty()).not());


        horAccept.setOnAction(e -> {
            horAccept.setDisable(true);
            horReject.setDisable(true);

            Connections.getListener().sendGameVote(GameRules.Orientation.HORIZONTAL, true);
        });

        horReject.setOnAction(e -> {
            horAccept.setDisable(true);
            horReject.setDisable(true);

            Connections.getListener().sendGameVote(GameRules.Orientation.HORIZONTAL, false);
        });

        verAccept.setOnAction(e -> {
            verAccept.setDisable(true);
            verReject.setDisable(true);

            Connections.getListener().sendGameVote(GameRules.Orientation.VERTICAL, true);
        });

        verReject.setOnAction(e -> {
            verAccept.setDisable(true);
            verReject.setDisable(true);

            Connections.getListener().sendGameVote(GameRules.Orientation.VERTICAL, false);
        });
    }

    public void displayStrings(String str_hor, String str_ver) {
        horWord.setText(str_hor);
        verWord.setText(str_ver);
    }
}
