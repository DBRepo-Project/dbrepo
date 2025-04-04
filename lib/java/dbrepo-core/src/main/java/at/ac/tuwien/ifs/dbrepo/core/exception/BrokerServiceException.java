package at.ac.tuwien.ifs.dbrepo.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.SERVICE_UNAVAILABLE, reason = "error.broker.invalid")
public class BrokerServiceException extends Exception {

    public BrokerServiceException(String message) {
        super(message);
    }

    public BrokerServiceException(String message, Throwable thr) {
        super(message, thr);
    }

    public BrokerServiceException(Throwable thr) {
        super(thr);
    }

}
