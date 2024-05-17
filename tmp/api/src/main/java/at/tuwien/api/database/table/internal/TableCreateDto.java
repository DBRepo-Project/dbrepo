package at.tuwien.api.database.table.internal;

import at.tuwien.api.database.table.columns.ColumnCreateDto;
import at.tuwien.api.database.table.constraints.ConstraintsCreateDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class TableCreateDto {

    @NotBlank
    @Size(min = 1, max = 64)
    @Schema(example = "Air Quality")
    private String name;

    @NotNull
    @JsonProperty("need_sequence")
    private Boolean needSequence;

    @Size(max = 180)
    @Schema(example = "Air Quality in Austria")
    private String description;

    @NotNull
    private List<ColumnCreateDto> columns;

    @NotNull
    private ConstraintsCreateDto constraints;
}
