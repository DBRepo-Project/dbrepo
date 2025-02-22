package at.tuwien.api.identifier;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class SaveRelatedIdentifierDto {

    @NotNull
    @Schema(example = "5bb272c7-7421-4f74-83ac-0486812d0f44")
    private UUID id;

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


