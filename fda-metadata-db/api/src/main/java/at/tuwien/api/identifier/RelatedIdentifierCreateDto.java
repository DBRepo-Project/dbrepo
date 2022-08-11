package at.tuwien.api.identifier;

import io.swagger.v3.oas.annotations.Parameter;
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
    @Parameter(name = "identifier", example = "10.70124/dc4zh-9ce78")
    private String value;

    @Parameter(name = "type", example = "DOI")
    private RelatedTypeDto type;

    @Parameter(name = "relation", example = "Cites")
    private RelationTypeDto relation;

}


