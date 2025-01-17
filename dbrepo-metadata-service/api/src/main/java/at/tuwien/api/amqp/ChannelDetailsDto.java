package at.tuwien.api.amqp;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    private String name;

    @NotNull
    private String node;

    @NotNull
    @JsonProperty("number")
    private Integer number;

    @NotNull
    @JsonProperty("peer_host")
    private String peerHost;

    @NotNull
    @JsonProperty("peer_port")
    private Integer peerPort;

    @NotNull
    private String user;

}
