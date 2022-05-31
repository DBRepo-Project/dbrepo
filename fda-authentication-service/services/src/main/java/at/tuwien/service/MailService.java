package at.tuwien.service;

import at.tuwien.entities.user.User;
import at.tuwien.exception.UserEmailFailedException;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

@Service
public interface MailService {

    void send(User user, String subject, String template, Context context) throws UserEmailFailedException;
}
