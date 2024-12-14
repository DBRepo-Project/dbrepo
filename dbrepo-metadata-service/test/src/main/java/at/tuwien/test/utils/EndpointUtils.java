package at.tuwien.test.utils;

import at.tuwien.test.dto.LocaleDto;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class EndpointUtils {

    public static List<Class<?>> getExceptions() throws ClassNotFoundException {
        final ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*")));
        final Set<BeanDefinition> beans = provider.findCandidateComponents("at.tuwien.exception");
        final List<Class<?>> exceptions = new LinkedList<>();
        for (BeanDefinition bean : beans) {
            exceptions.add(Class.forName(bean.getBeanClassName()));
        }
        return exceptions;
    }

    public static List<String> getErrorCodes() throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        final LocaleDto locale = objectMapper.readValue(new File("../../dbrepo-ui/locales/en-US.json"), LocaleDto.class);
        return locale.getError()
                .entrySet()
                .stream()
                .map(group -> group.getValue()
                        .keySet()
                        .stream()
                        .map(key -> "error." + group.getKey() + "." + key)
                        .toList())
                .flatMap(List::stream)
                .toList();
    }
}
