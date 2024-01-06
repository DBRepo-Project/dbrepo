package at.tuwien.api.identifier;

import at.tuwien.api.database.DatabaseDto;
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
public class IdentifierDto {

    @Id
    @NotNull
    @Field(name = "id", type = FieldType.Keyword)
    private Long id;

    @JsonProperty("database_id")
    @Schema(example = "1")
    @Field(name = "database_id", type = FieldType.Keyword)
    private Long databaseId;

    @JsonProperty("query_id")
    @Schema(example = "1")
    @Field(name = "query_id", type = FieldType.Keyword)
    private Long queryId;

    @JsonProperty("table_id")
    @Schema(example = "1")
    @Field(name = "table_id", type = FieldType.Keyword)
    private Long tableId;

    @JsonProperty("view_id")
    @Schema(example = "1")
    @Field(name = "view_id", type = FieldType.Keyword)
    private Long viewId;

    @NotNull
    @Field(name = "type", type = FieldType.Keyword)
    private IdentifierTypeDto type;

    @Field(name = "titles", type = FieldType.Nested)
    private List<IdentifierTitleDto> titles;

    @Field(name = "descriptions", type = FieldType.Nested)
    private List<IdentifierDescriptionDto> descriptions;

    @Field(name = "funders", type = FieldType.Nested)
    private List<IdentifierFunderDto> funders;

    @NotBlank
    @Schema(example = "SELECT `id`, `value`, `location` FROM `air_quality` WHERE `location` = \"09:STEF\"")
    @Field(name = "query", type = FieldType.Text)
    private String query;

    @NotBlank
    @JsonProperty("query_normalized")
    @Schema(example = "SELECT `id`, `value`, `location` FROM `air_quality` WHERE `location` = \"09:STEF\"")
    @Field(name = "query_normalized", type = FieldType.Text)
    private String queryNormalized;

    @JsonProperty("related_identifiers")
    @Field(name = "related_identifiers", type = FieldType.Nested)
    private List<RelatedIdentifierDto> relatedIdentifiers;

    @NotBlank
    @JsonProperty("query_hash")
    @Schema(description = "query hash in sha512")
    @Field(name = "query_hash", type = FieldType.Text)
    private String queryHash;

    @NotNull
    @Field(name = "execution", type = FieldType.Date)
    @Schema(example = "2021-03-12T15:26:21Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant execution;

    @NotBlank
    @JsonProperty("result_hash")
    @Field(name = "result_hash", type = FieldType.Text)
    @Schema(example = "34fe82cda2c53f13f8d90cfd7a3469e3a939ff311add50dce30d9136397bf8e5")
    private String resultHash;

    @NotNull
    @JsonProperty("result_number")
    @Field(name = "result_number", type = FieldType.Long)
    @Schema(example = "1")
    private Long resultNumber;

    @Schema(example = "10.1038/nphys1170")
    @Field(name = "doi", type = FieldType.Keyword)
    private String doi;

    @Schema(example = "TU Wien")
    @Field(name = "publisher", type = FieldType.Text)
    private String publisher;

    @NotNull
    @JsonIgnore
    @Field(name = "creator", type = FieldType.Nested)
    private UserDto creator;

    @JsonProperty("publication_day")
    @Schema(example = "15")
    @Field(name = "publication_day", type = FieldType.Integer)
    private Integer publicationDay;

    @JsonProperty("publication_month")
    @Schema(example = "12")
    @Field(name = "publication_month", type = FieldType.Integer)
    private Integer publicationMonth;

    @NotNull
    @JsonProperty("publication_year")
    @Schema(example = "2022")
    @Field(name = "publication_year", type = FieldType.Integer)
    private Integer publicationYear;

    @Field(name = "language", type = FieldType.Keyword)
    private LanguageTypeDto language;

    @Field(name = "licenses", type = FieldType.Nested)
    private List<LicenseDto> licenses;

    @NotNull
    @Field(name = "creators", type = FieldType.Nested)
    private List<CreatorDto> creators;

    @NotNull
    @Field(name = "created", type = FieldType.Date)
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
