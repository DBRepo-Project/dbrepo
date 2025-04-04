package at.ac.tuwien.ifs.dbrepo.core.api.semantics;

import com.fasterxml.jackson.annotation.JsonProperty;
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
@EqualsAndHashCode
@ToString
public class TableColumnEntityDto {

    @NotNull
    @JsonProperty("database_id")
    @Schema(example = "475b4107-a64d-4495-a7ef-3cb0dadd4804")
    private UUID databaseId;

    @NotNull
    @JsonProperty("table_id")
    @Schema(example = "9a9208af-90ea-4382-9a11-0c8f6d89bd1f")
    private UUID tableId;

    @NotNull
    @JsonProperty("column_id")
    @Schema(example = "297860e3-3b29-451c-ae8a-a85ed5941018")
    private UUID columnId;

    @NotBlank
    @Schema(example = "https://www.wikidata.org/entity/Q1686799")
    private String uri;

    @Schema(example = "Apache Jena")
    private String label;

    @Schema(example = "open source semantic web framework for Java")
    private String description;

}
