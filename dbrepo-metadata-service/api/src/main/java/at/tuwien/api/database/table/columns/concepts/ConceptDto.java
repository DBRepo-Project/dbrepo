package at.tuwien.api.database.table.columns.concepts;

import at.tuwien.api.database.table.columns.ColumnBriefDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class ConceptDto {

    @Id
    @NotNull
    @Field(name = "id", type = FieldType.Keyword)
    private Long id;

    @NotBlank
    @Field(name = "uri", type = FieldType.Keyword)
    private String uri;

    @Field(name = "name", type = FieldType.Keyword)
    private String name;

    @Field(name = "description", type = FieldType.Text)
    private String description;

    @NotNull
    @Field(name = "created", type = FieldType.Date)
    @Schema(example = "2021-03-12T15:26:21Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

    @NotNull
    @org.springframework.data.annotation.Transient
    private List<ColumnBriefDto> columns;
}
