package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.UNAUTHORIZED)
public class TokenRevokedException extends Exception {

    public TokenRevokedException(String msg) {
        super(msg);
    }

    public TokenRevokedException(String msg, Throwable thr) {
        super(msg, thr);
    }

    public TokenRevokedException(Throwable thr) {
        super(thr);
    }
}
