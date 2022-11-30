package at.tuwien.api.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class GrantedAuthorityDto {

    @Schema(example = "ROLE_RESEARCHER")
    private String authority;



}
