package at.ac.tuwien.ifs.dbrepo.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "error.analyse.malformed")
public class AnalyseDataTypesException extends Exception {

    public AnalyseDataTypesException(String message) {
        super(message);
    }

    public AnalyseDataTypesException(String message, Throwable thr) {
        super(message, thr);
    }

    public AnalyseDataTypesException(Throwable thr) {
        super(thr);
    }

}
