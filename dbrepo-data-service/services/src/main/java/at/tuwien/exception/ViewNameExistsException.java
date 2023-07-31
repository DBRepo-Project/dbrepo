package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.IOException;

@ResponseStatus(code = HttpStatus.CONFLICT)
public class ViewNameExistsException extends IOException {

    public ViewNameExistsException(String msg) {
        super(msg);
    }

    public ViewNameExistsException(String msg, Throwable thr) {
        super(msg, thr);
    }

    public ViewNameExistsException(Throwable thr) {
        super(thr);
    }

}
