package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.IOException;

@ResponseStatus(code = HttpStatus.NOT_ACCEPTABLE)
public class FormatNotAvailableException extends IOException {

    public FormatNotAvailableException(String msg) {
        super(msg);
    }

    public FormatNotAvailableException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public FormatNotAvailableException(Throwable thr) {
        super(thr);
    }

}
