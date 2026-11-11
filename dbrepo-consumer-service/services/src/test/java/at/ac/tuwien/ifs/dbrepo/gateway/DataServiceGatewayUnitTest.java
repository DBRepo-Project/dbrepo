package at.ac.tuwien.ifs.dbrepo.gateway;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleDto;
import at.ac.tuwien.ifs.dbrepo.gateway.impl.DataServiceGatewayImpl;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class DataServiceGatewayUnitTest {

    @Test
    void insertRawTuple_postsToTableDataEndpoint() throws Exception {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory("http://data-service:8080"));
        final MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        final DataServiceGateway gateway = new DataServiceGatewayImpl(restTemplate);
        final UUID databaseId = UUID.fromString("6aa6643e-3223-44ce-a824-36ab66ae511e");
        final UUID tableId = UUID.fromString("d7c63353-b6a9-4ce3-b2f1-7fd2131e3cd6");

        server.expect(requestTo("http://data-service:8080/api/v1/database/"
                        + databaseId + "/table/" + tableId + "/data/ingest"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.header("X-DBRepo-Publisher", "owner"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"data\":{\"temperature\":21.5}}"))
                .andRespond(withStatus(HttpStatus.CREATED));

        gateway.insertRawTuple(databaseId, tableId, TupleDto.builder()
                .data(Map.of("temperature", 21.5))
                .build(), "owner");

        server.verify();
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints={429,500,503})
    void transientHttpFailuresRemainRetryable(int status) {
        final RestTemplate http = new RestTemplate();
        http.setUriTemplateHandler(new DefaultUriBuilderFactory("http://data-service:8080"));
        final MockRestServiceServer server=MockRestServiceServer.bindTo(http).build();
        server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.anything())
                .andRespond(withStatus(HttpStatus.valueOf(status)));
        org.junit.jupiter.api.Assertions.assertThrows(at.ac.tuwien.ifs.dbrepo.core.exception.RemoteUnavailableException.class,
                ()->new DataServiceGatewayImpl(http).insertRawTuple(UUID.randomUUID(),UUID.randomUUID(),
                        TupleDto.builder().data(Map.of("id",1)).build(),"publisher"));
        server.verify();
    }

}
