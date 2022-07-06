package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.I_AM_A_TEAPOT)
public class UserEmailNotVerifiedException extends Exception {

    public UserEmailNotVerifiedException(String msg) {
        super(msg);
    }

    public UserEmailNotVerifiedException(String msg, Throwable thr) {
        super(msg, thr);
    }

    public UserEmailNotVerifiedException(Throwable thr) {
        super(thr);
    }
}
