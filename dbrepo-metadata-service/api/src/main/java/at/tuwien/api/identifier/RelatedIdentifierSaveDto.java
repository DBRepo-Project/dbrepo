package at.tuwien.api.identifier;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class RelatedIdentifierSaveDto {

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


