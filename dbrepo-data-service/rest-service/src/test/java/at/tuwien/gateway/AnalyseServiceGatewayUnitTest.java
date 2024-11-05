package at.tuwien.gateway;

import at.tuwien.exception.*;
import at.tuwien.test.AbstractUnitTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class AnalyseServiceGatewayUnitTest extends AbstractUnitTest {

    @MockBean
    @Qualifier("restTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private AnalyseServiceGateway dataDatabaseSidecarGateway;

    @BeforeEach
    public void beforeEach() {
        genesis();
    }

    @Test
    public void importDataset_succeeds() throws RemoteUnavailableException, StorageNotFoundException,
            AnalyseServiceException {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), eq(HttpEntity.EMPTY), eq(Void.class)))
                .thenReturn(ResponseEntity.accepted()
                        .build());

        /* test */
        dataDatabaseSidecarGateway.importDataset(DATABASE_1_ID, TABLE_1_ID, "filename");
    }

    @Test
    public void importDataset_unavailable_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), eq(HttpEntity.EMPTY), eq(Void.class));

        /* test */
        assertThrows(RemoteUnavailableException.class, () -> {
            dataDatabaseSidecarGateway.importDataset(DATABASE_1_ID, TABLE_1_ID, "filename");
        });
    }

    @Test
    public void importDataset_statusCode_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), eq(HttpEntity.EMPTY), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .build());

        /* test */
        assertThrows(AnalyseServiceException.class, () -> {
            dataDatabaseSidecarGateway.importDataset(DATABASE_1_ID, TABLE_1_ID, "filename");
        });
    }

    @Test
    public void importDataset_s3_fails() {

        /* mock */
        doThrow(HttpClientErrorException.BadRequest.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), eq(HttpEntity.EMPTY), eq(Void.class));

        /* test */
        assertThrows(StorageNotFoundException.class, () -> {
            dataDatabaseSidecarGateway.importDataset(DATABASE_1_ID, TABLE_1_ID, "filename");
        });
    }

    @Test
    public void exportTable_succeeds() throws RemoteUnavailableException, StorageNotFoundException,
            AnalyseServiceException {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), eq(HttpEntity.EMPTY), eq(Void.class)))
                .thenReturn(ResponseEntity.accepted()
                        .build());

        /* test */
        dataDatabaseSidecarGateway.exportTable(DATABASE_1_ID, TABLE_1_ID);
    }

    @Test
    public void exportTable_unavailable_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), eq(HttpEntity.EMPTY), eq(Void.class));

        /* test */
        assertThrows(RemoteUnavailableException.class, () -> {
            dataDatabaseSidecarGateway.exportTable(DATABASE_1_ID, TABLE_1_ID);
        });
    }

    @Test
    public void exportTable_statusCode_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), eq(HttpEntity.EMPTY), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .build());

        /* test */
        assertThrows(AnalyseServiceException.class, () -> {
            dataDatabaseSidecarGateway.exportTable(DATABASE_1_ID, TABLE_1_ID);
        });
    }

    @Test
    public void exportTable_s3_fails() {

        /* mock */
        doThrow(HttpClientErrorException.BadRequest.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), eq(HttpEntity.EMPTY), eq(Void.class));

        /* test */
        assertThrows(StorageNotFoundException.class, () -> {
            dataDatabaseSidecarGateway.exportTable(DATABASE_1_ID, TABLE_1_ID);
        });
    }

}
