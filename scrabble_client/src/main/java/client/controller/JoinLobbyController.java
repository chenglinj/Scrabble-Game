package client.controller;

import client.Connections;
import client.util.StageUtils;
import core.game.Player;
import core.game.Lobby;
import core.message.MessageEvent;
import core.message.MessageWrapper;
import core.messageType.MSGLobbyList;
import core.messageType.MSGQuery;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;

public class JoinLobbyController implements Initializable {
    @FXML
    private TableView tblLobby;
    @FXML
    private Button btnJoin;

    private ObservableMap<String, Lobby> lobbies;

    private Stage waitDialog;
    private GUIEvents events;

    private void showDialogWarn(String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING, msg);
            StageUtils.dialogCenter((Stage) btnJoin.getScene().getWindow(), alert);
            alert.showAndWait();
        });
    }

    public class GUIEvents {
        MessageEvent<MSGLobbyList> listReceived = new MessageEvent<MSGLobbyList>() {
            @Override
            public MessageWrapper[] onMsgReceive(MSGLobbyList recMessage, Player sender) {
                lobbies.putAll(recMessage.getLobbies());
                return null;
            }
        };

        MessageEvent<MSGQuery> joinResp = new MessageEvent<MSGQuery>() {
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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        events = new GUIEvents();
        Connections.getListener().getEventList().addEvents(events.joinResp, events.listReceived);
        Connections.getListener().inLobbyProperty().addListener(events.joinedLobby);

        lobbies = FXCollections.observableMap(new HashMap<String, Lobby>());

        TableColumn<String, String> colNames =
                new TableColumn<>("Name");
        TableColumn<String, String> colDescript =
                new TableColumn<>("Description");

        colNames.setCellValueFactory((param) -> { return new SimpleStringProperty(param.getValue());
        });

        colDescript.setCellValueFactory((param) -> { return new SimpleStringProperty(
                lobbies.get(param.getValue()).getDescription());
        });

        colNames.prefWidthProperty().bind(tblLobby.widthProperty().multiply(0.4));
        colDescript.prefWidthProperty().bind(tblLobby.widthProperty().multiply(0.6));

        tblLobby.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                btnJoin.setDisable(newValue == null);
            }
        });

        ObservableList<String> lobbyNames = FXCollections.observableArrayList(lobbies.keySet());
        lobbies.addListener(new MapChangeListener<String, Lobby>() {
            @Override
            public void onChanged(Change<? extends String, ? extends Lobby> change) {
                if (change.wasRemoved() != change.wasAdded() && !change.wasRemoved()) {
                    lobbyNames.add(change.getKey());
                } else {
                    // update scores
                    tblLobby.refresh();
                }
            }
        });

        btnJoin.setOnAction(e -> {
            waitDialog = WaitDialogController.createDialog(
                    (Stage)btnJoin.getScene().getWindow(),
                    "Attempting to join game...");

            waitDialog.show();
            Connections.getListener().joinLobby((String)tblLobby.getSelectionModel().getSelectedItem());
        });

        tblLobby.setItems(lobbyNames);
        tblLobby.getColumns().setAll(colNames, colDescript);

        Connections.getListener().requestAllLobbies();
    }

    public void shutdown() {
        ((Stage)tblLobby.getScene().getWindow()).close();

        Connections.getListener().getEventList().removeEvents(
                events.joinResp, events.listReceived);
        Connections.getListener().inLobbyProperty().removeListener(events.joinedLobby);
    }
}
