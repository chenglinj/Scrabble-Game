package core.message;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.gson.*;
import core.messageType.*;

public interface Message {
    public enum MessageType {
        LOGIN,
        JOIN_LOBBY,
        CHAT,
        PING,
        AGENT_CHANGED,
        GAME_ACTION,
        GAME_VOTE,
        GAME_STATUS_CHANGED,
        QUERY,
        NEW_TURN,
        LOBBY_LIST,
        PLAYER_LIST,
        PLAYER_REQUEST_INVITE,
        SERVER_SEND_INVITES;

        private static final BiMap<MessageType,Class<? extends Message>> classMaps;

        static {
            classMaps = ImmutableBiMap.<MessageType, Class<? extends Message>>builder()
                    .put(LOGIN, MSGLogin.class)
                    .put(JOIN_LOBBY, MSGJoinLobby.class)
                    .put(CHAT, MSGChat.class)
                    .put(PING, MSGPing.class)
                    .put(AGENT_CHANGED, MSGAgentChanged.class)
                    .put(GAME_ACTION, MSGGameAction.class)
                    .put(GAME_VOTE, MSGGameVote.class)
                    .put(GAME_STATUS_CHANGED, MSGGameStatus.class)
                    .put(QUERY, MSGQuery.class)
                    .put(NEW_TURN, MSGNewTurn.class)
                    .put(LOBBY_LIST, MSGLobbyList.class)
                    .put(PLAYER_LIST, MSGPlayerList.class)
                    .put(PLAYER_REQUEST_INVITE, MSGInviteRequest.class)
                    .put(SERVER_SEND_INVITES, MSGInviteNotify.class).build();
        }
    }

    public static <T extends Message> MessageType fromMessageClass(Class<T> cl) {
        return MessageType.classMaps.inverse().get(cl);
    }

    public static Class<? extends Message> getEnumClass(MessageType mType) {
        return MessageType.classMaps.get(mType);
    }

    /***
     * Converts the JSON string into the appropriate Message class, where
     * events will respond to that specific messageType type.
     * @param str
     * @param gson
     * @return
     */
    public static MessageWrapper fromJSON(String str, Gson gson) {
        // TODO: Thanks to https://stackoverflow.com/a/31094365 for the hint

        JsonParser parser = new JsonParser();
        JsonElement element = parser.parse(str);
        JsonObject obj = element.getAsJsonObject();

        // determine message type so we can deserialise it
        String json_msgType = obj.get("msgType").getAsString();
        JsonObject json_msg = obj.getAsJsonObject("msg");

        Class<? extends Message> enumClass = getEnumClass(MessageType.valueOf(json_msgType));

        MessageWrapper recvMsg = new MessageWrapper(
                gson.fromJson(json_msg, enumClass));

        // GSON has issues with deserialising arrays/collections; we must do this manually
        JsonArray timestamps = obj.getAsJsonArray("timeStamps");
        recvMsg.setTimeStamps(timestamps, gson);

        return recvMsg;
    }
}
