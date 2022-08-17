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
public class ChannelDetailsDto {

    @NotNull
    @JsonProperty("connection_name")
    @Parameter(name = "connection name")
    private String connectionName;

    @NotNull
    @Parameter(name = "channel name")
    private String name;

    @NotNull
    @Parameter(name = "channel node")
    private String node;

    @NotNull
    @JsonProperty("number")
    @Parameter(name = "channel number")
    private Integer number;

    @NotNull
    @JsonProperty("peer_host")
    @Parameter(name = "channel peer host")
    private String peerHost;

    @NotNull
    @JsonProperty("peer_port")
    @Parameter(name = "channel peer port")
    private Integer peerPort;

    @NotNull
    @Parameter(name = "channel user")
    private String user;

}
