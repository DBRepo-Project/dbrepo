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
    private Boolean ackRequired;

    @NotNull
    private Boolean active;

    @NotNull
    @JsonProperty("activity_status")
    private String activityStatus;

    @NotNull
    @JsonProperty("channel_details")
    private ChannelDetailsDto channelDetails;

    @NotNull
    @JsonProperty("consumer_tag")
    private String consumerTag;

    @NotNull
    private Boolean exclusive;

    @NotNull
    @JsonProperty("prefetch_count")
    private Integer prefetchCount;

    @NotNull
    private QueueBriefDto queue;

}
