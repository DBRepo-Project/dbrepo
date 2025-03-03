package at.tuwien.api.database.table.constraints.foreign;

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

    @Schema(example = "f2b740ec-0b13-4d07-88a9-529d354bba6a")
    private UUID id;
}
