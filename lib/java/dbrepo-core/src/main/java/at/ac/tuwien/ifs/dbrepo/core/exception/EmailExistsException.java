package at.ac.tuwien.ifs.dbrepo.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.EXPECTATION_FAILED, reason = "error.user.email-exists")
public class EmailExistsException extends Exception {

    public EmailExistsException(String message) {
        super(message);
    }

    public EmailExistsException(String message, Throwable thr) {
        super(message, thr);
    }

    public EmailExistsException(Throwable thr) {
        super(thr);
    }

}
