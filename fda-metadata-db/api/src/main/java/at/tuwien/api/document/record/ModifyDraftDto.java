package at.tuwien.api.document.record;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModifyDraftDto {

    @NotNull(message = "identifier is required")
    @Parameter(name = "identifier", description = "Identifier of the record, e.g. 4d0ns-ntd89")
    private String id;

}
