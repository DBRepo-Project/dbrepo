package at.ac.tuwien.ifs.dbrepo.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.SERVICE_UNAVAILABLE, reason = "error.external.invalid")
public class ExternalServiceException extends Exception {

    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable thr) {
        super(message, thr);
    }

    public ExternalServiceException(Throwable thr) {
        super(thr);
    }

}
