package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.ViewNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicaLocation;
import at.ac.tuwien.ifs.dbrepo.service.outbox.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ViewReplicationSqlUnitTest {
    private static final String SOURCE = "ui_replication_20261004_s46_new_jk1c";
    private static final String SQL = "select `" + SOURCE + "`.`measurements`.`id`, `" + SOURCE
            + "`.`measurements`.`value` from `measurements` where `" + SOURCE + "`.`measurements`.`value` > '3'";
    private static final String LOCAL_SQL = "select `measurements`.`id`, `measurements`.`value` from `measurements` where `measurements`.`value` > '3'";

    @Test
    void localizesTheFailingStoredViewDefinition() {
        assertEquals(LOCAL_SQL, ViewSqlSchema.localize(SQL, SOURCE));
    }

    @Test
    void keepsLiteralsCommentsAliasesAndOtherSchemas() {
        final String sql = "select `source`.`id`, 'source.t.id', 'it\\'s source.t.id', \"source.t.id\", other.t.id, source.t.id "
                + "from source.t as `source` /* source.t.id */ -- source.t.id\n# source.t.id\nwhere `source`.`value` > 3";
        final String expected = "select `source`.`id`, 'source.t.id', 'it\\'s source.t.id', \"source.t.id\", other.t.id, t.id "
                + "from t as `source` /* source.t.id */ -- source.t.id\n# source.t.id\nwhere `source`.`value` > 3";
        assertEquals(expected, ViewSqlSchema.localize(sql, "source"));
    }

    @Test
    void handlesJoinsCommaRelationsAndNestedQueries() {
        final String sql = "select source.a.id from source.a join source.b on a.id=b.id, source.c "
                + "where a.id in (select source.d.id from source.d) union select source.e.id from source.e";
        assertEquals("select a.id from a join b on a.id=b.id, c where a.id in (select d.id from d) union select e.id from e",
                ViewSqlSchema.localize(sql, "source"));
        assertEquals("select `t`.`id` from `t`", ViewSqlSchema.localize("select `so``urce`.`t`.`id` from `so``urce`.`t`", "so`urce"));
    }

    @Test
    void rejectsMissingOriginInsteadOfSendingIncorrectSql() {
        assertThrows(IllegalArgumentException.class, () -> ViewSqlSchema.localize(SQL, null));
    }

    @Test
    void initialDeliveryAndRetainedRetryBothLocalizeWithoutChangingTheEvent() throws Exception {
        final var metadata = mock(org.springframework.web.client.RestTemplate.class);
        final var external = mock(org.springframework.web.client.RestTemplate.class);
        final var outbox = mock(ReplicationOutboxService.class);
        final var json = new ObjectMapper().findAndRegisterModules();
        final var service = new ReplicationServiceImpl(metadata, mock(org.springframework.web.client.RestTemplate.class), external, json, outbox);
        final UUID sourceId = UUID.randomUUID();
        final UUID remoteId = UUID.randomUUID();
        final UUID viewId = UUID.randomUUID();
        final var notification = ViewNotificationDto.builder().databaseId(sourceId).creationId(viewId)
                .viewDto(ViewDto.builder().id(viewId).query(SQL).build())
                .replicas(List.of(ReplicaLocation.builder().url("https://replica.example").replicaDatabaseId(remoteId).build())).build();
        when(metadata.exchange(eq("/api/v1/database/" + sourceId), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(DatabaseDto.class)))
                .thenReturn(ResponseEntity.ok(DatabaseDto.builder().id(sourceId).internalName(SOURCE).build()));
        when(external.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(ViewBriefDto.class)))
                .thenReturn(ResponseEntity.ok(ViewBriefDto.builder().id(viewId).build()));

        assertEquals(1, service.replicateView(notification));
        final String retainedPayload = json.writeValueAsString(notification);
        final var entry = ReplicationOutboxEntry.builder().id(UUID.randomUUID()).operationType(ReplicationOutboxOperationType.VIEW_CREATE)
                .status(ReplicationOutboxStatus.PENDING).localDatabaseId(sourceId).remoteDatabaseId(remoteId)
                .targetSiteUrl("https://replica.example").payloadJson(retainedPayload).build();
        when(outbox.findById(entry.getId())).thenReturn(Optional.of(entry));
        assertTrue(service.retryOutboxEntry(entry.getId()));

        final ArgumentCaptor<HttpEntity> sent = ArgumentCaptor.forClass(HttpEntity.class);
        verify(external, times(2)).exchange(eq("https://replica.example/api/v1/database/" + remoteId + "/view/replicate"),
                eq(HttpMethod.POST), sent.capture(), eq(ViewBriefDto.class));
        for (HttpEntity request : sent.getAllValues()) {
            final var body = (ViewNotificationDto) request.getBody();
            assertEquals(LOCAL_SQL, body.getViewDto().getQuery());
            assertEquals(viewId, body.getCreationId());
        }
        assertEquals(SQL, notification.getViewDto().getQuery());
        assertEquals(retainedPayload, entry.getPayloadJson());
        verify(outbox).markSucceeded(entry.getId());
    }
}
