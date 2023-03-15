package at.tuwien.config;

import lombok.extern.log4j.Log4j2;
import org.codehaus.plexus.util.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.io.File;
import java.io.IOException;

@Log4j2
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
            runQuery(FileUtils.fileRead(new File("./src/test/resources/" + scriptName)));
        } catch (IOException e) {
            log.error("Failed to load script {}", scriptName);
            throw new RuntimeException("Failed to load script", e);
        }
    }

}

