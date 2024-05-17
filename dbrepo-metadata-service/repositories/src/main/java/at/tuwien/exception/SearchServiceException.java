package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.SERVICE_UNAVAILABLE, reason = "error.search.invalid")
public class SearchServiceException extends Exception {

    public SearchServiceException(String message) {
        super(message);
    }

    public SearchServiceException(String message, Throwable thr) {
        super(message, thr);
    }

    public SearchServiceException(Throwable thr) {
        super(thr);
    }

}
