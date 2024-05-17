package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "error.image.missing")
public class ImageNotFoundException extends Exception {

    public ImageNotFoundException(String msg) {
        super(msg);
    }

    public ImageNotFoundException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public ImageNotFoundException(Throwable thr) {
        super(thr);
    }

}
