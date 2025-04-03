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
    @Schema(example = "3")
    private Long id;

    @NotNull
    @Schema(example = "eeckcuwfsfbi8b")
    private String uid;
}
