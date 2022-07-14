package at.tuwien.api.database.table.columns.concepts;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.time.Instant;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConceptDto {

    @NotNull
    @Parameter(name = "uri", required = true)
    private String uri;

    @NotNull
    @Parameter(name = "name", required = true)
    private String name;

    @NotNull
    @Parameter(name = "created", required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;
}
