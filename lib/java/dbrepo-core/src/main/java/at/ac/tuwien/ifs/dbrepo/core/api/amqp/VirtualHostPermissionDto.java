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
    @Schema(description = "The user name", example = "username")
    private String user;

    @NotNull
    @Schema(description = "The virtual host name", example = "dbrepo")
    private String vhost;

    @NotNull
    @Schema(description = "The configure permissions regex", example = ".*")
    private String configure;

    @NotNull
    @Schema(description = "The write permissions regex", example = ".*")
    private String write;

    @NotNull
    @Schema(description = "The read permissions regex", example = ".*")
    private String read;

}
