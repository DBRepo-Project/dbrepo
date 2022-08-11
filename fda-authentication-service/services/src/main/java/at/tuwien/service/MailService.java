package at.tuwien.service;

import at.tuwien.entities.user.User;
import at.tuwien.exception.UserEmailFailedException;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

@Service
public interface MailService {

    /**
     * Sends a mail to a user with subject and template and passing variables through the context.
     *
     * @param user     The user.
     * @param subject  The subject.
     * @param template The template.
     * @param context  The context.
     * @throws UserEmailFailedException The user email was not found.
     */
    void send(User user, String subject, String template, Context context) throws UserEmailFailedException;
}
