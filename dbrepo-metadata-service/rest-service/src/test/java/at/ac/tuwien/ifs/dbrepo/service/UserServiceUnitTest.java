package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.AuthServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.UserNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.gateway.KeycloakGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class UserServiceUnitTest extends BaseTest {

    @MockitoBean
    private KeycloakGateway keycloakGateway;

    @Autowired
    private UserService userService;

    @Autowired
    private MetadataMapper metadataMapper;

    @Test
    public void findByUsername_succeeds() throws UserNotFoundException, NotAllowedException {
        final UserRepresentation mockUser = metadataMapper.userDtoToUserRepresentation(USER_1_DTO);
        mockUser.setRealmRoles(Arrays.asList(DEFAULT_RESEARCHER_ROLES));

        /* mock */
        when(keycloakGateway.findByUsername(USER_1_USERNAME))
                .thenReturn(mockUser);

        /* test */
        final UserDto response = userService.findByUsername(USER_1_USERNAME);
        assertEquals(USER_1_ID, response.getId());
        assertEquals(USER_1_USERNAME, response.getUsername());
    }

    @Test
    public void findById_succeeds() throws UserNotFoundException, NotAllowedException {
        final UserRepresentation mockUser = metadataMapper.userDtoToUserRepresentation(USER_1_DTO);
        mockUser.setRealmRoles(Arrays.asList(DEFAULT_RESEARCHER_ROLES));

        /* mock */
        when(keycloakGateway.findById(USER_1_ID))
                .thenReturn(mockUser);

        /* test */
        final UserDto response = userService.findById(USER_1_ID);
        assertEquals(USER_1_ID, response.getId());
        assertEquals(USER_1_USERNAME, response.getUsername());
    }

    @Test
    public void findAll_succeeds() {
        final UserRepresentation mockUser1 = new UserRepresentation();
        mockUser1.setId(USER_1_ID.toString());
        mockUser1.setUsername(USER_1_USERNAME);
        final UserRepresentation mockUser2 = new UserRepresentation();
        mockUser2.setId(USER_2_ID.toString());
        mockUser2.setUsername(USER_2_USERNAME);

        /* mock */
        when(keycloakGateway.findAll())
                .thenReturn(List.of(mockUser1, mockUser2));

        /* test */
        final List<UserDto> response = userService.findAll();
        assertEquals(2, response.size());
    }

    @Test
    public void modify_succeeds() throws UserNotFoundException, AuthServiceException, NotAllowedException {
        final UserRepresentation mockUser = new UserRepresentation();
        mockUser.setId(USER_1_ID.toString());
        mockUser.setUsername(USER_1_USERNAME);

        /* mock */
        when(keycloakGateway.findById(USER_1_ID))
                .thenReturn(mockUser);
        doNothing()
                .when(keycloakGateway)
                .updateUser(any(UUID.class), any(UserUpdateDto.class));
        doNothing()
                .when(keycloakGateway)
                .updateUser(any(UUID.class), any(UserUpdateDto.class));

        /* test */
        final UserDto response = userService.modify(USER_1_DTO, USER_1_UPDATE_DTO);
        assertEquals(USER_1_ID, response.getId());
        assertEquals(USER_1_USERNAME, response.getUsername());
    }

    @Test
    public void findByUsername_fails() throws UserNotFoundException, NotAllowedException {

        /* mock */
        doThrow(UserNotFoundException.class)
                .when(keycloakGateway)
                .findByUsername(USER_1_USERNAME);

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            userService.findByUsername(USER_1_USERNAME);
        });
    }

    @Test
    public void find_fails() throws UserNotFoundException, NotAllowedException {

        /* mock */
        doThrow(UserNotFoundException.class)
                .when(keycloakGateway)
                .findById(USER_1_ID);

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            userService.findById(USER_1_ID);
        });
    }


}
