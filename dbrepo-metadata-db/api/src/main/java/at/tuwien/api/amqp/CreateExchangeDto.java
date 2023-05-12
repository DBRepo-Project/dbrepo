package at.tuwien.api.amqp;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class CreateExchangeDto {

    @NotNull
    @JsonProperty("auto_delete")
    private Boolean autoDelete;

    @NotNull
    private Boolean durable;

    @NotNull
    private Boolean internal;

    @NotBlank
    private String type;

}
