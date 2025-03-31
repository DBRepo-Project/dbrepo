package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.SERVICE_UNAVAILABLE)
public class RemoteUnavailableException extends Exception {

    public RemoteUnavailableException(String message) {
        super(message);
    }

    public RemoteUnavailableException(String message, Throwable thr) {
        super(message, thr);
    }

    public RemoteUnavailableException(Throwable thr) {
        super(thr);
    }

}
