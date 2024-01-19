package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.CONFLICT, reason = "Image already exists")
public class ImageAlreadyExistsException extends Exception {

    public ImageAlreadyExistsException(String msg) {
        super(msg);
    }

    public ImageAlreadyExistsException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public ImageAlreadyExistsException(Throwable thr) {
        super(thr);
    }

}