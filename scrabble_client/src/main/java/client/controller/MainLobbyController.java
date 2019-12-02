package client.controller;

import client.Connections;
import client.util.StageUtils;
import core.game.Player;
import core.message.MessageEvent;
import core.message.MessageWrapper;
import core.messageType.MSGChat;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainLobbyController implements Initializable {
    @FXML
    private AnchorPane paneChat;
    @FXML
    private Button btnJoin;
    @FXML
    private Button btnCreate;

    private ChatBoxController chatBox;
    private GUIEvents events;

    private class GUIEvents {
        // message received
        MessageEvent<MSGChat> chatEvent = new MessageEvent<MSGChat>() {
            @Override
            public MessageWrapper[] onMsgReceive(MSGChat recMessage, Player sender) {
                chatBox.appendText(String.format("%s said:\t%s\n",
                        recMessage.getSender().getName(), recMessage.getChatMsg()), Color.BLACK);
                return null;
            }
        };

        ChangeListener<Boolean> joinedLobby = new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    Platform.runLater(() -> loadLobbyWindow());
                }
            }
        };
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ChatBox.fxml"));
        chatBox = new ChatBoxController();
        loader.setController(chatBox);

        // load & add chat box
        Node node = null;
        try {
            node = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }

        AnchorPane.setBottomAnchor(node, 0.0);
        AnchorPane.setTopAnchor(node, 0.0);
        AnchorPane.setLeftAnchor(node, 0.0);
        AnchorPane.setRightAnchor(node, 0.0);
        paneChat.getChildren().add(node);

        btnJoin.setOnAction(e -> {
            FXMLLoader loader1 = new FXMLLoader(getClass().getResource("/JoinLobby.fxml"));
            JoinLobbyController joinLobbyController = new JoinLobbyController();

            loader1.setController(joinLobbyController);

            Stage joinStage = new Stage();
            joinStage.initModality(Modality.APPLICATION_MODAL);
            joinStage.initOwner(btnJoin.getScene().getWindow());
            joinStage.setTitle("Join Lobby");

            try {
                joinStage.setScene(new Scene(loader1.load()));
                joinStage.setOnShown(e2 ->
                        StageUtils.centreStage((Stage)btnJoin.getScene().getWindow(), joinStage));
                joinStage.setOnCloseRequest(e2 -> joinLobbyController.shutdown());
                joinStage.show();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });

        btnCreate.setOnAction(e -> {
            FXMLLoader loader2 = new FXMLLoader(getClass().getResource("/CreateLobbyDialog.fxml"));
            CreateLobbyDialog createLobbyController = new CreateLobbyDialog();

            loader2.setController(createLobbyController);

            Stage createStage = new Stage();
            createStage.initModality(Modality.APPLICATION_MODAL);
            createStage.initOwner(btnCreate.getScene().getWindow());
            createStage.setTitle("Create a Lobby");

            try {
                createStage.setScene(new Scene(loader2.load()));
                createStage.setOnShown(e2 ->
                        StageUtils.centreStage((Stage)btnCreate.getScene().getWindow(), createStage));

                createStage.setOnCloseRequest(e2 -> createLobbyController.shutdown());

                createStage.show();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });


        // now add events
        events = new GUIEvents();
        Connections.getListener().getEventList().addEvents(events.chatEvent);

        Connections.getListener().inLobbyProperty().addListener(events.joinedLobby);
    }

    private void loadLobbyWindow() {
        shutdown();
        ((Stage)btnCreate.getScene().getWindow()).close();
        LobbyController.createStage("", "", true).show();
    }

    public void shutdown() {
        Connections.getListener().inLobbyProperty().removeListener(events.joinedLobby);
        Connections.getListener().getEventList().removeEvents(events.chatEvent);
    }
}
