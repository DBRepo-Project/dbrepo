package at.tuwien.config;

import at.tuwien.entities.user.RoleType;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class AuthenticationConfig {

    @Value("${fda.token.max}")
    private Integer tokenCount;

    @Value("${fda.default_roles}")
    private RoleType[] defaultRoles;

    @Value("${fda.superusers}")
    private String[] superUsers;

}
