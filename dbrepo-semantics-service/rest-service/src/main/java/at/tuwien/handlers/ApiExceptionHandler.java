package at.tuwien.handlers;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

//    @Hidden
//    @ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
//    @ExceptionHandler({AmqpException.class})
//    public ResponseEntity<ApiErrorDto> handle(AmqpException e, WebRequest request) {
//        final ApiErrorDto response = ApiErrorDto.builder()
//                .status(HttpStatus.GATEWAY_TIMEOUT)
//                .message(e.getLocalizedMessage())
//                .code("error.table.amqp")
//                .build();
//        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
//    }

}
