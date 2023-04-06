package at.tuwien.api.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserAttributeDto {

    @NotNull
    @JsonIgnore
    private String id;

    @NotNull
    @JsonIgnore
    @JsonProperty("user_id")
    @Schema(example = "1ffc7b0e-9aeb-4e8b-b8f1-68f3936155b4")
    private String userId;

    @Schema(example = "theme_dark")
    private String name;

    @Schema(example = "true")
    private String value;

}
