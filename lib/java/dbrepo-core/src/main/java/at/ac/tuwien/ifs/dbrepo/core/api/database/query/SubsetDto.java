package at.ac.tuwien.ifs.dbrepo.core.api.database.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class SubsetDto {

    @NotNull
    @Schema(description = "The id(s) of the column(s)")
    private Set<SubsetColumnDto> columns;

    @NotNull
    @JsonProperty("datasource_ids")
    @Schema(description = "The table(s) that are selected")
    private Set<UUID> datasourceIds;

    @Schema(description = "The join(s) that are applied")
    private Set<JoinDto> joins;

    private List<FilterDto> filters;

    private Set<OrderDto> orders;

}
