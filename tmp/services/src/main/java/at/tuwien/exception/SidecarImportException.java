package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.SERVICE_UNAVAILABLE)
public class SidecarImportException extends Exception {

    public SidecarImportException(String message) {
        super(message);
    }

    public SidecarImportException(String message, Throwable thr) {
        super(message, thr);
    }

    public SidecarImportException(Throwable thr) {
        super(thr);
    }

}
