package at.tuwien.api.error;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;
import org.springframework.http.HttpStatus;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiErrorDto {

    @NotNull(message = "http status is required")
    @Parameter(name = "http status", example = "NOT_FOUND")
    private HttpStatus status;

    @NotNull(message = "message is required")
    @Parameter(name = "message", example = "Could not find container")
    private String message;

    @NotNull(message = "code is required")
    @Parameter(name = "code", example = "error.container.notfound")
    private String code;

}
