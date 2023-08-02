package at.tuwien.api.crossref.label;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class CrossrefPrefLabelDto {

    @JsonProperty("Label")
    private CrossrefLabelDto label;

}
