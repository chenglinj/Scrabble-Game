package core.message;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import core.game.Player;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

// TODO: Handle thread-safety issues with add/removeEvents ??
public class EventMessageList {
    private final Multimap<Message.MessageType, MessageEvent> events;

    public EventMessageList() {
        events = Multimaps.synchronizedMultimap(HashMultimap.create());
    }

    public Collection<MessageWrapper> fireEvent(Message msg, Message.MessageType msgType,
                                          Player sender) {
        final Collection<MessageEvent> eventsToFire = events.get(msgType);
        final List<MessageWrapper> msgs = new ArrayList<>(eventsToFire.size());

        synchronized (events) {
            //System.out.println("Iteration start: " + events.size());
            for (MessageEvent e : eventsToFire) {
                MessageWrapper[] msgWraps = e.onMsgReceive(msg, sender);

                if (msgWraps != null)
                    msgs.addAll(Arrays.asList(msgWraps));

                //System.out.println("Complete an iteration: " + events.size());
            }
            //System.out.println("Iteration end" + events.size());
        }

        return msgs;
    }

    public void addEvents(MessageEvent... eventList) {
        synchronized (events) {
            for (MessageEvent e : eventList) {
                Type generic_type = ((ParameterizedType)
                        e.getClass().getGenericInterfaces()[0])
                        .getActualTypeArguments()[0];
                Class<? extends Message> t = (Class<? extends Message>)generic_type;

                if (events.put(Message.fromMessageClass(t), e)) {
                    System.out.println("Added event, size: " + events.size());
                }
            }
        }
    }

    public void removeEvents(MessageEvent... eventList) {
        synchronized (events) {
            for (MessageEvent e : eventList) {
                Type generic_type = ((ParameterizedType)
                        e.getClass().getGenericInterfaces()[0])
                        .getActualTypeArguments()[0];
                Class<? extends Message> t = (Class<? extends Message>)generic_type;

                if (events.remove(Message.fromMessageClass(t), e)) {
                    System.out.println("Removed event, size: " + events.size());
                }
            }
        }
    }
}
