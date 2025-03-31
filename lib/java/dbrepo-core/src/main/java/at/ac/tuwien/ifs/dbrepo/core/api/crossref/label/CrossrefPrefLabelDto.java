package at.ac.tuwien.ifs.dbrepo.core.api.crossref.label;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class CrossrefPrefLabelDto {

    @JsonProperty("Label")
    private CrossrefLabelDto label;

}
