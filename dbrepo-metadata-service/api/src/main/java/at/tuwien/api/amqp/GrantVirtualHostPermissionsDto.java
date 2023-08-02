package at.tuwien.api.amqp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotNull;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class GrantVirtualHostPermissionsDto {

    @NotNull
    @Schema(example = ".*")
    private String configure;

    @NotNull
    @Schema(example = ".*")
    private String write;

    @NotNull
    @Schema(example = ".*")
    private String read;

}
