package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.SERVICE_UNAVAILABLE, reason = "error.metadata.invalid")
public class ServiceException extends Exception {

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String message, Throwable thr) {
        super(message, thr);
    }

    public ServiceException(Throwable thr) {
        super(thr);
    }

}
