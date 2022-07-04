package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.FAILED_DEPENDENCY)
public class UserEmailAlreadyVerifiedException extends Exception {

    public UserEmailAlreadyVerifiedException(String msg) {
        super(msg);
    }

    public UserEmailAlreadyVerifiedException(String msg, Throwable thr) {
        super(msg, thr);
    }

    public UserEmailAlreadyVerifiedException(Throwable thr) {
        super(thr);
    }
}
