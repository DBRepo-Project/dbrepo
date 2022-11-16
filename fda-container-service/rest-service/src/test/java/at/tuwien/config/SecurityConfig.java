package at.tuwien.config;

import at.tuwien.BaseUnitTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import java.util.List;

@Configuration
public class SecurityConfig extends BaseUnitTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager(List.of(
                new User(USER_1_USERNAME, passwordEncoder.encode(USER_1_PASSWORD), List.of(USER_1_AUTHORITY)),
                new User(USER_2_USERNAME, passwordEncoder.encode(USER_2_PASSWORD), List.of(USER_2_AUTHORITY)),
                new User(USER_3_USERNAME, passwordEncoder.encode(USER_3_PASSWORD), List.of(USER_3_AUTHORITY)),
                new User(USER_4_USERNAME, passwordEncoder.encode(USER_4_PASSWORD), List.of())
        ));
    }

}
