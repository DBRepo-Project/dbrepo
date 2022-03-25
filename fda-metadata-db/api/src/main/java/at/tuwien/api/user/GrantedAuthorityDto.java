package at.tuwien.api.user;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GrantedAuthorityDto {

    @Parameter(name = "authority name")
    private String authority;



}
