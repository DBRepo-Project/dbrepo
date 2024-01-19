package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND)
public class OntologyNotFoundException extends Exception {

    public OntologyNotFoundException(String msg) {
        super(msg);
    }

    public OntologyNotFoundException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public OntologyNotFoundException(Throwable thr) {
        super(thr);
    }

}
