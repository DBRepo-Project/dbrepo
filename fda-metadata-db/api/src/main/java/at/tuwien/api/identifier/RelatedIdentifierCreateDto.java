package at.tuwien.api.identifier;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import javax.validation.constraints.NotNull;

@Data
@Getter
@Setter
@Builder
@Jacksonized
public class RelatedIdentifierCreateDto {

    @NotNull
    @Schema(example = "10.70124/dc4zh-9ce78")
    private String value;

    @Schema(example = "DOI")
    private RelatedTypeDto type;

    @Schema(example = "Cites")
    private RelationTypeDto relation;

}


