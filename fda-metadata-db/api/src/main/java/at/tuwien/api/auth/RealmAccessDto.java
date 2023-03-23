package at.tuwien.api.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RealmAccessDto {

    @NotNull
    @Schema(description = "list of roles associated to the user", example = "[\"create-container\",\"create-database\"]")
    private String[] roles;



}
