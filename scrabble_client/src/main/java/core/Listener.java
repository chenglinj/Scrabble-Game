package core;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.google.gson.*;
import core.game.Player;
import core.message.EventMessageList;
import core.message.Message;
import core.message.MessageWrapper;
import core.messageType.MSGPing;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.*;

public abstract class Listener {
    private static final int HEARTBEAT_PERIOD = 10000; // in ms
    private String listenerName;

    // TODO: This solves the issue of iterating through the list, but we must make
    // sure only ONE thread can modify it.
    protected volatile EventMessageList eventList;
    protected final Gson gson;
    protected BiMap<Socket, Player> connections;

    protected abstract void onUserConnect(Socket s) throws IOException;
    protected abstract void prepareEvents();
    protected abstract boolean onMessageReceived(MessageWrapper msgRec, Socket s) throws IOException;
    protected abstract void onUserDisconnect(Player p);

    public Listener(String name) {
        this.listenerName = name;

        eventList = new EventMessageList();
        gson = new GsonBuilder()
                .enableComplexMapKeySerialization()
                .addSerializationExclusionStrategy(new ExclusionStrategy() { // refer to https://stackoverflow.com/a/13637572
            @Override
            public boolean shouldSkipField(FieldAttributes f) {
                return f.getDeclaringClass().equals(Observable.class);
            }

            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }
        }).create();

        reset();
        prepareEvents();
    }

    public String getName() {
        return listenerName;
    }

    public EventMessageList getEventList() {
        return eventList;
    }
    // reset variables
    protected void reset() {
        connections = Maps.synchronizedBiMap(HashBiMap.create());
    }


    /***
     * For listening to messages from a client/player.
     * Should be handled by separate threads.
     * @param client
     */
    void run_socket(Socket client, Thread heartbeat_t) {
        try {
            /**
            // TODO: Adding this line causes issues with ScrabbleServerListener [FIX]
            synchronized (connections) {
                connections.put(client, null);
            }
            **/

            DataInputStream in = new DataInputStream(client.getInputStream());

            while (true) {
                String read = in.readUTF();

                //System.out.println("(Preparse from " + listenerName + ":)\t" + read);
                MessageWrapper msgRec = Message.fromJSON(read, gson);

                // TODO: debug
                if (msgRec.getMessageType() != Message.MessageType.PING)
                    System.out.println(String.format("[%s gets from %s]:\t" + read,
                            listenerName, connections.get(client)));


                if (!onMessageReceived(msgRec, client))
                    continue;

                Collection<MessageWrapper> msgsToSend = eventList.fireEvent(
                        msgRec.getMessage(), msgRec.getMessageType(),
                        connections.get(client));

                // add timestamp to each message to send
                msgRec.appendRecentTime();
                for (MessageWrapper wrap : msgsToSend) {
                    wrap.setTimeStamps(msgRec.getTimeStamps());
                }

                processMessages(msgsToSend);
            }
        } catch (IOException e) {
            // client disconnect (most likely)\
            System.out.println("Error coming from: " + listenerName);

            triggerDisconnect(client);
        }
    }

    void run_heartbeat(Socket client) {
        // TODO: Document something about write error while using TCP.
        try {
            while (true) {
                sendMessage(new MSGPing(), client);
                Thread.sleep(HEARTBEAT_PERIOD);
            }
        }  catch (IOException | InterruptedException e) {
            System.out.println("Error coming from: " + listenerName + "\t[HEARTBEAT] ");
        }
    }

    // TODO: A VERY BAD PROCESSOR
    // TODO: A VERY BAD BROADCASTER
    protected void processMessages(Collection<MessageWrapper> msgList) {
        if (msgList == null || msgList.contains(null))
            return;

        for (MessageWrapper smsg : msgList) {
            sendMessage(smsg);
        }
    }

    /***
     * Removes player from connection list and signals to all other
     * player of the disconnect.
     * @param s Socket of player.
     */
    protected void triggerDisconnect(Socket s) {
        Player disconnectedPlayer = connections.get(s);
        System.out.println("Disconnected called: " + disconnectedPlayer);

        synchronized (connections) {
            connections.remove(s);
            System.out.println("REMOVED SOMETHING");
        }

        onUserDisconnect(disconnectedPlayer);
    }

    /***
     * Send a message via a socket.
     */
    void sendMessage(Message msg, Socket s) throws IOException {
        MessageWrapper smsg = new MessageWrapper(msg);
        smsg.appendRecentTime();

        String json = gson.toJson(smsg);

        if (smsg.getMessageType() != Message.MessageType.PING)
            System.out.println("[" + listenerName + " sends:]\t" + json);

        DataOutputStream out = new DataOutputStream(s.getOutputStream());
        out.writeUTF(json);
    }

    /***
     * Send a message by a MessageWrapper, which contains data on which
     * recipients to send to.
     */
    protected void sendMessage(MessageWrapper smsg) {
        if (smsg == null)
            return;

        for (Player p : smsg.getSendTo()) {
            try {
                Socket socket_send = connections.inverse().get(p);

                if (socket_send == null)
                    continue;

                // send message to client's socket
                String json = gson.toJson(smsg);

                if (smsg.getMessageType() != Message.MessageType.PING)
                    System.out.println("[" + listenerName + " sends to " + p + "]:\t" + json);

                DataOutputStream out = new DataOutputStream(socket_send.getOutputStream());
                out.writeUTF(json);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
