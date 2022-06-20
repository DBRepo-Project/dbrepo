package at.tuwien.seeder.impl;

import at.tuwien.config.SecurityConfig;
import at.tuwien.entities.user.User;
import at.tuwien.repositories.UserRepository;
import at.tuwien.seeder.Seeder;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class UserSeederImpl extends AbstractSeeder implements Seeder {

    private final UserRepository userRepository;
    private final SecurityConfig securityConfig;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserSeederImpl(UserRepository userRepository, SecurityConfig securityConfig,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.securityConfig = securityConfig;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void seed() {
        if (userRepository.findAll().size() > 0) {
            log.warn("Already seeded. Skip.");
            return;
        }
        USER_1.setPassword(passwordEncoder.encode(securityConfig.getSystemPassword()));
        final User user1 = userRepository.save(USER_1);
        log.info("Seeded user with username {}", user1.getUsername());
        log.debug("seeded user {}", user1);
    }
}
