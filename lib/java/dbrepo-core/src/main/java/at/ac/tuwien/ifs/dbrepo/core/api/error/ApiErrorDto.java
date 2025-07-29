package at.ac.tuwien.ifs.dbrepo.core.api.error;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import org.springframework.http.HttpStatus;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ApiErrorDto {

    @NotNull
    @Schema(example = "NOT_FOUND")
    private HttpStatus status;

    @NotNull
    @Schema(description = "The error message in English", example = "Error message")
    private String message;

    @NotNull
    @Schema(description = "The error code used for internationalization of the error messages", example = "error.service.code")
    private String code;

}
