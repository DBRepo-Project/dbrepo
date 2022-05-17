package at.tuwien.service.impl;

import at.tuwien.config.MailConfig;
import at.tuwien.entities.user.User;
import at.tuwien.exception.UserEmailFailedException;
import at.tuwien.service.MailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class MailServiceImpl implements MailService {

    private final MailConfig mailConfig;
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Autowired
    public MailServiceImpl(MailConfig mailConfig, JavaMailSender mailSender, SpringTemplateEngine templateEngine) {
        this.mailConfig = mailConfig;
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Override
    public void send(User user, String subject, String path, Context context) throws UserEmailFailedException {
        final SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject(subject);
        message.setFrom(mailConfig.getMailFrom());
        message.setReplyTo(mailConfig.getMailReplyTo());
        final String content = templateEngine.process(path, context);
        message.setText(content);
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Failed to send message to {}: {}", user.getEmail(), e.getMessage());
            throw new UserEmailFailedException("Failed to send message", e);
        }
        log.info("Sent mail to {}", user.getEmail());
        log.debug("sent mail to user {}", user);
        log.trace("sent mail with content [{}]", content);
    }
}
