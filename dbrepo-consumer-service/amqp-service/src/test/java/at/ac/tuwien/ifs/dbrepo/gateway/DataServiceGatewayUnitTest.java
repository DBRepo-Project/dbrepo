package at.ac.tuwien.ifs.dbrepo.gateway;

import at.ac.tuwien.ifs.dbrepo.config.GatewayConfig;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.DataServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.RemoteUnavailableException;
import at.ac.tuwien.ifs.dbrepo.core.exception.TableNotFoundException;
import at.ac.tuwien.ifs.dbrepo.gateway.impl.DataServiceGatewayImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class DataServiceGatewayUnitTest {

    private static final UUID DATABASE_ID = UUID.fromString("14904d0e-8ed5-4f41-9084-74cbb3e5b801");
    private static final UUID TABLE_ID = UUID.fromString("e4a2c1ab-4620-431d-a56b-f5b27c047b02");
    private static final String URL = "/api/v1/database/" + DATABASE_ID + "/table/" + TABLE_ID + "/data";
    private static final TupleDto TUPLE = TupleDto.builder().data(Map.of("value", 42)).build();
    private MockRestServiceServer server;
    private DataServiceGateway gateway;

    @BeforeEach
    void setUp() {
        final RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        gateway = new DataServiceGatewayImpl(restTemplate);
    }

    @Test
    void postsToNormalTupleEndpoint() {
        server.expect(requestTo(URL)).andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"data\":{\"value\":42}}"))
                .andRespond(withStatus(HttpStatusCode.valueOf(201)));
        assertDoesNotThrow(() -> gateway.insertRawTuple(DATABASE_ID, TABLE_ID, TUPLE));
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {408, 429, 500, 502, 503, 504})
    void temporaryHttpFailuresAreRetryable(int status) {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatusCode.valueOf(status)));
        assertThrows(RemoteUnavailableException.class, () -> gateway.insertRawTuple(DATABASE_ID, TABLE_ID, TUPLE));
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 202, 204, 302, 400, 401, 403, 409, 422})
    void permanentOrUnexpectedResponsesAreNotSuccessful(int status) {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatusCode.valueOf(status)));
        assertThrows(DataServiceException.class, () -> gateway.insertRawTuple(DATABASE_ID, TABLE_ID, TUPLE));
        server.verify();
    }

    @Test
    void missingTableIsPermanent() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatusCode.valueOf(404)));
        assertThrows(TableNotFoundException.class, () -> gateway.insertRawTuple(DATABASE_ID, TABLE_ID, TUPLE));
        server.verify();
    }

    @Test
    void networkTimeoutIsRetryable() {
        server.expect(requestTo(URL)).andRespond(withException(new SocketTimeoutException("timeout")));
        assertThrows(RemoteUnavailableException.class, () -> gateway.insertRawTuple(DATABASE_ID, TABLE_ID, TUPLE));
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void unboundedHttpTimeoutsAreRejected(int timeout) {
        final GatewayConfig config = new GatewayConfig();
        ReflectionTestUtils.setField(config, "dataConnectTimeout", timeout);
        ReflectionTestUtils.setField(config, "dataReadTimeout", 30000);
        assertThrows(IllegalArgumentException.class, config::restTemplate);
        ReflectionTestUtils.setField(config, "dataConnectTimeout", 5000);
        ReflectionTestUtils.setField(config, "dataReadTimeout", timeout);
        assertThrows(IllegalArgumentException.class, config::restTemplate);
    }
}
