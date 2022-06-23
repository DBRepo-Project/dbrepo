package at.tuwien.api.document.links;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DoiDto {

    @Parameter(name = "client")
    private String client;

    @NotBlank(message = "identifier is required")
    @Parameter(name = "identifier")
    private String identifier;

    @Parameter(name = "provider")
    private String provider;

}
