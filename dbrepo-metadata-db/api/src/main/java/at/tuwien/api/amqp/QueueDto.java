package at.tuwien.api.amqp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QueueDto {

    @NotNull
    @JsonProperty("auto_delete")
    private Boolean autoDelete;

    @NotNull
    private Boolean durable;

    @NotNull
    private Boolean exclusive;

    @NotBlank
    private String name;

    @NotBlank
    private String node;

    @NotBlank
    private String type;

    @NotBlank
    private String vhost;

}
