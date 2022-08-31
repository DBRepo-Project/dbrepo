package at.tuwien.config;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.nio.charset.StandardCharsets;

@Log4j2
@Getter
@Configuration
public class MailConfig {

    @Value("${fda.mail.from}")
    private String mailFrom;

    @Value("${fda.mail.replyto}")
    private String mailReplyTo;

    @Value("${fda.mail.prefix}")
    private String mailPrefix;

    @Value("${spring.mail.username}")
    private String mailUsername;

    @Value("${fda.mail.verify}")
    private Boolean mailVerify;

    @Bean
    public SpringTemplateEngine springTemplateEngine() {
        final SpringTemplateEngine springTemplateEngine = new SpringTemplateEngine();
        springTemplateEngine.addTemplateResolver(emailTemplateResolver());
        return springTemplateEngine;
    }

    private ClassLoaderTemplateResolver emailTemplateResolver() {
        final ClassLoaderTemplateResolver emailTemplateResolver = new ClassLoaderTemplateResolver();
        emailTemplateResolver.setPrefix(mailPrefix + "templates/");
        emailTemplateResolver.setSuffix(".txt");
        emailTemplateResolver.setTemplateMode(TemplateMode.TEXT);
        emailTemplateResolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        emailTemplateResolver.setCacheable(false);
        return emailTemplateResolver;
    }

}
