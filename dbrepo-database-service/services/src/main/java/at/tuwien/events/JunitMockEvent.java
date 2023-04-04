package at.tuwien.events;

import org.springframework.context.ApplicationEvent;

public class JunitMockEvent extends ApplicationEvent {

    private final String message;

    public JunitMockEvent(Object source, String message) {
        super(source);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
