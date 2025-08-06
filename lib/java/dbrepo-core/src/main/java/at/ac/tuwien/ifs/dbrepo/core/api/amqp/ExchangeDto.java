package at.ac.tuwien.ifs.dbrepo.core.api.amqp;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
public class ExchangeDto {

    @NotNull
    @JsonProperty("auto_delete")
    @Schema(description = "If set to true, the auto-deleted exchange is deleted when the last binding is removed", example = "false")
    private Boolean autoDelete;

    @NotNull
    @Schema(description = "The exchange survives a broker restart", example = "true")
    private Boolean durable;

    @NotNull
    @Schema(description = "The exchange is marked as broker-internal", example = "false")
    private Boolean internal;

    @NotBlank
    @Schema(description = "The exchange name", example = "dbrepo")
    private String name;

    @NotBlank
    @Schema(description = "The exchange type", example = "quorum")
    private String type;

    @JsonProperty("user_who_performed_action")
    @Schema(description = "The user who created the exchange", example = "admin")
    private String creator;

    @NotBlank
    @Schema(description = "The virtual host name", example = "dbrepo")
    private String vhost;

}
