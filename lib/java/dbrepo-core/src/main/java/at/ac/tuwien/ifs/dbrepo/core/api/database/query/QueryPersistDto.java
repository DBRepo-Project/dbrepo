package at.ac.tuwien.ifs.dbrepo.core.api.database.query;

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
public class QueryPersistDto {

    @NotNull
    @Schema(description = "If false, the query is marked for deletion at a later point in time", example = "true")
    private Boolean persist;

}
