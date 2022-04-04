package at.tuwien.api.identifier;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Instant;

@Data
@Getter
@Setter
@Builder
public class CreatorDto {

    @NotNull
    private Long id;

    @NotNull
    private Long pid;

    @NotBlank
    @Parameter(name = "query title", example = "Maximilian")
    private String firstname;

    @NotBlank
    @Parameter(name = "lastname", example = "Mustermann")
    private String lastname;

    @NotNull
    private Instant created;

    @NotNull
    @JsonProperty("last_modified")
    private Instant lastModified;

}
