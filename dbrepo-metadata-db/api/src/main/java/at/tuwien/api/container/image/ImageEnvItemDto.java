package at.tuwien.api.container.image;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ImageEnvItemDto {

    @NotNull
    private Long iid;

    @NotBlank
    @Schema(example = "MARIADB_ROOT_PASSWORD")
    private String key;

    @NotBlank
    @JsonIgnore
    @ToString.Exclude
    @Schema(example = "mariadb")
    private String value;

    @NotNull
    @Schema(example = "PRIVILEGED_PASSWORD")
    private ImageEnvItemTypeDto type;

}
