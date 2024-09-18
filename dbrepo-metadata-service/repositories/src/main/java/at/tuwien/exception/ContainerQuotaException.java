package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.LOCKED, reason = "error.container.quota")
public class ContainerQuotaException extends Exception {

    public ContainerQuotaException(String message) {
        super(message);
    }

    public ContainerQuotaException(String message, Throwable thr) {
        super(message, thr);
    }

    public ContainerQuotaException(Throwable thr) {
        super(thr);
    }

}
