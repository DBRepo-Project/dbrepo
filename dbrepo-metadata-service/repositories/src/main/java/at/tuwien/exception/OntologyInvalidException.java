package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
public class OntologyInvalidException extends Exception {

    public OntologyInvalidException(String msg) {
        super(msg);
    }

    public OntologyInvalidException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public OntologyInvalidException(Throwable thr) {
        super(thr);
    }

}
