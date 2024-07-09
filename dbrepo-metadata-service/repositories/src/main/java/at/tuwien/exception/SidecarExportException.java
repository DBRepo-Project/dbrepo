package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.SERVICE_UNAVAILABLE, reason = "error.sidecar.export")
public class SidecarExportException extends Exception {

    public SidecarExportException(String message) {
        super(message);
    }

    public SidecarExportException(String message, Throwable thr) {
        super(message, thr);
    }

    public SidecarExportException(Throwable thr) {
        super(thr);
    }

}
