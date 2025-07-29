package at.ac.tuwien.ifs.dbrepo.core.api.grafana;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
@Jacksonized
@ToString
public class DatasourceDto {

    @NotNull
    @Schema(description = "The id", example = "1")
    private Long id;

    @NotNull
    @Schema(description = "The unique id", example = "kLtEtcRGk")
    private String uid;

    @NotNull
    @Schema(description = "The organization id", example = "1")
    private Long orgId;

    @NotNull
    @Schema(description = "The machine-friendly name", example = "some_datasource")
    private String name;

    @NotNull
    @Schema(description = "The datasource type logo url", example = "plugins/logo.svg")
    private String typeLogoUrl;

    @NotNull
    @Schema(description = "The access", example = "PROXY")
    private AccessTypeDto access;

    @Schema(description = "The url", example = "http://example.com")
    private String url;

    @Schema(description = "The password", example = "s3cr3t")
    private String password;

    @Schema(description = "The user", example = "user")
    private String user;

    @Schema(description = "If true, configure the data source with basic authentication", example = "true")
    private Boolean basicAuth;

    @Schema(description = "The basic auth username", example = "user")
    private String basicAuthUser;

    @Schema(description = "The basic auth password", example = "s3cr3t")
    private String basicAuthPassword;

    @Schema(example = "false")
    private Boolean withCredentials;

    @Schema(description = "If true, this is the default data source for grafana", example = "false")
    private Boolean isDefault;

    @NotNull
    @Schema(description = "If true, configure the datasource to read only", example = "true")
    private Boolean readOnly;

    @NotNull
    @Schema(description = "The type", example = "INFINITY")
    private DatasourceTypeDto type;

    @NotNull
    @Schema(description = "The configuration version", example = "0")
    private Integer version;

}
