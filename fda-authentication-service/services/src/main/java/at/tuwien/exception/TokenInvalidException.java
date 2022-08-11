package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.EXPECTATION_FAILED)
public class TokenInvalidException extends Exception {

    public TokenInvalidException(String msg) {
        super(msg);
    }

    public TokenInvalidException(String msg, Throwable thr) {
        super(msg, thr);
    }

    public TokenInvalidException(Throwable thr) {
        super(thr);
    }
}
