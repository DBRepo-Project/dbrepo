package at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.foreign;

import io.swagger.v3.oas.annotations.media.Schema;
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
public class ForeignKeyBriefDto {

    @Schema(description = "The foreign key id", example = "f2b740ec-0b13-4d07-88a9-529d354bba6a")
    private UUID id;
}
