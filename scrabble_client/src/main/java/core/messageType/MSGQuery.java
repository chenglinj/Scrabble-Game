package core.messageType;

import core.ConnectType;
import core.message.Message;

public class MSGQuery implements Message {
    public enum QueryType {
        AUTHENTICATED,
        GAME_ALREADY_STARTED,
        GET_PLAYER_LIST,
        GET_LOBBY_LIST,
        LOBBY_ALREADY_MADE,
        GET_ALL_ONLINE_PLAYERS,
        LOBBY_NOT_EXISTS
    }

    private QueryType queryType;
    private boolean value;
    private ConnectType serverType;

    public MSGQuery(QueryType queryType, boolean value) {
        this.queryType = queryType;
        this.value = value;
    }

    public MSGQuery(QueryType queryType, boolean value, ConnectType type) {
        this(queryType, value);
        this.serverType = type;
    }


    public QueryType getQueryType() {
        return queryType;
    }

    public boolean getValue() { return value; }

    public ConnectType getServerType() {
        return serverType;
    }
}
