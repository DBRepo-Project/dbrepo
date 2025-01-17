package at.tuwien.api.crossref;

import at.tuwien.api.crossref.label.CrossrefPrefLabelDto;
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
public class CrossrefDto {

    @Schema(example = "https://doi.org/10.13039/100000001")
    private String id;

    private CrossrefPrefLabelDto prefLabel;

}
