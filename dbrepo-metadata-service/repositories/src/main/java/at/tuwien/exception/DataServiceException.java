package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.SERVICE_UNAVAILABLE, reason = "error.data.invalid")
public class DataServiceException extends Exception {

    public DataServiceException(String message) {
        super(message);
    }

    public DataServiceException(String message, Throwable thr) {
        super(message, thr);
    }

    public DataServiceException(Throwable thr) {
        super(thr);
    }

}
