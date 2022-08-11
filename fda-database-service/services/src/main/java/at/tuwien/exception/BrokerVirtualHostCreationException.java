package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_ACCEPTABLE)
public class BrokerVirtualHostCreationException extends Exception {

    public BrokerVirtualHostCreationException(String msg) {
        super(msg);
    }

    public BrokerVirtualHostCreationException(String msg, Throwable thr) {
        super(msg, thr);
    }

    public BrokerVirtualHostCreationException(Throwable thr) {
        super(thr);
    }
}
