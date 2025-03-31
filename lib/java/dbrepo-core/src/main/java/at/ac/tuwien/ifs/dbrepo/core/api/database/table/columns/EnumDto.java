package at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class EnumDto {

    @NotNull
    @Schema(example = "5343bb3d-14d3-4eb7-a86f-b8fc553cb315")
    private UUID id;

    @NotNull
    @Schema(example = "3")
    private String value;

}
