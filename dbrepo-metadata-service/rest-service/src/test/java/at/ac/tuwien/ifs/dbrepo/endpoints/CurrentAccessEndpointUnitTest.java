package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.database.AccessTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.AccessNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.gateway.KeycloakGateway;
import at.ac.tuwien.ifs.dbrepo.service.*;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CurrentAccessEndpointUnitTest extends BaseTest {
    private final DatabaseService databases = mock(DatabaseService.class);
    private final AccessService accesses = mock(AccessService.class);
    private final AccessEndpoint endpoint = new AccessEndpoint(accesses, Mappers.getMapper(MetadataMapper.class),
            databases, mock(KeycloakGateway.class), mock(DashboardService.class));

    @Test
    void ownerHasWriteAllWithoutAnExplicitGrant() throws Exception {
        when(databases.findById(DATABASE_1_ID)).thenReturn(DATABASE_1);
        final var body = endpoint.current(DATABASE_1_ID, USER_1_PRINCIPAL).getBody();
        assertEquals(AccessTypeDto.WRITE_ALL, body.getType());
        assertEquals(USER_1_USERNAME, body.getUser().getUsername());
        verifyNoInteractions(accesses);
    }

    @Test
    void memberReceivesOnlyOwnGrant() throws Exception {
        when(databases.findById(DATABASE_1_ID)).thenReturn(DATABASE_1);
        when(accesses.find(DATABASE_1, USER_2_USERNAME)).thenReturn(DATABASE_1_USER_2_READ_ACCESS);
        final var body = endpoint.current(DATABASE_1_ID, USER_2_PRINCIPAL).getBody();
        assertEquals(AccessTypeDto.READ, body.getType());
        assertEquals(USER_2_USERNAME, body.getUser().getUsername());
    }

    @Test
    void missingGrantIsRejected() throws Exception {
        when(databases.findById(DATABASE_1_ID)).thenReturn(DATABASE_1);
        when(accesses.find(DATABASE_1, USER_2_USERNAME)).thenThrow(new AccessNotFoundException("No access"));
        assertThrows(AccessNotFoundException.class, () -> endpoint.current(DATABASE_1_ID, USER_2_PRINCIPAL));
    }

    @Test
    void anonymousDoesNotReadDatabaseOrGrants() {
        assertThrows(NotAllowedException.class, () -> endpoint.current(DATABASE_1_ID, null));
        verifyNoInteractions(databases, accesses);
    }
}
