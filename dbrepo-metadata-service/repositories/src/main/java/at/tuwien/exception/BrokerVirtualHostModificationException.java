package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_ACCEPTABLE)
public class BrokerVirtualHostModificationException extends Exception {

    public BrokerVirtualHostModificationException(String msg) {
        super(msg);
    }

    public BrokerVirtualHostModificationException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public BrokerVirtualHostModificationException(Throwable thr) {
        super(thr);
    }
}
