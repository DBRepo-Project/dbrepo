package at.tuwien.api.document.record;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.util.Date;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmbargoDto {

    @NotNull(message = "embargo active is required")
    @Parameter(name = "embargo active")
    private Boolean active;

    @Parameter(name = "embargo until date")
    private Date until;

    @Parameter(name = "embargo explanation")
    private String reason;

}
