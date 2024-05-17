package at.tuwien.api.identifier;

import at.tuwien.api.database.LanguageTypeDto;
import at.tuwien.api.database.LicenseDto;
import at.tuwien.api.user.UserDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.annotation.Id;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class IdentifierDto {

    @NotNull
    private Long id;

    @NotNull
    @JsonProperty("database_id")
    @Schema(example = "1")
    private Long databaseId;

    @JsonProperty("query_id")
    @Schema(example = "1")
    private Long queryId;

    @JsonProperty("table_id")
    @Schema(example = "1")
    private Long tableId;

    @JsonProperty("view_id")
    @Schema(example = "1")
    private Long viewId;

    @NotNull
    private IdentifierTypeDto type;

    @NotNull
    private List<IdentifierTitleDto> titles;

    private List<IdentifierDescriptionDto> descriptions;

    private List<IdentifierFunderDto> funders;

    @NotBlank
    @Schema(example = "SELECT `id`, `value`, `location` FROM `air_quality` WHERE `location` = \"09:STEF\"")
    private String query;

    @NotBlank
    @JsonProperty("query_normalized")
    @Schema(example = "SELECT `id`, `value`, `location` FROM `air_quality` WHERE `location` = \"09:STEF\"")
    private String queryNormalized;

    @JsonProperty("related_identifiers")
    private List<RelatedIdentifierDto> relatedIdentifiers;

    @NotBlank
    @JsonProperty("query_hash")
    @Schema(description = "query hash in sha512")
    private String queryHash;

    @NotNull
    @Schema(example = "2021-03-12T15:26:21Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant execution;

    @JsonProperty("result_hash")
    @Schema(example = "34fe82cda2c53f13f8d90cfd7a3469e3a939ff311add50dce30d9136397bf8e5")
    private String resultHash;

    @JsonProperty("result_number")
    @Schema(example = "1")
    private Long resultNumber;

    @Schema(example = "10.1038/nphys1170")
    private String doi;

    @NotBlank
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

    private List<LicenseDto> licenses;

    @NotNull
    private List<CreatorDto> creators;

    @NotNull
    @Schema(example = "2021-03-12T15:26:21Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

    @NotNull
    @JsonProperty("last_modified")
    @Schema(example = "2021-03-12T15:26:21Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant lastModified;

}
