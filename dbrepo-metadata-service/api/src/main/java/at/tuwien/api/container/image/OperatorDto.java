package at.tuwien.api.container.image;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class OperatorDto {

    @NotBlank
    @JsonProperty("display_name")
    @Schema(example = "XOR")
    private String displayName;

    @NotBlank
    @Schema(example = "XOR")
    private String value;

    @NotNull
    @Schema(example = "https://mariadb.com/kb/en/xor/")
    private String documentation;

}
