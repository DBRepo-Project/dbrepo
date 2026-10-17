package at.ac.tuwien.ifs.dbrepo.mvc;

import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Subset;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.endpoints.SubsetEndpoint;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.service.SubsetService;
import at.ac.tuwien.ifs.dbrepo.service.MetadataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SubsetEndpointMvcTest extends BaseTest {
    @Mock private MetadataService metadataService;
    @Mock private MetadataServiceGateway metadataServiceGateway;
    @Mock private SubsetService subsetService;
    @Mock private MetadataMapper metadataMapper;
    @InjectMocks private SubsetEndpoint endpoint;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(endpoint).build();
    }

    @Test
    void findById_publicDatabase_returnsSubsetMetadata() throws Exception {
        final Subset subset = Subset.builder().id(QUERY_7_ID).databaseId(DATABASE_4_ID).build();
        when(metadataService.getDatabase(DATABASE_4_ID)).thenReturn(DATABASE_4_CACHE);
        when(subsetService.findById(DATABASE_4_CACHE, QUERY_7_ID)).thenReturn(subset);
        when(metadataMapper.subsetToQueryDto(subset)).thenReturn(QUERY_7_DTO);

        mockMvc.perform(get("/api/v1/database/" + DATABASE_4_ID + "/subset/" + QUERY_7_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(QUERY_7_ID.toString()))
                .andExpect(jsonPath("$.database_id").value(DATABASE_4_ID.toString()));
    }

    @Test
    void findById_privateDatabase_rejectsAnonymousRequest() throws Exception {
        DATABASE_4_CACHE.setIsPublic(false);
        when(metadataService.getDatabase(DATABASE_4_ID)).thenReturn(DATABASE_4_CACHE);

        mockMvc.perform(get("/api/v1/database/" + DATABASE_4_ID + "/subset/" + QUERY_7_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
        verifyNoInteractions(subsetService, metadataMapper, metadataServiceGateway);
    }

}
