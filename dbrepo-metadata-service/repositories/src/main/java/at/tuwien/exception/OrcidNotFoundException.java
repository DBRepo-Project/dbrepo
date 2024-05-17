package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "error.orcid.missing")
public class OrcidNotFoundException extends Exception {

    public OrcidNotFoundException(String msg) {
        super(msg);
    }

    public OrcidNotFoundException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public OrcidNotFoundException(Throwable thr) {
        super(thr);
    }

}
