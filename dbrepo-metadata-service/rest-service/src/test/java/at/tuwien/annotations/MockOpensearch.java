package at.tuwien.annotations;

import at.tuwien.repository.sdb.*;
import org.mockito.Mock;
import org.opensearch.spring.boot.autoconfigure.OpenSearchRestClientAutoConfiguration;
import org.opensearch.spring.boot.autoconfigure.OpenSearchRestHighLevelClientAutoConfiguration;
import org.opensearch.spring.boot.autoconfigure.data.OpenSearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@MockBeans({@MockBean(DatabaseIdxRepository.class)/*, @MockBean(UnitIdxRepository.class),
        @MockBean(ConceptIdxRepository.class), @MockBean(TableIdxRepository.class), @MockBean(UserIdxRepository.class),
        @MockBean(TableColumnIdxRepository.class), @MockBean(ViewIdxRepository.class)*/, @MockBean(IdentifierIdxRepository.class)})
@EnableAutoConfiguration(exclude = {OpenSearchRestClientAutoConfiguration.class, OpenSearchRestHighLevelClientAutoConfiguration.class})
public @interface MockOpensearch {
}
