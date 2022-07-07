package at.tuwien.api.identifier;

import at.tuwien.api.user.UserDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.time.Instant;

@Data
@Getter
@Setter
@Builder
public class RelatedIdentifierDto {

    @NotNull
    private Long id;

    @JsonIgnore
    @NotNull
    private Long iid;

    @NotNull
    @Parameter(name = "identifier", example = "10.70124/dc4zh-9ce78")
    private String value;

    @Parameter(name = "type", example = "DOI")
    private RelatedTypeDto type;

    @Parameter(name = "relation", example = "Cites")
    private RelationTypeDto relation;

    @ToString.Exclude
    @JsonIgnore
    @NotNull
    private UserDto creator;

    @NotNull
    private Instant created;

    @JsonProperty("last_modified")
    private Instant lastModified;

    private Instant deleted;

}


