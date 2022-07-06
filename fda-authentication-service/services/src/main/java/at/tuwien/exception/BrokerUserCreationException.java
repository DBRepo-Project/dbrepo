package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_ACCEPTABLE)
public class BrokerUserCreationException extends Exception {

    public BrokerUserCreationException(String msg) {
        super(msg);
    }

    public BrokerUserCreationException(String msg, Throwable thr) {
        super(msg, thr);
    }

    public BrokerUserCreationException(Throwable thr) {
        super(thr);
    }
}
