package at.tuwien.api.doi;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class AffiliationDto {

    @Schema(example = "ISE, TU Wien, Data Science Research Unit, Vienna, Austria")
    private String name;

}
