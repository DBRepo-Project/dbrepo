package at.ac.tuwien.ifs.dbrepo.core.api.crossref.label;

import at.ac.tuwien.ifs.dbrepo.core.api.crossref.form.CrossRefLiteralFormDto;
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
public class CrossRefLabelDto {

    private CrossRefLiteralFormDto literalForm;

    @Schema(example = "http://data.crossref.org/fundingdata/vocabulary/Label-36515")
    private String about;

}
