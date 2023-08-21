package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.EXPECTATION_FAILED)
public class UserEmailAlreadyExistsException extends Exception {

    public UserEmailAlreadyExistsException(String message) {
        super(message);
    }

    public UserEmailAlreadyExistsException(String message, Throwable thr) {
        super(message, thr);
    }

    public UserEmailAlreadyExistsException(Throwable thr) {
        super(thr);
    }

}
