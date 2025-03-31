package at.ac.tuwien.ifs.dbrepo.core.api.amqp;

import com.fasterxml.jackson.annotation.JsonProperty;
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
