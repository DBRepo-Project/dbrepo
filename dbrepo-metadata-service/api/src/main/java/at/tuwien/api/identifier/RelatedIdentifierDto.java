package at.tuwien.api.identifier;

import at.tuwien.api.user.UserDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotNull;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.annotation.Id;

import java.time.Instant;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class RelatedIdentifierDto {

    @NotNull
    @Schema(example = "8")
    private Long id;

    @NotNull
    @Schema(example = "10.70124/dc4zh-9ce78")
    private String value;

    @NotNull
    @Schema(example = "DOI")
    private RelatedTypeDto type;

    @NotNull
    @Schema(example = "Cites")
    private RelationTypeDto relation;

}


