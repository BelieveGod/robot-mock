package cn.nannar.robotmock.fx.event;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * @author LTJ
 * @date 2023/4/23
 */
public class ProgressEvent extends Event {
    public static final EventType<ProgressEvent> ANY = new EventType<>(Event.ANY,"ANY");
    public static final EventType<ProgressEvent> LOADING = new EventType<>(ANY, "LOADING");
    public static final EventType<ProgressEvent> LOADED = new EventType<>(ANY, "LOADED");


    public ProgressEvent(EventType<? extends Event> eventType) {
        super(eventType);
    }

    public ProgressEvent(Object source, EventTarget target, EventType<? extends Event> eventType) {
        super(source, target, eventType);
    }
}
