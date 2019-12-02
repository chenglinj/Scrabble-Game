package client.controller;

import client.ClientMain;
import client.Connections;
import client.GameWindow;
import client.util.StageUtils;
import core.ConnectType;
import core.game.Player;
import core.message.MessageEvent;
import core.message.MessageWrapper;
import core.messageType.MSGAgentChanged;
import core.messageType.MSGChat;
import core.messageType.MSGGameStatus;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class LobbyController implements Initializable {
    @FXML
    private ListView lstPlayers;
    @FXML
    private  ListView lstReceivers;
    @FXML
    private Button btnStartGame;
    @FXML
    private Button btnInvite;
    @FXML
    private AnchorPane chatPane;

    private ChatBoxController chatBox;

    private boolean isHosting;

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

        // when client.listeners sends the initial list of players in lobby
        MessageEvent<MSGAgentChanged> getPlayersEvent = new MessageEvent<MSGAgentChanged>() {
            @Override
            public MessageWrapper[] onMsgReceive(MSGAgentChanged recMessage, Player sender) {
                switch (recMessage.getStatus()) {
                    case JOINED:
                        Platform.runLater(() -> lstPlayers.getItems().addAll(recMessage.getPlayers()));
                        break;
                    case DISCONNECTED:
                        Platform.runLater(() -> lstPlayers.getItems().removeAll(recMessage.getPlayers()));
                        break;
                }

                return null;
            }
        };

        // when player joins or leaves the lobby
        MessageEvent<MSGAgentChanged> getPlayerStatus = new MessageEvent<MSGAgentChanged>() {
            @Override
            public MessageWrapper[] onMsgReceive(MSGAgentChanged recMessage, Player sender) {
                // if host has left the lobby
                if (recMessage.hasHostLeft()) {
                    Platform.runLater(() ->
                            ClientMain.endApp("The host has left the lobby. The app will now close."));
                }

                for (Player player : recMessage.getPlayers()) {
                    switch (recMessage.getStatus()) {
                        case JOINED:
                            chatBox.appendText(String.format("%s has joined the lobby.\n", player),
                                    Color.GREEN);
                            break;
                        case DISCONNECTED:
                            chatBox.appendText(String.format("%s has left the lobby.\n", player),
                                    Color.RED);
                            break;
                    }
                }

                return null;
            }
        };

        // host has announced the game to start
        MessageEvent<MSGGameStatus> gameStartEvent = new MessageEvent<MSGGameStatus>() {
            @Override
            public MessageWrapper[] onMsgReceive(MSGGameStatus recMessage, Player sender) {
                Platform.runLater(() -> {
                    shutdown(); // clear events
                    ((Stage)btnStartGame.getScene().getWindow()).close();

                    try {
                        GameWindow gameWindow = new GameWindow(recMessage.getGameData());
                        gameWindow.show();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                return null;
            }
        };
    }

    private GUIEvents events;

    public LobbyController(boolean isHosting) {
        this.isHosting = isHosting;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // add chat box
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ChatBox.fxml"));
        chatBox = new ChatBoxController();
        loader.setController(chatBox);

        try {
            Node node = loader.load();

            chatPane.getChildren().add(node);
            AnchorPane.setBottomAnchor(node, 0.0);
            AnchorPane.setTopAnchor(node, 0.0);
            AnchorPane.setLeftAnchor(node, 0.0);
            AnchorPane.setRightAnchor(node, 0.0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // add events to clientlistener
        events = new GUIEvents();
        Connections.getListener().getEventList().addEvents(
                events.chatEvent,
                events.getPlayersEvent,
                events.getPlayerStatus,
                events.gameStartEvent);

        btnStartGame.setOnAction(e -> {
            Connections.getListener().sendGameStart();
            btnStartGame.disableProperty().set(true);
        });

        // invite not allowed in local
        if (Connections.getListener().getServerType() == ConnectType.LOCAL) {
            btnInvite.setDisable(true);
        }

        btnInvite.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                FXMLLoader loader2 = new FXMLLoader(getClass().getResource("/InvitePlayerList.fxml"));
                PlayerInviteDialog inviteController = new PlayerInviteDialog();

                loader2.setController(inviteController);

                Stage inviteStage = new Stage();
                inviteStage.initModality(Modality.APPLICATION_MODAL);
                inviteStage.initOwner(chatPane.getScene().getWindow());
                inviteStage.setTitle("Invite players");

                try {
                    inviteStage.setScene(new Scene(loader2.load()));
                    inviteStage.setOnShown(e ->
                            StageUtils.centreStage((Stage)btnInvite.getScene().getWindow(), inviteStage));
                    inviteStage.showAndWait();
                    inviteController.shutdown();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });

        btnStartGame.setDisable(!isHosting);
    }

    public void shutdown() {
        if (events != null) {
            Connections.getListener().getEventList().removeEvents(
                    events.chatEvent,
                    events.getPlayersEvent,
                    events.getPlayerStatus,
                    events.gameStartEvent);
        }
    }

    public static Stage createStage(String ip, String port, boolean isHosting) {
        FXMLLoader loader = new FXMLLoader(
                LobbyController.class.getResource("/LobbyForm.fxml"));
        LobbyController lobbyController = new LobbyController(isHosting);
        loader.setController(lobbyController);

        Stage lobbyStage = new Stage();
        try {
            lobbyStage.setScene(new Scene(loader.load()));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        lobbyStage.setOnCloseRequest(t -> {
            Platform.exit();
            System.exit(0);
        });

        lobbyStage.setTitle(String.format("[User %s] @ %s:%s (Lobby - %s)",
                Connections.playerProperty().get().getName(), ip, port,
                Connections.getListener().getLobbyName()));

        Connections.getListener().requestLobbyDetails();
        return lobbyStage;
    }
}
