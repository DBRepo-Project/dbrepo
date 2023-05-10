package at.tuwien.api.identifier;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

@Data
@Getter
@Setter
@Builder
public class IdentifierBriefDto {

    @NotNull
    private Long id;

    @NotNull
    @JsonProperty("container_id")
    @Schema(name = "container id", example = "1")
    private Long containerId;

    @NotNull
    @JsonProperty("database_id")
    @Schema(name = "database id", example = "1")
    private Long databaseId;

    @JsonProperty("query_id")
    @Schema(name = "query id", example = "1")
    private Long queryId;

    @NotBlank
    @Schema(example = "Airquality Stephansplatz, Vienna, Austria")
    private String title;

    @NotNull
    private IdentifierTypeDto type;

    @Schema(example = "10.1038/nphys1170")
    private String doi;

    @Schema(example = "TU Wien")
    private String publisher;

    @NotNull
    @JsonProperty("publication_year")
    @Schema(example = "2022")
    private Integer publicationYear;

    @NotNull
    private List<CreatorBriefDto> creators;

    @JsonIgnore
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

    @JsonIgnore
    @JsonProperty("last_modified")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant lastModified;

}
