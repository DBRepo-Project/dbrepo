package at.ac.tuwien.ifs.dbrepo.core.api.crossref.label;

import at.ac.tuwien.ifs.dbrepo.core.api.crossref.form.CrossrefLiteralFormDto;
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
public class CrossrefLabelDto {

    private CrossrefLiteralFormDto literalForm;

    @Schema(example = "http://data.crossref.org/fundingdata/vocabulary/Label-36515")
    private String about;

}
