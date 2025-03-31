package at.ac.tuwien.ifs.dbrepo.core.api.amqp;

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
