package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.CONFLICT)
public class ContainerAlreadyStoppedException extends Exception {

    public ContainerAlreadyStoppedException(String message) {
        super(message);
    }

    public ContainerAlreadyStoppedException(String message, Throwable thr) {
        super(message, thr);
    }

    public ContainerAlreadyStoppedException(Throwable thr) {
        super(thr);
    }

}
