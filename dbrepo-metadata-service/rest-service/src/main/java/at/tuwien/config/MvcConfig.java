package at.tuwien.config;

import at.tuwien.converters.IdentifierStatusTypeDtoConverter;
import at.tuwien.converters.IdentifierTypeDtoConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new IdentifierStatusTypeDtoConverter());
        registry.addConverter(new IdentifierTypeDtoConverter());
    }
}