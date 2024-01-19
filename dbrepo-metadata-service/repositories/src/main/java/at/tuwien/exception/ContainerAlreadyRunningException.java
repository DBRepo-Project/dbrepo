package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.CONFLICT)
public class ContainerAlreadyRunningException extends Exception {

    public ContainerAlreadyRunningException(String message) {
        super(message);
    }

    public ContainerAlreadyRunningException(String message, Throwable thr) {
        super(message, thr);
    }

    public ContainerAlreadyRunningException(Throwable thr) {
        super(thr);
    }

}
