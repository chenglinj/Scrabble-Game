package server;

import com.google.common.base.Predicate;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import core.ConnectType;
import core.ServerListener;
import core.game.Player;
import core.game.Lobby;
import core.message.*;
import core.messageType.*;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.net.Socket;
import java.util.*;

public class ScrabbleServerListener extends ServerListener {

    private BiMap<String, Lobby> lobbyMap;
    private Map<Player, Lobby> playerLobbyMap; // note this is not a bijection, so a BiMap can't be used

    private class ServerEvents {
        // return list of players back to player who sent details to join lobby
        MessageEvent<MSGJoinLobby> playerJoin = new MessageEvent<MSGJoinLobby>() {
            @Override
            public MessageWrapper[] onMsgReceive(MSGJoinLobby recv, Player sender) {
                Lobby lobby = lobbyMap.get(recv.getLobbyName());

                if (lobby == null) { // if lobby hasn't been made yet, make player owner
                    if (recv.getDescription() == null) {
                        return MessageWrapper.prepWraps(new MessageWrapper(
                                new MSGQuery(MSGQuery.QueryType.LOBBY_NOT_EXISTS, true),
                                sender));
                    }

                    lobby = new Lobby(sender, recv.getDescription());

                    synchronized (lobbyMap) {
                        lobbyMap.put(recv.getLobbyName(), lobby);
                    }
                } else if (recv.getDescription() != null && lobby != null) {
                    return MessageWrapper.prepWraps(new MessageWrapper(
                            new MSGQuery(MSGQuery.QueryType.LOBBY_ALREADY_MADE, true),
                            sender));

                } else if (lobby.getGameSession() != null) {
                    // if the lobby has already started game
                    return MessageWrapper.prepWraps(new MessageWrapper(
                            new MSGQuery(MSGQuery.QueryType.GAME_ALREADY_STARTED, true),
                            sender));
                } else {
                    synchronized (lobby) {
                        lobby.addPlayer(sender);
                    }
                }

                synchronized (playerLobbyMap) {
                    playerLobbyMap.put(sender, lobby);
                }

                Message msg1 = new MSGQuery(MSGQuery.QueryType.GAME_ALREADY_STARTED, false);
                MSGAgentChanged msg2 = new MSGAgentChanged(MSGAgentChanged.NewStatus.JOINED, false, sender);
                // TODO: Is there a cleaner way to do this?
                Set<Player> retSend = new HashSet<>(lobby.getPlayers());
                retSend.remove(sender);

                System.out.println("Agents size: " + lobby.getPlayers().size());

                return MessageWrapper.prepWraps(
                        new MessageWrapper(msg1, sender),
                        new MessageWrapper(msg2, retSend));
            }
        };

        // when some player sends chat msg, broadcast it to all other players
        MessageEvent<MSGChat> chatReceived = new MessageEvent<MSGChat>() {
            @Override
            public MessageWrapper[] onMsgReceive(MSGChat recMessage, Player sender) {
                Lobby lobbyChat = playerLobbyMap.get(sender);

                Collection<Player> receivers = null;

                if (lobbyChat == null) { // if in main lobby room
                    List<Player> allOnline = new ArrayList<>();
                    playerLobbyMap.forEach((k, v) -> {
                        if (v == null) allOnline.add(k);
                    });

                    receivers = allOnline;
                } else {
                    receivers = playerLobbyMap.get(sender).getPlayers();
                }

                return MessageWrapper.prepWraps(
                        new MessageWrapper(recMessage, receivers));
            }
        };

        // when Start Game is pressed by the owner (and received by server)
        MessageEvent<MSGGameStatus> startGame = new MessageEvent<MSGGameStatus>() {
            @Override
            public MessageWrapper[] onMsgReceive(MSGGameStatus msg, Player sender) {
                // TODO: This only works with one lobby..
                Lobby lobby = playerLobbyMap.get(sender);
                if (lobby != null && lobby.getOwner().equals(sender)) {
                    lobby.prepareGame();

                    Message sendMsg = new MSGGameStatus(
                            MSGGameStatus.GameStatus.STARTED,
                            lobby.getGameSession());

                    // send back the clients the initial game state
                    return MessageWrapper.prepWraps(
                            new MessageWrapper(sendMsg,
                                    playerLobbyMap.get(sender).getPlayers()));
                }

                return null;
            }
        };


        // when player makes a move, broadcast it to all other users
        MessageEvent<MSGGameAction> playerMakesMove = new MessageEvent<MSGGameAction>() {
            @Override
            public MessageWrapper[] onMsgReceive(MSGGameAction msg, Player sender) {
                Lobby lobby = playerLobbyMap.get(sender);

                // to avoid spam/some cheating
                if (lobby.getGameSession().hasMadeMove() || lobby.getGameSession().getCurrentTurn() != sender)
                    return null;

                lobby.getGameSession().incrementBoard(msg.getMoveLocation(), msg.getLetter());

                if (msg.getMoveLocation() == null || msg.getLetter() == null) { // player skipped turn
                    Player prevPlayer = lobby.getGameSession().getCurrentTurn();
                    lobby.getGameSession().nextTurn(true);

                    if (lobby.getGameSession().allPlayersSkipped()) { // when all players skipped their turn
                        Message msgEndGame = new MSGGameStatus(
                                MSGGameStatus.GameStatus.ENDED,
                                null);

                        return MessageWrapper.prepWraps(new MessageWrapper
                                (msgEndGame, lobby.getPlayers()));
                    } else { // new turn
                        Message msgSkip = new MSGNewTurn(
                                prevPlayer,
                                lobby.getGameSession().getCurrentTurn(),
                                lobby.getGameSession().getScores().get(prevPlayer),
                                true);

                        return MessageWrapper.prepWraps(
                                new MessageWrapper(msgSkip, playerLobbyMap.get(sender).getPlayers())
                        );
                    }
                }

                // otherwise, send this action to all other players, so they can then vote
                return MessageWrapper.prepWraps(new MessageWrapper(msg,
                        playerLobbyMap.get(sender).getPlayers()));
            }
        };


        MessageEvent<MSGGameVote> voteReceived = new MessageEvent<MSGGameVote>() {
            @Override
            public MessageWrapper[] onMsgReceive(MSGGameVote msg, Player sender) {

                // TODO: add logic here
                Lobby lobby = playerLobbyMap.get(sender);
                lobby.getGameSession().addVote(msg.isAccepted(), msg.getOrient());

                if (lobby.getGameSession().allVoted()) {
                    // all players voted, move onto next player
                    Player prevPlayer = lobby.getGameSession().getCurrentTurn();
                    lobby.getGameSession().nextTurn(false);

                    Message msgNextTurn = new MSGNewTurn(
                            prevPlayer,
                            lobby.getGameSession().getCurrentTurn(),
                            lobby.getGameSession().getScores().get(prevPlayer),
                            false);

                    // if board is full, end the game
                    if (lobby.getGameSession().isBoardFull()) {
                        Message msgEndGame = new MSGGameStatus(MSGGameStatus.GameStatus.ENDED, null);

                        // TODO: Better structure protocol
                        // send additional message to end game
                        return MessageWrapper.prepWraps(
                                new MessageWrapper(msgNextTurn, lobby.getPlayers()),
                                new MessageWrapper(msgEndGame, lobby.getPlayers()));
                    } else {
                        return MessageWrapper.prepWraps(
                                new MessageWrapper(msgNextTurn, lobby.getPlayers()));
                    }
                }

                return null;
            }
        };

        // ping back to the user
        MessageEvent<MSGPing> pingReceived =  new MessageEvent<MSGPing>() {
            @Override
            public MessageWrapper[] onMsgReceive(MSGPing msg, Player sender) {
                return MessageWrapper.prepWraps(new MessageWrapper(msg, sender));
            }
        };

        MessageEvent<MSGQuery> requestPlayers = new MessageEvent<MSGQuery>() {
            @Override
            public MessageWrapper[] onMsgReceive(MSGQuery recMessage, Player sender) {
                if (recMessage.getQueryType() != MSGQuery.QueryType.GET_PLAYER_LIST)
                    return null;

                Lobby lobby = playerLobbyMap.get(sender);
                Message msg = new MSGAgentChanged(
                        MSGAgentChanged.NewStatus.JOINED,
                        lobby.getPlayers());

                return MessageWrapper.prepWraps(new MessageWrapper(msg, sender));
            }
        };

        MessageEvent<MSGQuery> requestLobbyList = new MessageEvent<MSGQuery>() {
            @Override
            public MessageWrapper[] onMsgReceive(MSGQuery recMessage, Player sender) {
                if (recMessage.getQueryType() != MSGQuery.QueryType.GET_LOBBY_LIST)
                    return null;

                BiMap<String, Lobby> filtered =
                        Maps.filterEntries(lobbyMap, new Predicate<Map.Entry<String, Lobby>>() {
                    @Override
                    public boolean apply(Map.@Nullable Entry<String, Lobby> input) {
                        return input.getValue().getGameSession() == null;
                    }
                });

                Message msg = new MSGLobbyList(filtered);
                return MessageWrapper.prepWraps(new MessageWrapper(msg, sender));
            }
        };

        MessageEvent<MSGQuery> requestOnlinePlayers = new MessageEvent<MSGQuery>() {
            @Override
            public MessageWrapper[] onMsgReceive(MSGQuery recMessage, Player sender) {
                if (recMessage.getQueryType() != MSGQuery.QueryType.GET_ALL_ONLINE_PLAYERS)
                    return null;

                // add all players not in a lobby
                List<String> allOnline = new ArrayList<>();
                playerLobbyMap.forEach((k, v) -> {
                    if (v == null) allOnline.add(k.toString());
                });

                Message msg = new MSGPlayerList(allOnline);
                return MessageWrapper.prepWraps(new MessageWrapper(msg, sender));
            }
        };

        MessageEvent<MSGInviteRequest> requestInvite = new MessageEvent<MSGInviteRequest>() {
            @Override
            public MessageWrapper[] onMsgReceive(MSGInviteRequest recMessage, Player sender) {
                // ensure player is still in lobby
                if (playerLobbyMap.get(sender) == null)
                    return null;

                Set<Player> playersToSend = new HashSet<>();

                for (String s : recMessage.getPlayers()) {
                    Player playerToSend = new Player(s);

                    // ensure player isn't in a lobby
                    if (playerLobbyMap.get(playerToSend) == null) {
                        playersToSend.add(playerToSend);
                    }
                }

                Message msg = new MSGInviteNotify(
                        lobbyMap.inverse().get(playerLobbyMap.get(sender)));

                return MessageWrapper.prepWraps(new MessageWrapper(
                        msg, playersToSend));
            }
        };
    }

    public ScrabbleServerListener(String name, ConnectType connectType) {
        super(name, connectType);

        if (connectType == ConnectType.INTERNET) {

        }
    }

    @Override
    protected void reset() {
        super.reset();
        lobbyMap = Maps.synchronizedBiMap(HashBiMap.create());
        playerLobbyMap = new HashMap<>(); // TODO: concurrent hash map
    }

    @Override
    protected void onUserConnect(Socket s) {
    }

    @Override
    protected void prepareEvents() {
        // add events
        ServerEvents events = new ServerEvents();
        eventList.addEvents(
                events.chatReceived,
                events.pingReceived,
                events.playerJoin,
                events.startGame,
                events.voteReceived,
                events.playerMakesMove,
                events.requestPlayers,
                events.requestLobbyList,
                events.requestOnlinePlayers,
                events.requestInvite
        );
    }

    @Override
    protected boolean onMessageReceived(MessageWrapper msgRec, Socket s) throws IOException {
        if (connections.get(s) == null) { // if player hasn't been authenticated yet

            if (msgRec.getMessageType() == Message.MessageType.LOGIN) {
                Player player = (Player)((MSGLogin)msgRec.getMessage()).getPlayer();
                boolean is_unique = false;

                synchronized (connections) {
                    // register user associated with socket
                    if (!connections.values().contains(player)) {
                        connections.put(s, player);
                        is_unique = true;
                    }
                }

                // send back message if their name is unique to the server
                sendMessage(new MSGQuery(MSGQuery.QueryType.AUTHENTICATED, is_unique, getServerType()), s);

                if (is_unique) {
                    playerLobbyMap.put(player, null);

                    if (getServerType() == ConnectType.INTERNET) {
                        Timer timer_welcome = new Timer();

                        timer_welcome.schedule(
                                new java.util.TimerTask() {
                                    @Override
                                    public void run() {
                                        try {
                                            sendMessage(new MSGChat(
                                                    String.format("Welcome user %s to %s server!\n" +
                                                            "Please keep in " +
                                                            "mind this is a public chatroom.",
                                                            player.getName(), getName()),
                                                    new Player("Server"), null), s);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }

                                        timer_welcome.cancel();
                                        timer_welcome.purge();
                                    }
                                },
                                750
                        );
                    }

                }

                return is_unique;
            }
        }

        return true;
    }

    @Override
    protected void onUserDisconnect(Player p) {
        System.out.println("From server disconnect: " + p);
        if (p == null)
            return;

        Lobby lobby = playerLobbyMap.get(p);
        System.out.println("Lobby: " + lobby + "\t");

        if (lobby != null) {
            synchronized (lobby.getPlayers()) {
                if (lobby != null)
                    lobby.getPlayers().remove(p);
            }
        }

        synchronized (playerLobbyMap) {
            playerLobbyMap.remove(p);
        }

        if (lobby != null && lobby.getPlayers().isEmpty()) {
            synchronized (lobbyMap) {
                if (lobby != null && lobby.getPlayers().isEmpty())
                    lobbyMap.inverse().remove(lobby);
            }
        }

        if (lobby != null) {
            sendMessage(new MessageWrapper(
                    new MSGAgentChanged(
                            MSGAgentChanged.NewStatus.DISCONNECTED,
                            lobby.getOwner().equals(p), p), lobby.getPlayers()));
        }
    }
}
