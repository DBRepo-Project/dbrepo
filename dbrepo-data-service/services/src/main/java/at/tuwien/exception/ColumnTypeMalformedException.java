package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.IOException;

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class ColumnTypeMalformedException extends IOException {

    public ColumnTypeMalformedException(String msg) {
        super(msg);
    }

    public ColumnTypeMalformedException(String msg, Throwable thr) {
        super(msg, thr);
    }

    public ColumnTypeMalformedException(Throwable thr) {
        super(thr);
    }

}
