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
public class ConsumerDto {

    @NotNull
    @JsonProperty("ack_required")
    @Schema(description = "Consumer requires acknowledgement", example = "true")
    private Boolean ackRequired;

    @NotNull
    @Schema(description = "The state of the consumer", example = "true")
    private Boolean active;

    @NotNull
    @JsonProperty("activity_status")
    @Schema(description = "The activity status", example = "up")
    private String activityStatus;

    @NotNull
    @JsonProperty("channel_details")
    private ChannelDetailsDto channelDetails;

    @NotNull
    @JsonProperty("consumer_tag")
    @Schema(description = "The consumer tag", example = "amq.ctag-AMq0sbj9sS-WWN3r-Avn8A")
    private String consumerTag;

    @NotNull
    @Schema(description = "The exclusivity of the queue, if set to true, the queue can only be used by the declaring connection", example = "false")
    private Boolean exclusive;

    @NotNull
    @JsonProperty("prefetch_count")
    @Schema(description = "The number unacknowledged messages on a channel when consuming", example = "250")
    private Integer prefetchCount;

    @NotNull
    private QueueBriefDto queue;

}
