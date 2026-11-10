package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.service.DatabaseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DatabasePreviewVisibilityUnitTest extends BaseTest {
    private final DatabaseService databases = mock(DatabaseService.class);
    private final DatabaseEndpoint endpoint = new DatabaseEndpoint(null, null, null, databases, null, null, null, null, null);

    private Principal principal(String role) {
        return switch (role) {
            case "owner" -> USER_1_PRINCIPAL;
            case "member" -> USER_2_PRINCIPAL;
            case "system" -> USER_LOCAL_ADMIN_PRINCIPAL;
            case "unrelated" -> USER_4_PRINCIPAL;
            default -> null;
        };
    }

    @ParameterizedTest
    @ValueSource(strings = {"anonymous", "owner", "member", "unrelated", "system"})
    void privateImageUsesCurrentAccessAndCannotBeStored(String role) throws Exception {
        DATABASE_1.setIsPublic(false);
        DATABASE_1.setIsSchemaPublic(false);
        when(databases.findById(DATABASE_1_ID)).thenReturn(DATABASE_1);
        if (role.equals("anonymous") || role.equals("unrelated")) {
            assertThrows(NotAllowedException.class, () -> endpoint.findPreviewImage(DATABASE_1_ID, principal(role)));
        } else {
            var response = endpoint.findPreviewImage(DATABASE_1_ID, principal(role));
            assertArrayEquals(DATABASE_1.getImage(), response.getBody());
            assertEquals("no-store", response.getHeaders().getCacheControl());
            assertTrue(response.getHeaders().getVary().containsAll(List.of("Authorization", "Cookie")));
        }
    }

    @Test
    void membershipAndVisibilityChangesAreCheckedOnEveryRead() throws Exception {
        when(databases.findById(DATABASE_1_ID)).thenReturn(DATABASE_1);
        DATABASE_1.setIsPublic(true);
        endpoint.findPreviewImage(DATABASE_1_ID, null);
        DATABASE_1.setIsPublic(false);
        DATABASE_1.setIsSchemaPublic(false);
        assertThrows(NotAllowedException.class, () -> endpoint.findPreviewImage(DATABASE_1_ID, null));
        endpoint.findPreviewImage(DATABASE_1_ID, USER_2_PRINCIPAL);
        DATABASE_1.setAccesses(List.of());
        assertThrows(NotAllowedException.class, () -> endpoint.findPreviewImage(DATABASE_1_ID, USER_2_PRINCIPAL));
        endpoint.findPreviewImage(DATABASE_1_ID, USER_1_PRINCIPAL);
        DATABASE_1.setIsSchemaPublic(true);
        endpoint.findPreviewImage(DATABASE_1_ID, null);
    }
}
