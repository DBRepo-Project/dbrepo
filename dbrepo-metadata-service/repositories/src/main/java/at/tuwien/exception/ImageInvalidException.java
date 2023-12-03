package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "Image already exists")
public class ImageInvalidException extends Exception {

    public ImageInvalidException(String msg) {
        super(msg);
    }

    public ImageInvalidException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public ImageInvalidException(Throwable thr) {
        super(thr);
    }

}