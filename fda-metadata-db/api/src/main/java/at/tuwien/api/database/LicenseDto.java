package at.tuwien.api.database;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LicenseDto {

    @NotNull
    @Parameter(name = "license identifier")
    private String identifier;

    @NotBlank
    @Parameter(name = "license uri")
    private String uri;

}