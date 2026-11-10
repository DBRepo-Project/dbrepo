package at.ac.tuwien.ifs.dbrepo.endpoint;

import at.ac.tuwien.ifs.dbrepo.core.entity.cache.*;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.service.TableService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ConsumerIngestAuthorizationTest {
    @Autowired MockMvc mvc;
    @MockitoBean MetadataServiceGateway gateway;
    @MockitoBean at.ac.tuwien.ifs.dbrepo.gateway.KeycloakGateway keycloakGateway;
    @MockitoBean TableService tableService;
    private final UUID databaseId=UUID.randomUUID(), tableId=UUID.randomUUID();
    private Database database;
    private String path;

    @BeforeEach void prepare() throws Exception {
        Table table=Table.builder().id(tableId).databaseId(databaseId).ownedBy("publisher").columns(List.of()).build();
        database=Database.builder().id(databaseId).tables(List.of(table)).accesses(List.of(
                DatabaseAccess.builder().username("publisher").type(AccessType.WRITE_OWN).build())).build();
        when(gateway.getDatabaseById(databaseId)).thenReturn(database);
        path="/api/v1/database/"+databaseId+"/table/"+tableId+"/data/ingest";
    }

    @Test @WithMockUser(authorities="system")
    void workerUsesCurrentPublisherGrantAndHonorsRevocation() throws Exception {
        mvc.perform(post(path).header("X-DBRepo-Publisher","publisher").contentType(MediaType.APPLICATION_JSON)
                .content("{\"data\":{\"id\":1}}")).andExpect(status().isCreated());
        database.setAccesses(List.of(DatabaseAccess.builder().username("publisher").type(AccessType.READ).build()));
        mvc.perform(post(path).header("X-DBRepo-Publisher","publisher").contentType(MediaType.APPLICATION_JSON)
                .content("{\"data\":{\"id\":2}}")).andExpect(status().isForbidden());
        verify(tableService,times(1)).createTuple(any(),any(),any());
    }

    @Test @WithMockUser(authorities="system")
    void workerCannotWriteForAnUnrelatedPublisher() throws Exception {
        mvc.perform(post(path).header("X-DBRepo-Publisher","unrelated").contentType(MediaType.APPLICATION_JSON)
                .content("{\"data\":{\"id\":1}}")).andExpect(status().isForbidden());
        verifyNoInteractions(tableService);
    }

    @Test @WithMockUser(username="publisher",authorities="insert-table-data")
    void ordinaryWriterCannotAssertAnIngestionIdentity() throws Exception {
        mvc.perform(post(path).header("X-DBRepo-Publisher","publisher").contentType(MediaType.APPLICATION_JSON)
                .content("{\"data\":{\"id\":1}}")).andExpect(status().isUnauthorized());
        verifyNoInteractions(tableService);
    }

    @Test @WithMockUser(authorities="system")
    void unidentifiedPublisherIsRejected() throws Exception {
        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{\"data\":{\"id\":1}}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(tableService);
    }

    @Test @WithMockUser(authorities="system")
    void tableMustBelongToTheRequestedDatabase() throws Exception {
        database.setTables(List.of());
        mvc.perform(post(path).header("X-DBRepo-Publisher","publisher").contentType(MediaType.APPLICATION_JSON)
                .content("{\"data\":{\"id\":1}}")).andExpect(status().isNotFound());
        verifyNoInteractions(tableService);
    }
}
