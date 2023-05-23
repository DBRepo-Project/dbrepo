package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND)
public class SemanticConceptNotFoundException extends Exception {

    public SemanticConceptNotFoundException(String msg) {
        super(msg);
    }

    public SemanticConceptNotFoundException(String msg, Throwable thr) {
        super(msg, thr);
    }

    public SemanticConceptNotFoundException(Throwable thr) {
        super(thr);
    }

}
