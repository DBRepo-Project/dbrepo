package at.ac.tuwien.ifs.dbrepo.core.api.crossref.form;

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
public class CrossRefLiteralFormDto {

    @Schema(example = "en")
    private String lang;

    @Schema(example = "National Science Foundation")
    private String content;

}
