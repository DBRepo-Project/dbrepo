package at.ac.tuwien.ifs.dbrepo.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "error.container.missing")
public class ContainerNotFoundException extends Exception {

    public ContainerNotFoundException(String msg) {
        super(msg);
    }

    public ContainerNotFoundException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public ContainerNotFoundException(Throwable thr) {
        super(thr);
    }

}
