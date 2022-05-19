package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.EXPECTATION_FAILED)
public class UserEmailFailedException extends Exception {

    public UserEmailFailedException(String msg) {
        super(msg);
    }

    public UserEmailFailedException(String msg, Throwable thr) {
        super(msg, thr);
    }

    public UserEmailFailedException(Throwable thr) {
        super(thr);
    }
}
