package at.tuwien.api.identifier;

import com.fasterxml.jackson.annotation.JsonFormat;
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

    @NotBlank
    @Parameter(name = "name", example = "Mustermann, Maximilian")
    private String name;

    @Parameter(name = "affiliation", example = "TU Wien")
    private String affiliation;

    @Parameter(name = "orcid", example = "ORCID")
    private String orcid;

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Europe/Vienna")
    private Instant created;

    @JsonProperty("last_modified")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Europe/Vienna")
    private Instant lastModified;

}
