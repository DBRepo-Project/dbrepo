package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_GATEWAY, reason = "error.data.connection")
public class ServiceConnectionException extends Exception {

    public ServiceConnectionException(String msg) {
        super(msg);
    }

    public ServiceConnectionException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public ServiceConnectionException(Throwable thr) {
        super(thr);
    }

}
