package at.tuwien.api.identifier;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
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
public class IdentifierBriefDto {

    @NotNull
    private Long id;

    @JsonProperty("database_id")
    @Field(name = "database_id")
    @Schema(example = "1")
    private Long databaseId;

    @JsonProperty("query_id")
    @Field(name = "query_id")
    @Schema(example = "1")
    private Long queryId;

    @NotNull
    private IdentifierTypeDto type;

    private List<IdentifierTitleDto> titles;

    @Schema(example = "10.1038/nphys1170")
    private String doi;

    @Schema(example = "TU Wien")
    private String publisher;

    @NotNull
    @JsonProperty("publication_year")
    @Field(name = "publication_year")
    @Schema(example = "2022")
    private Integer publicationYear;

    @NotNull
    private List<CreatorBriefDto> creators;

    @JsonIgnore
    @Field(type = FieldType.Date)
    @Schema(example = "2021-03-12T15:26:21Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

    @JsonIgnore
    @org.springframework.data.annotation.Transient
    @Field(type = FieldType.Date)
    @JsonProperty("last_modified")
    @Schema(example = "2021-03-12T15:26:21Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant lastModified;

}
