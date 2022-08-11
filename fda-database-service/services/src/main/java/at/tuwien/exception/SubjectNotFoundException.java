package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Subject not found")
public class SubjectNotFoundException extends Exception {

    public SubjectNotFoundException(String message) {
        super(message);
    }

    public SubjectNotFoundException(String message, Throwable thr) {
        super(message, thr);
    }

    public SubjectNotFoundException(Throwable thr) {
        super(thr);
    }

}
