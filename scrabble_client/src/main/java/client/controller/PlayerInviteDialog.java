package client.controller;

import client.Connections;
import core.game.Player;
import core.message.MessageEvent;
import core.message.MessageWrapper;
import core.messageType.MSGPlayerList;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class PlayerInviteDialog implements Initializable {
    @FXML
    private Button btnInvite;
    @FXML
    private TableView tblPlayers;

    private ObservableList<String> lobbyNames;
    private GUIEvents events;

    private class GUIEvents {
        MessageEvent<MSGPlayerList> listReceived = new MessageEvent<MSGPlayerList>() {
            @Override
            public MessageWrapper[] onMsgReceive(MSGPlayerList recMessage, Player sender) {
                lobbyNames.addAll(recMessage.getPlayerNames());
                return null;
            }
        };
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        events = new GUIEvents();
        Connections.getListener().getEventList().addEvents(events.listReceived);

        TableColumn<String, String> colNames = new TableColumn<>("Player name");
        colNames.setCellValueFactory((param) -> { return new SimpleStringProperty(param.getValue()); });
        colNames.prefWidthProperty().bind(tblPlayers.widthProperty());

        lobbyNames = FXCollections.observableArrayList();

        tblPlayers.setItems(lobbyNames);
        tblPlayers.getColumns().setAll(colNames);
        tblPlayers.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        tblPlayers.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                btnInvite.setDisable(newValue == null);
            }
        });

        btnInvite.setOnAction(e -> {
            Connections.getListener().sendInvites(tblPlayers.getSelectionModel().getSelectedItems());
            ((Stage)tblPlayers.getScene().getWindow()).close();
        });

        Connections.getListener().requestOnlinePlayers();
    }

    public void shutdown() {
        Connections.getListener().getEventList().removeEvents(events.listReceived);
    }
}
