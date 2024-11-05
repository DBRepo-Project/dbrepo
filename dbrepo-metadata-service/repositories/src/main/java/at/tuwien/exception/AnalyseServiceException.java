package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.SERVICE_UNAVAILABLE, reason = "error.analyse.invalid")
public class AnalyseServiceException extends Exception {

    public AnalyseServiceException(String message) {
        super(message);
    }

    public AnalyseServiceException(String message, Throwable thr) {
        super(message, thr);
    }

    public AnalyseServiceException(Throwable thr) {
        super(thr);
    }

}
