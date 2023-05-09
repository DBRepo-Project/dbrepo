package at.tuwien.api.error;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.http.HttpStatus;

import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiErrorDto {

    @NotNull(message = "http status is required")
    @Schema(example = "STATUS")
    private HttpStatus status;

    @NotNull(message = "message is required")
    @Schema(example = "Error message")
    private String message;

    @NotNull(message = "code is required")
    @Schema(example = "error.service.code")
    private String code;

}
