package at.ac.tuwien.ifs.dbrepo.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_GATEWAY, reason = "error.metadata.connection")
public class MetadataServiceConnectionException extends Exception {

    public MetadataServiceConnectionException(String msg) {
        super(msg);
    }

    public MetadataServiceConnectionException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public MetadataServiceConnectionException(Throwable thr) {
        super(thr);
    }

}
