package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.GONE)
public class ContainerAlreadyRemovedException extends Exception {

    public ContainerAlreadyRemovedException(String message) {
        super(message);
    }

    public ContainerAlreadyRemovedException(String message, Throwable thr) {
        super(message, thr);
    }

    public ContainerAlreadyRemovedException(Throwable thr) {
        super(thr);
    }

}
