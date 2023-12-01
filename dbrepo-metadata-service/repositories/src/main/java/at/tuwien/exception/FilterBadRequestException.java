package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class FilterBadRequestException extends Exception {

    public FilterBadRequestException(String msg) {
        super(msg);
    }

    public FilterBadRequestException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public FilterBadRequestException(Throwable thr) {
        super(thr);
    }

}
