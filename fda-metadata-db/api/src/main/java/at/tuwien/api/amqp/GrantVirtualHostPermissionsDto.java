package at.tuwien.api.amqp;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GrantVirtualHostPermissionsDto {

    @NotNull
    @JsonProperty("virtual_host")
    @Parameter(name = "component name", example = "/")
    private String virtualHost;

    @NotNull
    @Parameter(name = "configure permission", example = ".*")
    private String configure;

    @NotNull
    @Parameter(name = "write permission", example = ".*")
    private String write;

    @NotNull
    @Parameter(name = "read permission", example = ".*")
    private String read;

}
