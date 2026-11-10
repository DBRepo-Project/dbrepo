package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.container.CreateContainerDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.container.Container;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.metadata.ContainerRepository;
import at.ac.tuwien.ifs.dbrepo.metadata.ImageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ContainerRegistrationPersistenceTest extends BaseTest {
    @Autowired ContainerService service;
    @Autowired ImageRepository images;
    @Autowired ContainerRepository containers;
    @Autowired EntityManager entityManager;
    @Autowired MetadataMapper mapper;
    @Autowired ObjectMapper json;
    @Autowired Validator validator;

    private CreateContainerDto request() {
        return CreateContainerDto.builder().name("Registration fixture").host("fixture-db")
                .imageId(IMAGE_1_ID).quota(37L)
                .privilegedUsername("fixture_root").privilegedPassword("fixture-root-secret")
                .readonlyUsername("fixture_reader").readonlyPassword("fixture-reader-secret").build();
    }

    @Test
    void persistsCredentialsQuotaAndOptionalPortWithoutExposingPasswords() throws Exception {
        images.saveAndFlush(IMAGE_1);
        CreateContainerDto request = request();
        assertTrue(validator.validate(request).isEmpty());
        Container created = service.create(request);
        containers.flush();
        entityManager.clear();
        Container stored = containers.findById(created.getId()).orElseThrow();
        assertEquals("fixture_reader", stored.getReadonlyUsername());
        assertEquals("fixture-reader-secret", stored.getReadonlyPassword());
        assertEquals(37, stored.getQuota());
        assertEquals(IMAGE_1_DEFAULT_PORT, stored.getPort());
        assertEquals("fixture-db", stored.getUiHost());
        assertEquals(IMAGE_1_DEFAULT_PORT, stored.getUiPort());
        assertEquals(0, mapper.containerToContainerDto(stored).getCount());
        String response = json.writeValueAsString(mapper.containerToContainerDto(stored));
        assertFalse(response.contains("fixture-root-secret"));
        assertFalse(response.contains("fixture-reader-secret"));
        assertFalse(request.toString().contains("fixture-root-secret"));
        assertFalse(request.toString().contains("fixture-reader-secret"));
        service.remove(stored);
        containers.flush();
        assertTrue(containers.findById(created.getId()).isEmpty());
    }

    @Test
    void removingRegistrationPreservesSharedImageAndOtherContainer() throws Exception {
        images.saveAndFlush(IMAGE_1);
        Container first = service.create(request());
        CreateContainerDto secondRequest = request();
        secondRequest.setName("Another registration");
        Container second = service.create(secondRequest);
        containers.flush();
        service.remove(first);
        containers.flush();
        entityManager.clear();
        assertTrue(containers.findById(first.getId()).isEmpty());
        assertTrue(containers.findById(second.getId()).isPresent());
        assertTrue(images.findById(IMAGE_1_ID).isPresent());
    }

    @Test
    void preservesExplicitDisplayConnection() throws Exception {
        images.saveAndFlush(IMAGE_1);
        CreateContainerDto request = request();
        request.setPort(3307);
        request.setUiHost("display.example");
        request.setUiPort(3308);
        Container stored = service.create(request);
        containers.flush();
        assertEquals(3307, stored.getPort());
        assertEquals("display.example", stored.getUiHost());
        assertEquals(3308, stored.getUiPort());
    }

    @Test
    void rejectsMissingCredentialsPrivilegedReaderAndInvalidQuota() {
        CreateContainerDto request = request();
        request.setReadonlyPassword(null);
        assertFalse(validator.validate(request).isEmpty());
        request.setReadonlyPassword("fixture-reader-secret");
        request.setReadonlyUsername(request.getPrivilegedUsername());
        assertFalse(validator.validate(request).isEmpty());
        request.setReadonlyUsername("fixture_reader");
        request.setQuota(0L);
        assertFalse(validator.validate(request).isEmpty());
        request.setQuota(2147483648L);
        assertFalse(validator.validate(request).isEmpty());
    }
}
