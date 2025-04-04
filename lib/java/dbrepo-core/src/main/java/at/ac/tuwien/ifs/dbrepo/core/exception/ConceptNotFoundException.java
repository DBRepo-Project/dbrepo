package at.ac.tuwien.ifs.dbrepo.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "error.concept.missing")
public class ConceptNotFoundException extends Exception {

    public ConceptNotFoundException(String msg) {
        super(msg);
    }

    public ConceptNotFoundException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public ConceptNotFoundException(Throwable thr) {
        super(thr);
    }

}
