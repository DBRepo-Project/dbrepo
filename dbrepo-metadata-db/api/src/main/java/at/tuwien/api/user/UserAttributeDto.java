package at.tuwien.api.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserAttributeDto {

    @NotNull
    @JsonIgnore
    private UUID id;

    @NotNull
    @JsonIgnore
    @Schema(example = "1ffc7b0e-9aeb-4e8b-b8f1-68f3936155b4")
    private UUID userId;

    @Schema(example = "theme_dark")
    private String name;

    @Schema(example = "true")
    private String value;

}
