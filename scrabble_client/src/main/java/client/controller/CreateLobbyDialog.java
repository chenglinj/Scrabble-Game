package client.controller;

import client.Connections;
import client.util.StageUtils;
import core.game.Player;
import core.message.MessageEvent;
import core.message.MessageWrapper;
import core.messageType.MSGQuery;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class CreateLobbyDialog implements Initializable {
    @FXML
    private Button btnCreate;
    @FXML
    private Button btnCancel;
    @FXML
    private TextField txtDescript;
    @FXML
    private TextField txtName;

    private Stage waitDialog;

    private class GUIEvents {
        MessageEvent<MSGQuery> createResp = new MessageEvent<MSGQuery>() {
            @Override
            public MessageWrapper[] onMsgReceive(MSGQuery recMessage, Player sender) {
                Platform.runLater(() -> waitDialog.close());
                return null;
            }
        };

        ChangeListener<Boolean> joinedLobby = new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    Platform.runLater(() -> shutdown());
                }
            }
        };
    }

    private GUIEvents events;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        events = new GUIEvents();
        Connections.getListener().getEventList().addEvents(events.createResp);
        Connections.getListener().inLobbyProperty().addListener(events.joinedLobby);

        btnCancel.setOnAction(e -> ((Stage)btnCancel.getScene().getWindow()).close());

        btnCreate.setOnAction(e -> {
            if (txtName.getText().isEmpty()) {
                new Alert(Alert.AlertType.WARNING,"Cannot create game: The lobby name is empty.").showAndWait();
                return;
            }

            waitDialog = WaitDialogController.createDialog(
                    (Stage)btnCancel.getScene().getWindow(),
                    "Attempting to create game...");

            waitDialog.show();
            Connections.getListener().createLobby(txtName.getText(), txtDescript.getText());
        });
    }

    public void shutdown() {
        ((Stage)txtName.getScene().getWindow()).close();

        Connections.getListener().getEventList().removeEvents(events.createResp);
        Connections.getListener().inLobbyProperty().removeListener(events.joinedLobby);
    }
}
