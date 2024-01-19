package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.METHOD_NOT_ALLOWED)
public class BrokerVirtualHostGrantException extends Exception {

    public BrokerVirtualHostGrantException(String msg) {
        super(msg);
    }

    public BrokerVirtualHostGrantException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public BrokerVirtualHostGrantException(Throwable thr) {
        super(thr);
    }
}
