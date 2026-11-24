package at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.check;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
@EqualsAndHashCode
public class CheckDto {

    @NotNull
    @Schema(description = "The check constraint id", example = "a699f758-7e3d-4af6-92ac-56cd40e6052f")
    private UUID id;

    @NotBlank
    @Schema(description = "The check constraint expression", example = "`temperature` > 0")
    private String check;
}
