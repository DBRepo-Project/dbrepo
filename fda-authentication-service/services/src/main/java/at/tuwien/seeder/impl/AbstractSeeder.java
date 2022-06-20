package at.tuwien.seeder.impl;

import at.tuwien.entities.user.RoleType;
import at.tuwien.entities.user.User;

import java.util.List;

public abstract class AbstractSeeder {

    public static final String USER_1_USERNAME = "system";
    public static final String USER_1_EMAIL = "noreply@dbrepo.ossdip.at";

    public static final List<RoleType> USER_1_ROLES = List.of(RoleType.ROLE_RESEARCHER);

    public static final User USER_1 = User.builder()
            .username(USER_1_USERNAME)
            .email(USER_1_EMAIL)
            .roles(USER_1_ROLES)
            .build();
}
