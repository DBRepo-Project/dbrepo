package at.ac.tuwien.ifs.dbrepo.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.SERVICE_UNAVAILABLE, reason = "error.auth.invalid")
public class AuthServiceException extends Exception {

    public AuthServiceException(String message) {
        super(message);
    }

    public AuthServiceException(String message, Throwable thr) {
        super(message, thr);
    }

    public AuthServiceException(Throwable thr) {
        super(thr);
    }

}
