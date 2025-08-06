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
public class UpdateDashboardAccessDto {

    @NotNull
    @Schema(description = "The permission", example = "View")
    private PermissionTypeDto permission;
}
