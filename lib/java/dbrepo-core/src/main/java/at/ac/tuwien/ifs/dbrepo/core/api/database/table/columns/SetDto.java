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
public class SetDto {

    @NotNull
    @Schema(example = "7eb4eded-bacc-4a91-84db-a9ae6ddafda7")
    private UUID id;

    @NotNull
    @Schema(example = "3")
    private String value;

}
