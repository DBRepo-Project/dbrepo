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
public class ConsumerDto {

    @NotNull
    @JsonProperty("ack_required")
    @Parameter(name = "acknowledge required")
    private Boolean ackRequired;

    @NotNull
    @Parameter(name = "active")
    private Boolean active;

    @NotNull
    @JsonProperty("activity_status")
    @Parameter(name = "activity status")
    private String activityStatus;

    @NotNull
    @JsonProperty("channel_details")
    @Parameter(name = "channel details")
    private ChannelDetailsDto channelDetails;

    @NotNull
    @JsonProperty("consumer_tag")
    @Parameter(name = "consumer tag")
    private String consumerTag;

    @NotNull
    @Parameter(name = "exclusive")
    private Boolean exclusive;

    @NotNull
    @JsonProperty("prefetch_count")
    @Parameter(name = "prefetch count")
    private Integer prefetchCount;

    @NotNull
    @Parameter(name = "queue")
    private QueueBriefDto queue;

}
