package at.ac.tuwien.ifs.dbrepo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.nio.charset.StandardCharsets;

@Configuration
public class TemplateConfig {

    @Bean
    public SpringTemplateEngine springTemplateEngine() {
        final SpringTemplateEngine springTemplateEngine = new SpringTemplateEngine();
        springTemplateEngine.addTemplateResolver(oaiTemplateResolver());
        return springTemplateEngine;
    }

    private ClassLoaderTemplateResolver oaiTemplateResolver() {
        final ClassLoaderTemplateResolver oaiTemplateResolver = new ClassLoaderTemplateResolver();
        oaiTemplateResolver.setPrefix("/templates/");
        oaiTemplateResolver.setSuffix(".xml");
        oaiTemplateResolver.setTemplateMode(TemplateMode.TEXT);
        oaiTemplateResolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        oaiTemplateResolver.setCacheable(false);
        return oaiTemplateResolver;
    }
}
