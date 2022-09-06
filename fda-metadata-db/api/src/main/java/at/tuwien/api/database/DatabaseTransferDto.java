package at.tuwien.api.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DatabaseTransferDto {

    @NotNull
    @JsonProperty("is_public")
    @Parameter(name = "database publicity", example = "true")
    private Boolean isPublic;

}
