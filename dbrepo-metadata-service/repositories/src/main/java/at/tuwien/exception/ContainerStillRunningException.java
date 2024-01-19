package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.CONFLICT, reason = "Container is still running")
public class ContainerStillRunningException extends Exception {

    public ContainerStillRunningException(String msg) {
        super(msg);
    }

    public ContainerStillRunningException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public ContainerStillRunningException(Throwable thr) {
        super(thr);
    }

}
