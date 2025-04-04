package at.ac.tuwien.ifs.dbrepo.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.SERVICE_UNAVAILABLE, reason = "error.metadata.invalid")
public class MetadataServiceException extends Exception {

    public MetadataServiceException(String message) {
        super(message);
    }

    public MetadataServiceException(String message, Throwable thr) {
        super(message, thr);
    }

    public MetadataServiceException(Throwable thr) {
        super(thr);
    }

}
