package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.ReplicationOwnerDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;
import at.ac.tuwien.ifs.dbrepo.metadata.ReplicationIdentityMappingRepository;
import at.ac.tuwien.ifs.dbrepo.metadata.entity.ReplicationIdentityMapping;
import at.ac.tuwien.ifs.dbrepo.service.impl.ReplicationIdentityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReplicationIdentityServiceUnitTest {

    @Mock
    private ReplicationIdentityMappingRepository repository;

    @Mock
    private UserService userService;

    private ReplicationIdentityServiceImpl service;

    @BeforeEach
    public void beforeEach() {
        service = new ReplicationIdentityServiceImpl(repository, userService);
        ReflectionTestUtils.setField(service, "localIssuer", "https://identity.example/realms/dbrepo/");
    }

    @Test
    public void resolve_sameIssuer_resolvesLocalSubject() throws Exception {
        final UUID subject = UUID.randomUUID();
        final UserDto localUser = UserDto.builder().id(subject).username("alice").build();
        when(userService.findById(subject)).thenReturn(localUser);

        final Optional<UserDto> result = service.resolve(owner("https://origin.example/", subject.toString(),
                "https://identity.example/realms/dbrepo"));

        assertTrue(result.isPresent());
        assertEquals("alice", result.get().getUsername());
        verify(repository).findByOriginSiteAndOriginIssuerAndOriginSubject(
                "https://origin.example", "https://identity.example/realms/dbrepo", subject.toString());
    }

    @Test
    public void resolve_explicitMappingOverridesSameIssuerUser() throws Exception {
        final String subject = UUID.randomUUID().toString();
        final ReplicationIdentityMapping mapping = ReplicationIdentityMapping.builder()
                .localUsername("bob")
                .build();
        when(repository.findByOriginSiteAndOriginIssuerAndOriginSubject(
                "https://origin.example", "https://identity.example/realms/dbrepo", subject))
                .thenReturn(Optional.of(mapping));
        when(userService.findByUsername("bob"))
                .thenReturn(UserDto.builder().username("bob").build());

        final Optional<UserDto> result = service.resolve(owner("https://origin.example", subject,
                "https://identity.example/realms/dbrepo"));

        assertTrue(result.isPresent());
        assertEquals("bob", result.get().getUsername());
        verify(userService, never()).findById(UUID.fromString(subject));
    }

    @Test
    public void resolve_differentIssuer_usesVerifiedMapping() throws Exception {
        final String subject = UUID.randomUUID().toString();
        final ReplicationIdentityMapping mapping = ReplicationIdentityMapping.builder()
                .originSite("https://origin.example")
                .originIssuer("https://remote-identity.example/realms/dbrepo")
                .originSubject(subject)
                .localUsername("alice")
                .build();
        final UserDto localUser = UserDto.builder().username("alice").build();
        when(repository.findByOriginSiteAndOriginIssuerAndOriginSubject(
                "https://origin.example", "https://remote-identity.example/realms/dbrepo", subject))
                .thenReturn(Optional.of(mapping));
        when(userService.findByUsername("alice")).thenReturn(localUser);

        final Optional<UserDto> result = service.resolve(owner("https://origin.example/", subject,
                "https://remote-identity.example/realms/dbrepo/"));

        assertTrue(result.isPresent());
        assertEquals("alice", result.get().getUsername());
    }

    @Test
    public void map_newIdentity_persistsNormalizedMapping() throws Exception {
        final String subject = UUID.randomUUID().toString();
        when(repository.findByOriginSiteAndOriginIssuerAndOriginSubject(
                "https://origin.example", "https://remote-identity.example/realms/dbrepo", subject))
                .thenReturn(Optional.empty());

        service.map(owner("https://origin.example/", subject,
                "https://remote-identity.example/realms/dbrepo/"), "alice");

        final ArgumentCaptor<ReplicationIdentityMapping> captor =
                ArgumentCaptor.forClass(ReplicationIdentityMapping.class);
        verify(repository).save(captor.capture());
        assertEquals("https://origin.example", captor.getValue().getOriginSite());
        assertEquals("https://remote-identity.example/realms/dbrepo", captor.getValue().getOriginIssuer());
        assertEquals(subject, captor.getValue().getOriginSubject());
        assertEquals("alice", captor.getValue().getLocalUsername());
    }

    @Test
    public void map_existingIdentityToDifferentUser_updatesMapping() throws Exception {
        final String subject = UUID.randomUUID().toString();
        final ReplicationIdentityMapping mapping = ReplicationIdentityMapping.builder()
                .localUsername("alice")
                .build();
        when(repository.findByOriginSiteAndOriginIssuerAndOriginSubject(
                "https://origin.example", "https://remote-identity.example/realms/dbrepo", subject))
                .thenReturn(Optional.of(mapping));

        service.map(owner("https://origin.example", subject,
                "https://remote-identity.example/realms/dbrepo"), "bob");

        assertEquals("bob", mapping.getLocalUsername());
        verify(repository).save(mapping);
    }

    private ReplicationOwnerDto owner(String site, String subject, String issuer) {
        return ReplicationOwnerDto.builder()
                .siteUrl(site)
                .issuer(issuer)
                .subject(subject)
                .username("remote-alice")
                .build();
    }
}
