package at.ac.tuwien.ifs.dbrepo.mvc;

import at.ac.tuwien.ifs.dbrepo.core.entity.identifier.Identifier;
import at.ac.tuwien.ifs.dbrepo.core.entity.identifier.IdentifierStatusType;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.metadata.ContainerRepository;
import at.ac.tuwien.ifs.dbrepo.metadata.DatabaseRepository;
import at.ac.tuwien.ifs.dbrepo.metadata.LicenseRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OaiHarvestIntegrationTest extends BaseTest {

    @Autowired private MockMvc mvc;
    @Autowired private EntityManager entities;
    @Autowired private LicenseRepository licenses;
    @Autowired private ContainerRepository containers;
    @Autowired private DatabaseRepository databases;

    @ParameterizedTest
    @ValueSource(strings = {"ListRecords", "ListIdentifiers"})
    void filtersByModificationTimeAndSet(String verb) throws Exception {
        licenses.save(LICENSE_1);
        containers.save(CONTAINER_1);
        databases.saveAndFlush(DATABASE_1);

        setDates(IDENTIFIER_1_ID, IdentifierStatusType.PUBLISHED, "2024-02-29T00:00:00Z", null);
        setDates(IDENTIFIER_2_ID, IdentifierStatusType.PUBLISHED, "2024-02-20T00:00:00Z", null);
        setDates(IDENTIFIER_3_ID, IdentifierStatusType.DRAFT, "2024-02-29T12:00:00Z", null);
        setDates(IDENTIFIER_4_ID, IdentifierStatusType.PUBLISHED, "2024-03-01T00:00:00Z", "2024-02-28T00:00:00Z");
        entities.clear();

        list(verb, "2024-02-29", "2024-02-29", "openaire_data")
                .andExpect(xpath("//header").nodeCount(1))
                .andExpect(xpath("//header/identifier").string("doi:" + IDENTIFIER_1_DOI));
        setDates(IDENTIFIER_2_ID, IdentifierStatusType.PUBLISHED, "2024-02-20T00:00:00Z", "2024-02-29T23:59:59.750Z");
        entities.clear();

        list(verb, "2024-02-29", "2024-02-29", null)
                .andExpect(xpath("//header").nodeCount(2))
                .andExpect(xpath("//header[datestamp='2024-02-29T23:59:59Z']/identifier").string("oai:" + IDENTIFIER_2_ID));
        list(verb, "2024-02-29T23:59:59Z", "2024-02-29T23:59:59Z", "openaire_data")
                .andExpect(xpath("//header").nodeCount(1))
                .andExpect(xpath("//header/setSpec").string("openaire_data"));
        list(verb, null, "2024-02-28", null).andExpect(xpath("//error[@code='noRecordsMatch']").exists());
        list(verb, "2999-01-01", null, null).andExpect(xpath("//error[@code='noRecordsMatch']").exists());
        list(verb, null, null, "unlisted").andExpect(xpath("//error[@code='noRecordsMatch']").exists());
        mvc.perform(get("/api/v1/oai").param("verb", "GetRecord").param("metadataPrefix", "oai_dc")
                        .param("identifier", "oai:" + IDENTIFIER_2_ID))
                .andExpect(xpath("//header/datestamp").string("2024-02-29T23:59:59Z"))
                .andExpect(xpath("//header/setSpec").string("openaire_data"));

        entities.find(Identifier.class, IDENTIFIER_2_ID).setPublisher("Updated publisher");
        entities.flush();
        entities.clear();
        final Instant modified = entities.find(Identifier.class, IDENTIFIER_2_ID).getLastModified();
        assertNotEquals(Instant.parse("2024-02-29T23:59:59.750Z"), modified);
        final String second = modified.truncatedTo(ChronoUnit.SECONDS).toString();
        list(verb, second, second, null)
                .andExpect(xpath("//header[identifier='oai:" + IDENTIFIER_2_ID + "']/datestamp").string(second));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ListRecords", "ListIdentifiers"})
    void rejectsInvalidArguments(String verb) throws Exception {
        list(verb, "2024-02-30", null, null).andExpect(xpath("//error[@code='badArgument']").exists());
        list(verb, "2024-03-01", "2024-02-29", null).andExpect(xpath("//error[@code='badArgument']").exists());
        list(verb, "2024-02-29", "2024-02-29T23:59:59Z", null).andExpect(xpath("//error[@code='badArgument']").exists());
        list(verb, null, null, "bad set").andExpect(xpath("//error[@code='badArgument']").exists());
        mvc.perform(get("/api/v1/oai").param("verb", verb))
                .andExpect(xpath("//error[@code='badArgument']").exists());
        mvc.perform(get("/api/v1/oai").param("verb", verb).param("metadataPrefix", "oai_dc", "oai_dc"))
                .andExpect(xpath("//error[@code='badArgument']").exists());
        mvc.perform(get("/api/v1/oai").param("verb", verb).param("metadataPrefix", "oai_dc").param("extra", "value"))
                .andExpect(xpath("//error[@code='badArgument']").exists());
        mvc.perform(get("/api/v1/oai").param("verb", verb).param("metadataPrefix", "unsupported"))
                .andExpect(xpath("//error[@code='cannotDisseminateFormat']").exists());
        mvc.perform(get("/api/v1/oai").param("verb", verb).param("resumptionToken", "page&offset"))
                .andExpect(xpath("//error[@code='badResumptionToken']").exists())
                .andExpect(xpath("//request/@resumptionToken").string("page&offset"));
        mvc.perform(get("/api/v1/oai").param("verb", verb).param("resumptionToken", "expired").param("metadataPrefix", "oai_dc"))
                .andExpect(xpath("//error[@code='badArgument']").exists());
    }

    private ResultActions list(String verb, String from, String until, String set) throws Exception {
        var request = get("/api/v1/oai").param("verb", verb).param("metadataPrefix", "oai_dc");
        if (from != null) request.param("from", from);
        if (until != null) request.param("until", until);
        if (set != null) request.param("set", set);
        return mvc.perform(request).andExpect(status().isOk());
    }

    private void setDates(UUID id, IdentifierStatusType status, String created, String modified) {
        assertEquals(1, entities.createQuery("""
                update Identifier i set i.status = :status, i.created = :created, i.lastModified = :modified where i.id = :id
                """).setParameter("id", id).setParameter("status", status).setParameter("created", Instant.parse(created))
                .setParameter("modified", modified == null ? null : Instant.parse(modified)).executeUpdate());
    }
}
