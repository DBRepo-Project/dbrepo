package at.ac.tuwien.ifs.dbrepo.core.api.container.image;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class OperatorDto {

    @NotNull
    @Schema(description = "The operator id", example = "816f55d5-1098-4f60-a4af-c8121c04dccf")
    private UUID id;

    @NotBlank
    @JsonProperty("display_name")
    @Schema(description = "The user-friendly name of the operator", example = "XOR")
    private String displayName;

    @NotBlank
    @Schema(description = "The machine-friendly name of the operator", example = "XOR")
    private String value;

    @NotNull
    @Schema(description = "The documentation link", example = "https://mariadb.com/kb/en/xor/")
    private String documentation;

}
