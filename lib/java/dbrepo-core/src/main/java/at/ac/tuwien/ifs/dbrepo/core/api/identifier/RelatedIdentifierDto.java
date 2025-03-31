package at.ac.tuwien.ifs.dbrepo.core.api.identifier;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotNull;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class RelatedIdentifierDto {

    @NotNull
    @Schema(example = "ce9d11f0-60a2-448d-a3e4-44719a443e8a")
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


