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
public class QueueDto {

    @NotNull
    @JsonProperty("auto_delete")
    @Schema(description = "If set to true, the auto-deleted queue is deleted when the last binding is removed", example = "false")
    private Boolean autoDelete;

    @NotNull
    @Schema(description = "The queue survives a broker restart", example = "true")
    private Boolean durable;

    @NotNull
    @Schema(description = "If set to true, the exclusive queue is only available to the declaring consumer")
    private Boolean exclusive;

    @NotBlank
    @Schema(description = "The queue name", example = "dbrepo")
    private String name;

    @NotBlank
    @Schema(description = "The node name", example = "rabbit@localhost")
    private String node;

    @NotBlank
    @Schema(description = "The queue type", example = "quorum")
    private String type;

    @NotBlank
    @Schema(description = "The virtual host name", example = "dbrepo")
    private String vhost;

}
