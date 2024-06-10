package at.tuwien.api.database.table;

import at.tuwien.api.database.table.columns.ColumnBriefDto;
import at.tuwien.api.user.UserBriefDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class TableBriefDto {

    @NotNull
    private Long id;

    @NotNull
    @JsonProperty("database_id")
    private Long databaseId;

    @NotBlank
    @Schema(example = "Air Quality")
    private String name;

    @Schema(example = "Air Quality in Austria")
    private String description;

    @NotBlank
    @JsonProperty("internal_name")
    @Schema(example = "air_quality")
    private String internalName;

    @NotNull
    @JsonProperty("is_versioned")
    @Schema(example = "true")
    private Boolean isVersioned;

    @NotNull
    private UserBriefDto owner;
}
