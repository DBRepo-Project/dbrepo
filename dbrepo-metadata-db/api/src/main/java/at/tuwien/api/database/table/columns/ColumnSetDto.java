package at.tuwien.api.database.table.columns;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.elasticsearch.annotations.Field;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ColumnSetDto {

    @Id
    @NotNull
    private Long id;

    @Id
    @NotNull
    @Field(name = "database_id")
    @JsonProperty("database_id")
    private Long databaseId;

    @Id
    @NotNull
    @Field(name = "table_id")
    @JsonProperty("table_id")
    private Long tableId;

    @Id
    @NotNull
    @Field(name = "column_id")
    @JsonProperty("column_id")
    private Long columnId;

    @NotBlank
    @Schema(example = "val1")
    private String value;

}
