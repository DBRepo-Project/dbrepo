package at.ac.tuwien.ifs.dbrepo.core.api.database.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
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
    @JsonProperty("datasource_id")
    @Schema(example = "f7df2a7d-4ade-4c78-97b0-7c744d0893c7")
    private UUID datasourceId;

    @NotNull
    @JsonProperty("datasource_type")
    private DatasourceType datasourceType;

    @NotNull
    @Schema(example = "[\"e891ba86-0258-41a6-a8d9-ff58bc10b618\"]")
    private List<UUID> columns;

    private List<FilterDto> filter;

    private List<OrderDto> order;

}
