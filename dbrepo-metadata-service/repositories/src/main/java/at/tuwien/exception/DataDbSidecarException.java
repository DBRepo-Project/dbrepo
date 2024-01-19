package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.IOException;

@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
public class DataDbSidecarException extends IOException {

    public DataDbSidecarException(String msg) {
        super(msg);
    }

    public DataDbSidecarException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public DataDbSidecarException(Throwable thr) {
        super(thr);
    }

}
