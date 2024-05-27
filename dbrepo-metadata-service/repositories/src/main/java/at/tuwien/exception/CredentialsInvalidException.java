package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.FORBIDDEN, reason = "error.user.credentials")
public class CredentialsInvalidException extends Exception {

    public CredentialsInvalidException(String msg) {
        super(msg);
    }

    public CredentialsInvalidException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public CredentialsInvalidException(Throwable thr) {
        super(thr);
    }

}
