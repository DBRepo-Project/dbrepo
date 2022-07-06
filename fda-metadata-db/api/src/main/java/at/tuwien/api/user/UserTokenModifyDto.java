package at.tuwien.api.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserTokenModifyDto {

    @NotBlank
    @JsonProperty("invenio_token")
    @Parameter(name = "invenio token")
    private String invenioToken;

}
