package at.tuwien.api.identifier;

import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.LanguageTypeDto;
import at.tuwien.api.database.LicenseDto;
import at.tuwien.api.user.UserDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
@Document(indexName = "identifier")
public class IdentifierDto {

    @Id
    private Long id;

    @Id
    @JsonProperty("database_id")
    @Field(name = "database_id")
    @Schema(example = "1")
    private Long databaseId;

    @JsonProperty("query_id")
    @Field(name = "query_id")
    @Schema(example = "1")
    private Long queryId;

    @JsonProperty("view_id")
    @Field(name = "view_id")
    @Schema(example = "1")
    private Long viewId;

    @NotNull
    private IdentifierTypeDto type;

    private List<IdentifierTitleDto> titles;

    private List<IdentifierDescriptionDto> descriptions;

    private List<IdentifierFunderDto> funders;

    @NotBlank
    @Schema(example = "SELECT `id`, `value`, `location` FROM `air_quality` WHERE `location` = \"09:STEF\"")
    private String query;

    @NotBlank
    @JsonProperty("query_normalized")
    @Field(name = "query_normalized")
    @Schema(example = "SELECT `id`, `value`, `location` FROM `air_quality` WHERE `location` = \"09:STEF\"")
    private String queryNormalized;

    @JsonProperty("related_identifiers")
    @Field(name = "related_identifiers")
    private List<RelatedIdentifierDto> relatedIdentifiers;

    @NotNull
    private DatabaseDto database;

    @NotBlank
    @JsonProperty("query_hash")
    @Field(name = "query_hash")
    @Schema(description = "query hash in sha512")
    private String queryHash;

    @NotNull
    @Field(type = FieldType.Date)
    @Schema(example = "2021-03-12T15:26:21Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant execution;

    @NotBlank
    @JsonProperty("result_hash")
    @Field(name = "result_hash")
    @Schema(example = "34fe82cda2c53f13f8d90cfd7a3469e3a939ff311add50dce30d9136397bf8e5")
    private String resultHash;

    @NotNull
    @JsonProperty("result_number")
    @Field(name = "result_number")
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
    @Field(name = "publication_day")
    @Schema(example = "15")
    private Integer publicationDay;

    @JsonProperty("publication_month")
    @Field(name = "publication_month")
    @Schema(example = "12")
    private Integer publicationMonth;

    @NotNull
    @JsonProperty("publication_year")
    @Field(name = "publication_year")
    @Schema(example = "2022")
    private Integer publicationYear;

    private LanguageTypeDto language;

    private List<LicenseDto> licenses;

    @NotNull
    private List<CreatorDto> creators;

    @NotNull
    @Field(type = FieldType.Date)
    @Schema(example = "2021-03-12T15:26:21Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

    @NotNull
    @JsonProperty("last_modified")
    @Schema(example = "2021-03-12T15:26:21Z")
    @org.springframework.data.annotation.Transient
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant lastModified;

}
