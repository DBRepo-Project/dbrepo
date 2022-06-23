package at.tuwien.api.document.record;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DraftVersionsDto {

    @Parameter(name = "index")
    private Long index;

    @JsonProperty("is_latest")
    @Parameter(name = "is latest version")
    private Boolean isLatest;

    @JsonProperty("is_latest_draft")
    @Parameter(name = "is latest draft")
    private Boolean isLatestDraft;

}
