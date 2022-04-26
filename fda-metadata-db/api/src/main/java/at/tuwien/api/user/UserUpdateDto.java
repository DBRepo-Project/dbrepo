package at.tuwien.api.user;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateDto {

    @NotNull
    @Parameter(name = "titles before the first name")
    private String titlesBefore;

    @NotNull
    @Parameter(name = "titles after the last name")
    private String titlesAfter;

    @NotNull
    @Parameter(name = "first name")
    private String firstname;

    @NotNull
    @Parameter(name = "last name")
    private String lastname;

}
