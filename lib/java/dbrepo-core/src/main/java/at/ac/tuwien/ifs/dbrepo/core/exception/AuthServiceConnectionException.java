package at.ac.tuwien.ifs.dbrepo.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_GATEWAY, reason = "error.auth.connection")
public class AuthServiceConnectionException extends Exception {

    public AuthServiceConnectionException(String msg) {
        super(msg);
    }

    public AuthServiceConnectionException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public AuthServiceConnectionException(Throwable thr) {
        super(thr);
    }

}
