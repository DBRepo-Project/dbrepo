package at.ac.tuwien.ifs.dbrepo.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_GATEWAY, reason = "error.broker.connection")
public class BrokerServiceConnectionException extends Exception {

    public BrokerServiceConnectionException(String msg) {
        super(msg);
    }

    public BrokerServiceConnectionException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public BrokerServiceConnectionException(Throwable thr) {
        super(thr);
    }

}
