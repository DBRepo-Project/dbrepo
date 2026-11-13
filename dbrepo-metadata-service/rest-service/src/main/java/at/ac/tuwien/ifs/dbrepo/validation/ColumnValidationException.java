package at.ac.tuwien.ifs.dbrepo.validation;

import at.ac.tuwien.ifs.dbrepo.core.exception.MalformedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "error.schema.invalid")
public class ColumnValidationException extends MalformedException {
    public ColumnValidationException(String message) {
        super(message);
    }
}
