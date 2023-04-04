package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.EXPECTATION_FAILED, reason = "Container not found")
public class ContainerUnauthorizedException extends Exception {

    public ContainerUnauthorizedException(String message) {
        super(message);
    }

    public ContainerUnauthorizedException(String message, Throwable thr) {
        super(message, thr);
    }

    public ContainerUnauthorizedException(Throwable thr) {
        super(thr);
    }

}
