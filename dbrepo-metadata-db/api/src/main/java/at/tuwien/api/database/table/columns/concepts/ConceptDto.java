package at.tuwien.api.database.table.columns.concepts;

import at.tuwien.api.database.table.columns.ColumnBriefDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Id;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.jackson.Jacksonized;
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
@Document(indexName = "concept")
public class ConceptDto {

    @Id
    private Long id;

    @NotBlank
    private String uri;

    private String name;

    private String description;

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

    @NotNull
    private List<ColumnBriefDto> columns;
}
