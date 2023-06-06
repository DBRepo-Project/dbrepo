package at.tuwien.api.identifier;

import at.tuwien.api.database.LanguageTypeDto;
import at.tuwien.api.database.LicenseDto;
import at.tuwien.api.user.UserDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.elasticsearch.annotations.Document;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
@Document(indexName = "identifier", createIndex = false)
public class IdentifierDto {

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

    @NotNull
    private IdentifierTypeDto type;

    @NotBlank
    @Schema(example = "Airquality Stephansplatz, Vienna, Austria")
    private String title;

    @Schema(example = "Air quality reports at Stephansplatz, Vienna")
    private String description;

    @NotBlank
    @Schema(example = "SELECT `id`, `value`, `location` FROM `air_quality` WHERE `location` = \"09:STEF\"")
    private String query;

    @NotBlank
    @JsonProperty("query_normalized")
    @Schema(example = "SELECT `id`, `value`, `location` FROM `air_quality` WHERE `location` = \"09:STEF\"")
    private String queryNormalized;

    @JsonProperty("related")
    private List<RelatedIdentifierDto> related;

    @NotBlank
    @JsonProperty("query_hash")
    @Schema(description = "query hash in sha512")
    private String queryHash;

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant execution;

    @NotBlank
    @JsonProperty("result_hash")
    private String resultHash;

    @NotNull
    @JsonProperty("result_number")
    @Schema(example = "1")
    private Long resultNumber;

    @NotNull
    @Schema(example = "everyone")
    private VisibilityTypeDto visibility;

    @Schema(example = "10.1038/nphys1170")
    private String doi;

    @Schema(example = "TU Wien")
    private String publisher;

    @NotNull
    @JsonIgnore
    private UserDto creator;

    @JsonProperty("publication_day")
    @Schema(example = "15")
    private Integer publicationDay;

    @JsonProperty("publication_month")
    @Schema(example = "12")
    private Integer publicationMonth;

    @NotNull
    @JsonProperty("publication_year")
    @Schema(example = "2022")
    private Integer publicationYear;

    private LanguageTypeDto language;

    private LicenseDto license;

    @NotNull
    private List<CreatorDto> creators;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

    @JsonProperty("last_modified")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant lastModified;

}
