package at.tuwien.api.amqp;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
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
