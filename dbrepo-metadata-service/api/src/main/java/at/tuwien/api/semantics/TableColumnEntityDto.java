package at.tuwien.api.semantics;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.Objects;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class TableColumnEntityDto {

    @NotNull
    @JsonProperty("database_id")
    @Schema(example = "1")
    private Long databaseId;

    @NotNull
    @JsonProperty("table_id")
    @Schema(example = "1")
    private Long tableId;

    @NotNull
    @JsonProperty("column_id")
    @Schema(example = "1")
    private Long columnId;

    @NotBlank
    @Schema(example = "https://www.wikidata.org/entity/Q1686799")
    private String uri;

    @Schema(example = "Apache Jena")
    private String label;

    @Schema(example = "open source semantic web framework for Java")
    private String description;

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TableColumnEntityDto other = (TableColumnEntityDto) obj;
        return Objects.equals(uri, other.uri);
    }

}
