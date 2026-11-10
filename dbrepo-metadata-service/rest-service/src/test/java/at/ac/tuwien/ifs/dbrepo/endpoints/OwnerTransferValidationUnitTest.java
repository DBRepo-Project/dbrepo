package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseTransferDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.service.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OwnerTransferValidationUnitTest extends BaseTest {
    private final DatabaseService databases = mock(DatabaseService.class);
    private final UserService users = mock(UserService.class);
    private final DashboardService dashboard = mock(DashboardService.class);
    private final DatabaseEndpoint endpoint = new DatabaseEndpoint(users, mock(MetadataMapper.class),
            mock(StorageService.class), databases, mock(ContainerService.class), dashboard);

    @Test
    void unknownTargetDoesNotChangeOwnershipOrDashboard() throws Exception {
        when(databases.findForUpdateById(DATABASE_1_ID)).thenReturn(DATABASE_1);
        when(users.findByUsername("unknown")).thenThrow(new UserNotFoundException("Not found"));
        assertThrows(UserNotFoundException.class, () -> endpoint.transfer(DATABASE_1_ID,
                DatabaseTransferDto.builder().username("unknown").build(), USER_1_PRINCIPAL));
        verify(databases, never()).modifyOwner(any(), any());
        verifyNoInteractions(dashboard);
    }

    @Test
    void nonOwnerCannotSelectATarget() throws Exception {
        when(databases.findForUpdateById(DATABASE_1_ID)).thenReturn(DATABASE_1);
        assertThrows(NotAllowedException.class, () -> endpoint.transfer(DATABASE_1_ID,
                DatabaseTransferDto.builder().username(USER_2_USERNAME).build(), USER_2_PRINCIPAL));
        verify(databases, never()).modifyOwner(any(), any());
        verifyNoInteractions(users, dashboard);
    }

    @Test
    void documentedErrorsCoverTransferExceptions() {
        new at.ac.tuwien.ifs.dbrepo.mvc.OpenApiEndpointMvcTest().openApiDocs_databaseEndpointApiResponses_succeeds();
    }
}
