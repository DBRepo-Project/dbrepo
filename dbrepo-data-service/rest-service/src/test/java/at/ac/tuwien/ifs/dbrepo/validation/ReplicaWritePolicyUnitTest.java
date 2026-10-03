package at.ac.tuwien.ifs.dbrepo.validation;

import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;
import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReplicaWritePolicyUnitTest {

    private final EndpointValidator validator = new EndpointValidator();

    @Test
    void systemRoleCannotBypassPrimaryWriteLocation() {
        ReflectionTestUtils.setField(validator, "baseUrl", "https://local.example");
        final var principal = new UsernamePasswordAuthenticationToken("system", "",
                List.of(new SimpleGrantedAuthority("system")));
        final Database database = Database.builder().creationLocation("https://remote.example").build();

        assertThrows(NotAllowedException.class,
                () -> validator.validatePrimaryWriteLocation(database, Table.builder().build(), principal));
    }

    @Test
    void legacyDatabaseStillChecksTableOrigin() {
        ReflectionTestUtils.setField(validator, "baseUrl", "https://local.example");
        final Database database = Database.builder().build();

        assertThrows(NotAllowedException.class, () -> validator.validatePrimaryWriteLocation(database,
                Table.builder().creationLocation("https://remote.example").build(), null));
        assertDoesNotThrow(() -> validator.validatePrimaryWriteLocation(database, Table.builder().build(), null));
    }
}
