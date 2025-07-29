package at.ac.tuwien.ifs.dbrepo.core.api.grafana;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
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
public class CreateDashboardResponseDto {

    @NotNull
    @Schema(description = "The generated dashboard id", example = "3")
    private Long id;

    @NotNull
    @Schema(description = "The generated dashboard unique id", example = "eeckcuwfsfbi8b")
    private String uid;
}
