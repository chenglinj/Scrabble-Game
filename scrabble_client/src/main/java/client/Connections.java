package client;

import client.listeners.ScrabbleClientListener;
import core.ConnectType;
import server.ScrabbleServerListener;
import core.game.Player;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class Connections {
    private static ScrabbleClientListener listener;
    private static ScrabbleServerListener server;
    private static ObjectProperty<Player> playerProperty;

    static {
        playerProperty = new SimpleObjectProperty<>(new Player(null));
    }

    /** Returns a connection that is listening to the server. */
    public static ScrabbleClientListener getListener() {
        if (listener == null) {
            listener = new ScrabbleClientListener(playerProperty.get().getName());
        }

        return listener;
    }

    /** Returns a connection represents the server/host. */
    public static ScrabbleServerListener getServer() {
        if (server == null) {
            server = new ScrabbleServerListener("Server", ConnectType.LOCAL);
        }

        return server;
    }

    public static ObjectProperty<Player> playerProperty() {
        return playerProperty;
    }
}
