package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.cache.DatabaseCacheRepository;
import at.ac.tuwien.ifs.dbrepo.core.api.database.*;
import at.ac.tuwien.ifs.dbrepo.core.api.grafana.PermissionTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.grafana.UpdateDashboardAccessDto;
import org.mockito.ArgumentCaptor;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.AccessType;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.DatabaseAccess;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.View;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.gateway.DataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.gateway.KeycloakGateway;
import at.ac.tuwien.ifs.dbrepo.gateway.SearchServiceGateway;
import at.ac.tuwien.ifs.dbrepo.metadata.ContainerRepository;
import at.ac.tuwien.ifs.dbrepo.metadata.DatabaseRepository;
import at.ac.tuwien.ifs.dbrepo.metadata.LicenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import org.springframework.security.concurrent.DelegatingSecurityContextCallable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@WithMockUser(username = BaseTest.USER_1_USERNAME, authorities = {
        "create-database-access", "delete-database-access", "modify-database-visibility", "modify-database-owner", "modify-database-image", "modify-view-visibility"})
class DashboardAccessPersistenceTest extends BaseTest {

    @Autowired private AccessEndpoint accessEndpoint;
    @Autowired private DatabaseEndpoint databaseEndpoint;
    @Autowired private ViewEndpoint viewEndpoint;
    @Autowired private DatabaseRepository databases;
    @Autowired private ContainerRepository containers;
    @Autowired private LicenseRepository licenses;
    @Autowired private PlatformTransactionManager transactions;
    @MockitoBean private DataServiceGateway dataGateway;
    @MockitoBean private SearchServiceGateway searchGateway;
    @MockitoBean private DatabaseCacheRepository cache;
    @MockitoBean private KeycloakGateway keycloak;
    @MockitoBean @Qualifier("dashboardServiceRestTemplate") private RestTemplate dashboardHttp;

    @BeforeEach
    void prepare() throws Exception {
        databases.deleteAll();
        CONTAINER_1.getDatabases().clear();
        DATABASE_1.getTables().clear();
        DATABASE_1.getViews().clear();
        DATABASE_1.getIdentifiers().clear();
        DATABASE_1.getSubsets().clear();
        DATABASE_1.getAccesses().clear();
        licenses.save(LICENSE_1);
        containers.save(CONTAINER_1);
        databases.saveAndFlush(DATABASE_1.toBuilder().accesses(new LinkedList<>()).isPublic(false)
                .isSchemaPublic(false).isDashboardEnabled(false).build());
        final UserRepresentation user = new UserRepresentation();
        user.setId(USER_2_ID.toString());
        user.setUsername(USER_2_USERNAME);
        user.setAttributes(java.util.Map.of());
        user.setRealmRoles(List.of());
        when(keycloak.findByUsername(USER_2_USERNAME)).thenReturn(user);
        when(dashboardHttp.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.accepted().build());
        when(dashboardHttp.exchange(anyString(), eq(HttpMethod.HEAD), any(HttpEntity.class), eq(Void.class), anyString()))
                .thenReturn(ResponseEntity.noContent().build());
    }

    @Test
    void missingGrafanaAccountCommitsGrantAndSameRequestRetriesOnlyGrafana() throws Exception {
        when(dashboardHttp.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Missing Grafana account",
                        HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8))
                .thenReturn(ResponseEntity.accepted().build());
        final CreateAccessDto request = CreateAccessDto.builder().type(AccessTypeDto.READ).build();

        assertThrows(DashboardServiceException.class, () -> accessEndpoint.create(DATABASE_1_ID, USER_2_USERNAME,
                request, USER_1_PRINCIPAL));
        assertEquals(1, memberGrants()); // Separate transaction observes the committed first request.

        // Grafana account is now available; repeating POST must not duplicate the SQL grant or row.
        assertEquals(HttpStatus.ACCEPTED, accessEndpoint.create(DATABASE_1_ID, USER_2_USERNAME,
                request, USER_1_PRINCIPAL).getStatusCode());
        assertEquals(1, memberGrants());
        verify(dataGateway, times(1)).createAccess(DATABASE_1_ID, USER_2_USERNAME, AccessTypeDto.READ);
        verify(searchGateway, times(1)).update(any(Database.class));
        verify(dashboardHttp, times(2)).exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));
    }

    static Stream<Object[]> ownerGrants() {
        return Stream.of(AccessTypeDto.READ, AccessTypeDto.WRITE_ALL).flatMap(type ->
                Stream.of(false, true).map(failFirst -> new Object[]{type, failFirst}));
    }

    @ParameterizedTest
    @MethodSource("ownerGrants")
    void transferredOwnerSelfGrantKeepsAdminAndRetriesAfterCommittedFailure(AccessTypeDto type, boolean failFirst)
            throws Exception {
        databaseEndpoint.transfer(DATABASE_1_ID, DatabaseTransferDto.builder().username(USER_2_USERNAME).build(),
                USER_1_PRINCIPAL);
        assertEquals(1, memberGrants());
        // Keep the legacy owner-without-grant scenario for installations created before transfer granted access.
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            final Database database = databases.findById(DATABASE_1_ID).orElseThrow();
            database.getAccesses().removeIf(access -> USER_2_USERNAME.equals(access.getUsername()));
            databases.saveAndFlush(database);
        });
        assertEquals(0, memberGrants());
        clearInvocations(dataGateway, searchGateway, dashboardHttp);
        if (failFirst) {
            when(dashboardHttp.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                    .thenThrow(HttpServerErrorException.create(HttpStatus.BAD_GATEWAY, "Synthetic outage",
                            HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8))
                    .thenReturn(ResponseEntity.accepted().build());
        }
        final CreateAccessDto request = CreateAccessDto.builder().type(type).build();
        if (failFirst) {
            assertThrows(DashboardServiceConnectionException.class, () ->
                    accessEndpoint.create(DATABASE_1_ID, USER_2_USERNAME, request, USER_2_PRINCIPAL));
            assertEquals(1, memberGrants());
        }
        assertEquals(HttpStatus.ACCEPTED, accessEndpoint.create(DATABASE_1_ID, USER_2_USERNAME,
                request, USER_2_PRINCIPAL).getStatusCode());
        assertEquals(1, memberGrants());
        verify(dataGateway, times(1)).createAccess(DATABASE_1_ID, USER_2_USERNAME, type);
        verify(searchGateway, times(1)).update(any(Database.class));
        final ArgumentCaptor<HttpEntity> sent = ArgumentCaptor.forClass(HttpEntity.class);
        verify(dashboardHttp, times(failFirst ? 2 : 1)).exchange(anyString(), eq(HttpMethod.PUT), sent.capture(), eq(Void.class));
        for (HttpEntity<?> entity : sent.getAllValues()) {
            assertEquals(PermissionTypeDto.ADMIN, ((UpdateDashboardAccessDto) entity.getBody()).getPermission());
        }
    }

    @Test
    void failedGrafanaRevokeCommitsDeletionAndRetryDoesNotRepeatSql() throws Exception {
        seedAccess(USER_2_USERNAME, AccessType.READ);
        when(dashboardHttp.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(HttpServerErrorException.create(HttpStatus.BAD_GATEWAY, "Synthetic outage",
                        HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8))
                .thenReturn(ResponseEntity.accepted().build());

        assertThrows(DashboardServiceConnectionException.class, () ->
                accessEndpoint.revoke(DATABASE_1_ID, USER_2_USERNAME, USER_1_PRINCIPAL));
        assertEquals(0, memberGrants());
        assertEquals(HttpStatus.ACCEPTED,
                accessEndpoint.revoke(DATABASE_1_ID, USER_2_USERNAME, USER_1_PRINCIPAL).getStatusCode());
        assertEquals(0, memberGrants());
        verify(dataGateway, times(1)).deleteAccess(DATABASE_1_ID, USER_2_USERNAME);
        verify(dashboardHttp, times(2)).exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));
    }

    @Test
    void ownerDifferentTypeAndProtectedSystemRetriesRemainRejected() throws Exception {
        seedAccess(USER_1_USERNAME, AccessType.WRITE_ALL);
        final CreateAccessDto request = CreateAccessDto.builder().type(AccessTypeDto.WRITE_ALL).build();
        assertThrows(NotAllowedException.class, () ->
                accessEndpoint.create(DATABASE_1_ID, USER_1_USERNAME,
                        CreateAccessDto.builder().type(AccessTypeDto.READ).build(), USER_1_PRINCIPAL));
        seedAccess(USER_2_USERNAME, AccessType.WRITE_ALL);
        final UserRepresentation system = new UserRepresentation();
        system.setUsername(USER_2_USERNAME);
        system.setRealmRoles(List.of("system"));
        when(keycloak.findByUsername(USER_2_USERNAME)).thenReturn(system);
        assertThrows(NotAllowedException.class, () ->
                accessEndpoint.create(DATABASE_1_ID, USER_2_USERNAME, request, USER_1_PRINCIPAL));
        verifyNoInteractions(dataGateway, dashboardHttp);
    }

    @Test
    void existingDifferentGrantRemainsRejected() {
        seedAccess(USER_2_USERNAME, AccessType.READ);
        assertThrows(NotAllowedException.class, () -> accessEndpoint.create(DATABASE_1_ID, USER_2_USERNAME,
                CreateAccessDto.builder().type(AccessTypeDto.WRITE_ALL).build(), USER_1_PRINCIPAL));
        verifyNoInteractions(dataGateway, dashboardHttp);
    }

    @Test
    void missingOwnerPreventsPrivateActivationBeforePersistenceAndCanRetry() throws Exception {
        when(dashboardHttp.exchange(anyString(), eq(HttpMethod.HEAD), any(HttpEntity.class), eq(Void.class), anyString()))
                .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Missing owner",
                        HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8))
                .thenReturn(ResponseEntity.noContent().build());
        final DatabaseModifyVisibilityDto request = privateEnabled();
        assertThrows(DashboardServiceException.class, () ->
                databaseEndpoint.visibility(DATABASE_1_ID, request, USER_1_PRINCIPAL));
        assertFalse(databases.findById(DATABASE_1_ID).orElseThrow().getIsDashboardEnabled());
        verifyNoInteractions(searchGateway);
        databaseEndpoint.visibility(DATABASE_1_ID, request, USER_1_PRINCIPAL);
        assertTrue(databases.findById(DATABASE_1_ID).orElseThrow().getIsDashboardEnabled());
        verify(searchGateway, times(1)).update(any(Database.class));
    }

    @Test
    void dashboardFailureRollsBackActivationAndRetryUpdatesVisibilityOnce() throws Exception {
        when(dashboardHttp.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE, "Outage",
                        HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8))
                .thenReturn(ResponseEntity.accepted().build());
        assertThrows(DashboardServiceConnectionException.class, () ->
                databaseEndpoint.visibility(DATABASE_1_ID, privateEnabled(), USER_1_PRINCIPAL));
        assertFalse(databases.findById(DATABASE_1_ID).orElseThrow().getIsDashboardEnabled());
        databaseEndpoint.visibility(DATABASE_1_ID, privateEnabled(), USER_1_PRINCIPAL);
        assertTrue(databases.findById(DATABASE_1_ID).orElseThrow().getIsDashboardEnabled());
        verify(searchGateway, times(2)).update(any(Database.class));
    }

    @Test
    void dashboardFailureKeepsFormerOwnerAbleToRetryTransfer() throws Exception {
        when(dashboardHttp.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE, "Outage",
                        HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8))
                .thenReturn(ResponseEntity.accepted().build());
        final DatabaseTransferDto request = DatabaseTransferDto.builder().username(USER_2_USERNAME).build();
        assertThrows(DashboardServiceConnectionException.class, () ->
                databaseEndpoint.transfer(DATABASE_1_ID, request, USER_1_PRINCIPAL));
        assertEquals(USER_1_USERNAME, databases.findById(DATABASE_1_ID).orElseThrow().getOwnedBy());
        databaseEndpoint.transfer(DATABASE_1_ID, request, USER_1_PRINCIPAL);
        assertEquals(USER_2_USERNAME, databases.findById(DATABASE_1_ID).orElseThrow().getOwnedBy());
        verify(dashboardHttp, times(2)).exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"grant", "image"})
    void concurrentFormerOwnerMutationWaitsForTransferAndRechecksOwner(String operation) throws Exception {
        final CountDownLatch transferAtGrafana = new CountDownLatch(1);
        final CountDownLatch releaseTransfer = new CountDownLatch(1);
        final CountDownLatch grantStarted = new CountDownLatch(1);
        when(dashboardHttp.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenAnswer(call -> {
                    transferAtGrafana.countDown();
                    assertTrue(releaseTransfer.await(10, TimeUnit.SECONDS));
                    return ResponseEntity.accepted().build();
                });
        try (ExecutorService workers = Executors.newFixedThreadPool(2)) {
            final Future<?> transfer = workers.submit(new DelegatingSecurityContextCallable<>(() ->
                    databaseEndpoint.transfer(DATABASE_1_ID,
                            DatabaseTransferDto.builder().username(USER_2_USERNAME).build(), USER_1_PRINCIPAL)));
            try {
                assertTrue(transferAtGrafana.await(10, TimeUnit.SECONDS));
                final Future<?> grant = workers.submit(new DelegatingSecurityContextCallable<>(() -> {
                    grantStarted.countDown();
                    if (operation.equals("image")) {
                        return databaseEndpoint.modifyImage(DATABASE_1_ID,
                                DatabaseModifyImageDto.builder().build(), USER_1_PRINCIPAL);
                    }
                    return accessEndpoint.create(DATABASE_1_ID, USER_2_USERNAME,
                            CreateAccessDto.builder().type(AccessTypeDto.READ).build(), USER_1_PRINCIPAL);
                }));
                assertTrue(grantStarted.await(10, TimeUnit.SECONDS));
                assertThrows(TimeoutException.class, () -> grant.get(250, TimeUnit.MILLISECONDS));
                releaseTransfer.countDown();
                transfer.get(10, TimeUnit.SECONDS);
                ExecutionException denied = null;
                try {
                    grant.get(10, TimeUnit.SECONDS);
                } catch (ExecutionException e) {
                    denied = e;
                }
                assertEquals(USER_2_USERNAME, databases.findById(DATABASE_1_ID).orElseThrow().getOwnedBy(),
                        "Concurrent mutation must preserve the committed owner");
                assertNotNull(denied, "Former owner's mutation must be rejected after transfer");
                assertInstanceOf(NotAllowedException.class, denied.getCause());
                assertEquals(1, memberGrants());
                verify(dataGateway, times(1)).createAccess(DATABASE_1_ID, USER_2_USERNAME, AccessTypeDto.WRITE_ALL);
                verify(dataGateway, never()).createAccess(DATABASE_1_ID, USER_2_USERNAME, AccessTypeDto.READ);
                verify(dashboardHttp, times(1)).exchange(anyString(), eq(HttpMethod.PUT),
                        any(HttpEntity.class), eq(Void.class));
            } finally {
                releaseTransfer.countDown();
            }
        }
    }

    private DatabaseModifyVisibilityDto privateEnabled() {
        return DatabaseModifyVisibilityDto.builder().isPublic(false).isSchemaPublic(false).isDashboardEnabled(true).build();
    }

    @Test
    void dashboardFailureRollsBackViewVisibilityAndAllowsRetry() throws Exception {
        seedView();
        when(dashboardHttp.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE, "Synthetic outage",
                        HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8))
                .thenReturn(ResponseEntity.accepted().build());
        final ViewUpdateDto request = ViewUpdateDto.builder().isPublic(false).isSchemaPublic(false).build();
        assertThrows(DashboardServiceConnectionException.class, () ->
                viewEndpoint.update(DATABASE_1_ID, VIEW_1_ID, request, USER_1_PRINCIPAL));
        assertTrue(viewIsPublic());
        viewEndpoint.update(DATABASE_1_ID, VIEW_1_ID, request, USER_1_PRINCIPAL);
        assertFalse(viewIsPublic());
    }

    @Test
    void concurrentViewChangeWaitsForPublicationAndUsesCommittedVisibility() throws Exception {
        seedView();
        final CountDownLatch publicationAtGrafana = new CountDownLatch(1);
        final CountDownLatch releasePublication = new CountDownLatch(1);
        final CountDownLatch viewStarted = new CountDownLatch(1);
        when(dashboardHttp.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenAnswer(call -> {
                    if (!call.getArgument(0, String.class).endsWith("/content")) {
                        publicationAtGrafana.countDown();
                        assertTrue(releasePublication.await(10, TimeUnit.SECONDS));
                    }
                    return ResponseEntity.accepted().build();
                });
        try (ExecutorService workers = Executors.newFixedThreadPool(2)) {
            final Future<?> publication = workers.submit(new DelegatingSecurityContextCallable<>(() ->
                    databaseEndpoint.visibility(DATABASE_1_ID, DatabaseModifyVisibilityDto.builder()
                            .isPublic(true).isSchemaPublic(true).isDashboardEnabled(true).build(), USER_1_PRINCIPAL)));
            try {
                assertTrue(publicationAtGrafana.await(10, TimeUnit.SECONDS));
                final Future<?> change = workers.submit(new DelegatingSecurityContextCallable<>(() -> {
                    viewStarted.countDown();
                    return viewEndpoint.update(DATABASE_1_ID, VIEW_1_ID,
                            ViewUpdateDto.builder().isPublic(false).isSchemaPublic(false).build(), USER_1_PRINCIPAL);
                }));
                assertTrue(viewStarted.await(10, TimeUnit.SECONDS));
                assertThrows(TimeoutException.class, () -> change.get(250, TimeUnit.MILLISECONDS));
                releasePublication.countDown();
                publication.get(10, TimeUnit.SECONDS);
                change.get(10, TimeUnit.SECONDS);
            } finally {
                releasePublication.countDown();
            }
        }
        final ArgumentCaptor<HttpEntity> sent = ArgumentCaptor.forClass(HttpEntity.class);
        verify(dashboardHttp).exchange(endsWith("/content"), eq(HttpMethod.PUT), sent.capture(), eq(Void.class));
        final DatabaseDto content = (DatabaseDto) sent.getValue().getBody();
        assertTrue(content.getIsPublic());
        assertTrue(content.getIsSchemaPublic());
        assertFalse(content.getViews().getFirst().getIsPublic());
        assertFalse(content.getViews().getFirst().getIsSchemaPublic());
        assertFalse(viewIsPublic());
    }

    private void seedView() {
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            final Database database = databases.findById(DATABASE_1_ID).orElseThrow();
            database.setIsDashboardEnabled(true);
            database.getViews().add(View.builder().id(VIEW_1_ID).database(database).ownedBy(USER_1_USERNAME)
                    .name("Synthetic view").internalName("synthetic_view").query("SELECT 1").queryHash("synthetic")
                    .isPublic(true).isSchemaPublic(true).isInitialView(false)
                    .identifiers(new LinkedList<>()).columns(new LinkedList<>()).build());
            databases.saveAndFlush(database);
        });
    }

    private boolean viewIsPublic() {
        return new TransactionTemplate(transactions).execute(status -> databases.findById(DATABASE_1_ID)
                .orElseThrow().getViews().getFirst().getIsPublic());
    }

    private int memberGrants() {
        return new TransactionTemplate(transactions).execute(status -> (int) databases.findById(DATABASE_1_ID)
                .orElseThrow().getAccesses().stream().filter(access -> USER_2_USERNAME.equals(access.getUsername())).count());
    }

    private void seedAccess(String username, AccessType type) {
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            final Database database = databases.findById(DATABASE_1_ID).orElseThrow();
            database.getAccesses().add(DatabaseAccess.builder().database(database).hdbid(database.getId())
                    .username(username).type(type).build());
            databases.saveAndFlush(database);
        });
    }
}
