package at.ac.tuwien.ifs.dbrepo.core.api.amqp;

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
public class VirtualHostPermissionDto {

    @NotNull
    @Schema(example = "username")
    private String user;

    @NotNull
    @Schema(example = "dbrepo")
    private String vhost;

    @NotNull
    @Schema(example = ".*")
    private String configure;

    @NotNull
    @Schema(example = ".*")
    private String write;

    @NotNull
    @Schema(example = ".*")
    private String read;

}
