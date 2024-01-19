package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.SERVICE_UNAVAILABLE)
public class DatabaseConnectionException extends Exception {

    public DatabaseConnectionException(String msg) {
        super(msg);
    }

    public DatabaseConnectionException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public DatabaseConnectionException(Throwable thr) {
        super(thr);
    }

}
