package at.tuwien.api.database.table.columns.concepts;

import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import javax.persistence.Column;
import javax.persistence.Id;
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
    @ApiModelProperty(name = "uri", required = true)
    private String uri;

    @NotNull
    @ApiModelProperty(name = "name", required = true)
    private String name;

    @NotNull
    @ApiModelProperty(name = "created", required = true)
    private Instant created;
}
