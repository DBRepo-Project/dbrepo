package at.tuwien.handlers;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.repository.sdb.DatabaseIdxRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Log4j2
@ExtendWith(SpringExtension.class)
@SpringBootTest
@MockAmqp
@MockOpensearch
public class ApiExceptionHandlerTest extends BaseUnitTest {

    @Test
    public void handle_succeeds() throws ClassNotFoundException {
        final List<Method> handlers = Arrays.asList(ApiExceptionHandler.class.getMethods());
        final List<Class<?>> exceptions = getExceptions();

        /* test */
        for (Class<?> exception : exceptions) {
            final boolean response = handlers.stream().anyMatch(h -> Arrays.asList(h.getParameterTypes()).contains(exception));
            assertTrue(response, "Exception " + exception.getName() + " does not have a corresponding handle method in the endpoint");
        }
    }

    private List<Class<?>> getExceptions() throws ClassNotFoundException {
        final ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*")));
        final Set<BeanDefinition> beans = provider.findCandidateComponents("at.tuwien.exception");
        final List<Class<?>> exceptions = new LinkedList<>();
        for (BeanDefinition bean : beans) {
            exceptions.add(Class.forName(bean.getBeanClassName()));
        }
        return exceptions;
    }

}
