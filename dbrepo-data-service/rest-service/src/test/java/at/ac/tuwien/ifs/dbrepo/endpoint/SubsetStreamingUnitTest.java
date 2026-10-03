package at.ac.tuwien.ifs.dbrepo.endpoint;

import at.ac.tuwien.ifs.dbrepo.core.api.database.query.SubsetDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Subset;
import at.ac.tuwien.ifs.dbrepo.endpoints.SubsetEndpoint;
import at.ac.tuwien.ifs.dbrepo.validation.EndpointValidator;
import at.ac.tuwien.ifs.dbrepo.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SubsetStreamingUnitTest {
    private final UUID databaseId = UUID.randomUUID(), subsetId = UUID.randomUUID();
    private final SubsetResultReader reader = mock(SubsetResultReader.class);
    private MockMvc mvc;
    private String path;

    @BeforeEach
    void setUp() throws Exception {
        var database = Database.builder().id(databaseId).isPublic(true).build();
        var subset = Subset.builder().id(subsetId).snapshotHash("a".repeat(64))
                .resultHash("v2:" + "b".repeat(64)).resultNumber(1L).build();
        var metadata = mock(MetadataService.class);
        var subsets = mock(SubsetService.class);
        when(metadata.getDatabase(databaseId)).thenReturn(database);
        when(subsets.findById(database, subsetId)).thenReturn(subset);
        when(subsets.openResult(database, subset)).thenReturn(reader);
        when(subsets.create(eq(database), any(SubsetDto.class), any(), isNull())).thenReturn(subsetId);
        when(reader.names()).thenReturn(List.of("id", "value"));
        doAnswer(invocation -> {
            invocation.<OutputStream>getArgument(0).write("[{\"id\":1,\"value\":\"original\"}]".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(reader).json(any(), eq(0L), eq(100L));
        doAnswer(invocation -> {
            invocation.<OutputStream>getArgument(0).write("id,value\n1,original\n".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(reader).csv(any());
        var endpoint = new SubsetEndpoint(null, null, null, subsets, null, null, metadata,
                mock(EndpointValidator.class), null, new ObjectMapper());
        mvc = MockMvcBuilders.standaloneSetup(endpoint).build();
        path = "/api/v1/database/" + databaseId + "/subset";
    }

    @Test
    void getStreamsRowsInsteadOfSerializingTheCallback() throws Exception {
        var result = mvc.perform(get(path + "/" + subsetId + "/data?page=0&size=100")
                .accept(MediaType.APPLICATION_JSON)).andExpect(request().asyncStarted()).andReturn();
        mvc.perform(asyncDispatch(result)).andExpect(status().isOk())
                .andExpect(header().string("X-Result-Mode", "immutable-snapshot"))
                .andExpect(content().json("[{\"id\":1,\"value\":\"original\"}]"));
        verify(reader, atLeastOnce()).close();
    }

    @Test
    void createStreamsTheSameImmutableRows() throws Exception {
        var result = mvc.perform(post(path + "?page=0&size=100").contentType(MediaType.APPLICATION_JSON)
                .content("{\"datasource_ids\":[\"" + UUID.randomUUID() + "\"],\"columns\":[]}"))
                .andExpect(request().asyncStarted()).andReturn();
        mvc.perform(asyncDispatch(result)).andExpect(status().isCreated())
                .andExpect(content().json("[{\"id\":1,\"value\":\"original\"}]"));
        verify(reader, atLeastOnce()).close();
    }

    @Test
    void csvStreamsAndHeadClosesWithoutStreaming() throws Exception {
        var result = mvc.perform(get(path + "/" + subsetId + "/data").accept("text/csv"))
                .andExpect(request().asyncStarted()).andReturn();
        mvc.perform(asyncDispatch(result)).andExpect(status().isOk())
                .andExpect(content().string("id,value\n1,original\n"));
        clearInvocations(reader);
        mvc.perform(head(path + "/" + subsetId + "/data").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(request().asyncNotStarted())
                .andExpect(header().string("X-Count", "1")).andExpect(content().string(""));
        verify(reader).close();
        verify(reader, never()).json(any(), anyLong(), anyLong());
        verify(reader, never()).csv(any());
    }
}
