package at.ac.tuwien.ifs.dbrepo.utils;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class H2Utils {

    @Autowired
    private EntityManager entityManager;

    @Transactional
    public void runQuery(String query) {
        log.debug("query={}", query);
        entityManager.createNativeQuery(query)
                .executeUpdate();
    }

    @Transactional
    public void runScript(String scriptName) {
        try {
            runQuery(FileUtils.readFileToString(new File("./src/test/resources/" + scriptName), StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error("Failed to load script {}", scriptName);
            throw new RuntimeException("Failed to load script", e);
        }
    }

}

