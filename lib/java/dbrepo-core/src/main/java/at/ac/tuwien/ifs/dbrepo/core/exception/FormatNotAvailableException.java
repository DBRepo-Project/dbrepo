package at.ac.tuwien.ifs.dbrepo.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_ACCEPTABLE, reason = "error.identifier.format")
public class FormatNotAvailableException extends Exception {

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
