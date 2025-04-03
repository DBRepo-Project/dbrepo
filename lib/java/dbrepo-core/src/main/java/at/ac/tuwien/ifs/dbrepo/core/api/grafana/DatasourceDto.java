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
    @Schema(example = "1")
    private Long id;

    @NotNull
    @Schema(example = "kLtEtcRGk")
    private String uid;

    @NotNull
    @Schema(example = "1")
    private Long orgId;

    @NotNull
    @Schema(example = "some_datasource")
    private String name;

    @NotNull
    @Schema(example = "plugins/logo.svg")
    private String typeLogoUrl;

    @NotNull
    @Schema(example = "PROXY")
    private AccessTypeDto access;

    @Schema(example = "http://example.com")
    private String url;

    @Schema(example = "s3cr3t")
    private String password;

    @Schema(example = "user")
    private String user;

    @Schema(example = "true")
    private Boolean basicAuth;

    @Schema(example = "user")
    private String basicAuthUser;

    @Schema(example = "s3cr3t")
    private String basicAuthPassword;

    @Schema(example = "false")
    private Boolean withCredentials;

    @Schema(example = "false")
    private Boolean isDefault;

    @NotNull
    @Schema(example = "true")
    private Boolean readOnly;

    @NotNull
    @Schema(example = "INFINITY")
    private DatasourceTypeDto type;

    @NotNull
    @Schema(example = "0")
    private Integer version;

}
