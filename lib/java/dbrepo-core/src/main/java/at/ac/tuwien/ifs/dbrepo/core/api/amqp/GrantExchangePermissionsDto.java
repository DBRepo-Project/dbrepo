package at.ac.tuwien.ifs.dbrepo.core.api.amqp;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class GrantExchangePermissionsDto {

    @NotNull
    @Schema(description = "The exchange name", example = "dbrepo")
    private String exchange;

    @NotNull
    @Schema(description = "The write permissions", example = ".*")
    private String write;

    @NotNull
    @Schema(description = "The read permissions", example = ".*")
    private String read;

}
