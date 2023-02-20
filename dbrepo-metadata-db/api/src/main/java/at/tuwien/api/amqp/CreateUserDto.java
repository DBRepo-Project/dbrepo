package at.tuwien.api.amqp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateUserDto {

    @NotNull
    @ToString.Exclude
    private String password;

    @Schema(example = "administrator")
    private String tags;

}
