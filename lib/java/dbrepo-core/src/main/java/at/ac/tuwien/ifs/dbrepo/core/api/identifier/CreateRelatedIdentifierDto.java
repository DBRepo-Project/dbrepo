package at.ac.tuwien.ifs.dbrepo.core.api.identifier;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class CreateRelatedIdentifierDto {

    @NotNull
    @Schema(example = "10.70124/dc4zh-9ce78")
    private String value;

    @NotNull
    @Schema(example = "DOI")
    private RelatedTypeDto type;

    @NotNull
    @Schema(example = "Cites")
    private RelationTypeDto relation;

}


