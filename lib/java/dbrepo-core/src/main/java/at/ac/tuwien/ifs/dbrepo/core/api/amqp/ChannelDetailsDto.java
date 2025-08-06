package at.ac.tuwien.ifs.dbrepo.core.api.amqp;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class ChannelDetailsDto {

    @NotNull
    @JsonProperty("connection_name")
    private String connectionName;

    @NotNull
    @Schema(description = "The channel name", example = "127.0.0.1:52956 -> 127.0.0.1:5672")
    private String name;

    @NotNull
    @Schema(description = "The node name", example = "127.0.0.1:52956")
    private String node;

    @NotNull
    @JsonProperty("number")
    @Schema(description = "The number of the channel", example = "1")
    private Integer number;

    @NotNull
    @JsonProperty("peer_host")
    @Schema(description = "The peer hostname", example = "localhost")
    private String peerHost;

    @NotNull
    @JsonProperty("peer_port")
    @Schema(description = "The peer port", example = "52956")
    private Integer peerPort;

    @NotNull
    @Schema(description = "The channel user", example = "rabbit")
    private String user;

}
