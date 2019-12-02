package client.listeners;

import client.ClientMain;
import client.Connections;
import client.util.StageUtils;
import core.ClientListener;
import core.ConnectType;
import core.game.Player;
import core.game.GameRules;
import core.message.Message;
import core.message.MessageEvent;
import core.message.MessageWrapper;
import core.messageType.*;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.awt.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Collection;
import java.util.Observable;
import java.util.Optional;

final public class ScrabbleClientListener extends ClientListener {
    private String lobbyName;
    private BooleanProperty inLobby;

    private DoubleProperty pingMS; // ping in milliseconds (ms)
    private ConnectType serverType;

    public ScrabbleClientListener(String name) {
        super(name);
        pingMS = new SimpleDoubleProperty();
        inLobby = new SimpleBooleanProperty(false);
    }

    public ConnectType getServerType() { return serverType; }

    public String getLobbyName() {
        return lobbyName;
    }

    public DoubleProperty pingMSProperty() {
        return pingMS;
    }

    public void sendGameVote(GameRules.Orientation orient, boolean accepted) {
        try {
            sendMessage(new MSGGameVote(orient, accepted));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendGameMove(Point location, Character letter) {
        try {
            sendMessage(new MSGGameAction(location, letter));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendGamePass() {
        sendGameMove(null, null);
    }

    public void sendChatMessage(String txt) {
        try {
            sendMessage(new MSGChat(txt, Connections.playerProperty().get(), lobbyName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendGameStart() {
        try {
            // TODO: Host starts the game lol..
            sendMessage(new MSGGameStatus(MSGGameStatus.GameStatus.STARTED, null));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onUserConnect(Socket s) throws IOException { }

    @Override
    protected void prepareEvents() {
        eventList.addEvents(
                // when user gets accepted into a lobby
                new MessageEvent<MSGQuery>() {
                    @Override
                    public MessageWrapper[] onMsgReceive(MSGQuery recMessage, Player sender) {
                        switch (recMessage.getQueryType()) {
                            case GAME_ALREADY_STARTED:
                                if (recMessage.getValue()) {
                                    showDialogWarn("The lobby has already started a game.");
                                } else {
                                    inLobby.set(true);
                                }
                                break;
                            case LOBBY_NOT_EXISTS:
                                showDialogWarn("The lobby has already been closed down.");
                                break;
                            case LOBBY_ALREADY_MADE:
                                showDialogWarn("The lobby has already been created by another player.");
                                break;
                            default:
                                break;
                        }

                        return null;
                    }
                },

                // invitation received from server
                new MessageEvent<MSGInviteNotify>() {
                    @Override
                    public MessageWrapper[] onMsgReceive(MSGInviteNotify recMessage, Player sender) {
                        // make sure player isn't in lobby
                        if (inLobby.get()) return null;

                        Platform.runLater(() -> {
                            ButtonType yesBtn = new ButtonType("Accept", ButtonBar.ButtonData.OK_DONE);
                            ButtonType noBtn = new ButtonType("Decline", ButtonBar.ButtonData.CANCEL_CLOSE);
                            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                                    "You have received an invitation to enter into the lobby: " +
                                    recMessage.getLobbyName() + ".",
                                    yesBtn, noBtn);

                            alert.setTitle("Invite");
                            Optional<ButtonType> result = alert.showAndWait();

                            // if person accepts the invitation
                            if (result.orElse(noBtn) == yesBtn) {
                                joinLobby(recMessage.getLobbyName());
                            }
                        });

                        return null;
                    }
                }
        );

    }

    private void showDialogWarn(String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING, msg);
            alert.showAndWait();
        });
    }

    @Override
    protected boolean onMessageReceived(MessageWrapper msgRec, Socket s) {
        if (msgRec.getMessageType() == Message.MessageType.PING &&
                msgRec.getTimeStamps().size() > 1) {
            pingMS.set(Math.abs(msgRec.getTimeStamps().get(0) - System.nanoTime())/Math.pow(10, 6));
            System.out.println(pingMS);
        }

        return true;
    }

    @Override
    protected void onUserDisconnect(Player p) {
        // TODO: This is a simplification.
        Platform.runLater(() ->
                ClientMain.endApp(
                "The server (or host) you were connected to has closed down. " +
                            "The app will now exit."));
    }

    @Override
    public void onAuthenticate() throws Exception {
        // sender player's username; server checks if its unique
        sendMessage(new MSGLogin(Connections.playerProperty().get()));

        // TODO: potential code dups, also dodgy code
        DataInputStream in = new DataInputStream(socket.getInputStream());

        // wait response from client.listeners (ignore pings)
        while (true) {
            String read = in.readUTF();
            MessageWrapper msgRec = Message.fromJSON(read, gson);

            if (msgRec.getMessageType() == Message.MessageType.QUERY) {
                MSGQuery qmsg = (MSGQuery)msgRec.getMessage();

                if (qmsg.getServerType() != null)
                    serverType = qmsg.getServerType();

                switch (qmsg.getQueryType()) {
                    case GAME_ALREADY_STARTED:
                        if (qmsg.getValue() == true)
                            throw new GameInProgressException();
                        return;
                    case AUTHENTICATED:
                        if (!qmsg.getValue())
                            throw new NonUniqueNameException();
                    default:
                        if (serverType == ConnectType.LOCAL) {
                            if (Connections.getServer().hasStarted())
                                this.createLobby(lobbyName, null);
                            else
                                this.joinLobby(lobbyName);
                        } else {
                            return;
                        }
                }
            }
        }
    }

    public void requestLobbyDetails() {
        try {
            sendMessage(new MSGQuery(MSGQuery.QueryType.GET_PLAYER_LIST, true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void requestAllLobbies() {
        try {
            sendMessage(new MSGQuery(MSGQuery.QueryType.GET_LOBBY_LIST, true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createLobby(String lobbyName, String descript) {
        try {
            this.lobbyName = lobbyName;
            sendMessage(new MSGJoinLobby(lobbyName, descript == null ? "" : descript));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void joinLobby(String lobbyName) {
        try {
            this.lobbyName = lobbyName;
            sendMessage(new MSGJoinLobby(lobbyName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public BooleanProperty inLobbyProperty() { return inLobby; }

    public void requestOnlinePlayers() {
        try {
            sendMessage(new MSGQuery(MSGQuery.QueryType.GET_ALL_ONLINE_PLAYERS, true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendInvites(Collection<String> players) {
        try {
            sendMessage(new MSGInviteRequest(players));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
