package at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.foreign.CreateForeignKeyDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class CreateTableConstraintsDto {

    @NotNull
    @Schema(description = "The list of unique column(s)", example = "[]")
    private List<List<String>> uniques;

    @NotNull
    @JsonProperty("foreign_keys")
    private List<CreateForeignKeyDto> foreignKeys;

    @NotNull
    @Schema(description = "The list of unique column(s)", example = "[]")
    private Set<String> checks;

    @NotNull
    @JsonProperty("primary_key")
    @Schema(description = "The list of primary key column(s)", example = "[\"id\"]")
    private Set<String> primaryKey;

}
