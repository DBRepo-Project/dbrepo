package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.AuthServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.UserNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.gateway.KeycloakGateway;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class UserServicePersistenceTest extends BaseTest {

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
    public void findByUsername_fails() throws UserNotFoundException, NotAllowedException {

        /* mock */
        doThrow(UserNotFoundException.class)
                .when(keycloakGateway)
                .findByUsername(USER_2_USERNAME);

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            userService.findByUsername(USER_2_USERNAME);
        });
    }

    @Test
    public void findAll_succeeds() {
        final UserRepresentation mockUser1 = metadataMapper.userDtoToUserRepresentation(USER_1_DTO);
        mockUser1.setRealmRoles(Arrays.asList(DEFAULT_RESEARCHER_ROLES));
        final UserRepresentation mockUser2 = metadataMapper.userDtoToUserRepresentation(USER_2_DTO);
        mockUser2.setRealmRoles(Arrays.asList(DEFAULT_RESEARCHER_ROLES));

        /* mock */
        when(keycloakGateway.findAll())
                .thenReturn(List.of(mockUser1, mockUser2));

        /* test */
        final List<UserDto> response = userService.findAll();
        assertEquals(2, response.size());
    }

    @Test
    public void modify_succeeds() throws UserNotFoundException, AuthServiceException {
        final UserUpdateDto request = UserUpdateDto.builder()
                .firstname(USER_1_FIRSTNAME)
                .lastname(USER_1_LASTNAME)
                .affiliation("NASA")
                .orcid(null)
                .theme("dark")
                .language("de")
                .build();

        /* mock */
        doNothing()
                .when(keycloakGateway)
                .updateUser(USER_1_ID, request);

        /* test */
        final UserDto response = userService.modify(USER_1_DTO, request);
        assertEquals(USER_1_ID, response.getId());
        assertEquals(USER_1_FIRSTNAME, response.getFirstname());
        assertEquals(USER_1_LASTNAME, response.getLastname());
        assertEquals("dark", response.getAttributes().getTheme());
        assertEquals("de", response.getAttributes().getLanguage());
        assertEquals("NASA", response.getAttributes().getAffiliation());
        assertNull(response.getAttributes().getOrcid());
    }

    @Test
    public void findById_succeeds() throws UserNotFoundException, NotAllowedException {
        final UserRepresentation mockUser = metadataMapper.userDtoToUserRepresentation(USER_1_DTO);
        mockUser.setRealmRoles(Arrays.asList(DEFAULT_RESEARCHER_ROLES));

        /* mock */
        when(keycloakGateway.findById(USER_1_ID))
                .thenReturn(mockUser);

        /* test */
        final UserDto user = userService.findById(USER_1_ID);
        assertEquals(USER_1_USERNAME, user.getUsername());
    }

    @Test
    public void findById_notFound_fails() throws UserNotFoundException, NotAllowedException {

        /* mock */
        doThrow(UserNotFoundException.class)
                .when(keycloakGateway)
                .findById(USER_2_ID);

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            userService.findById(USER_2_ID);
        });
    }
}
