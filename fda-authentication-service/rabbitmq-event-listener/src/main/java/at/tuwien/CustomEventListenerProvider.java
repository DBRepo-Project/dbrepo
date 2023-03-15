package at.tuwien;

import org.jboss.logging.Logger;
import org.keycloak.email.DefaultEmailSenderProvider;
import org.keycloak.email.EmailException;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserModel;

import java.util.Map;

public class CustomEventListenerProvider implements EventListenerProvider {

    private static final Logger log = Logger.getLogger(CustomEventListenerProvider.class);

    private final KeycloakSession session;
    private final RealmProvider model;

    public CustomEventListenerProvider(KeycloakSession session) {
        this.session = session;
        this.model = session.realms();
    }

    @Override
    public void onEvent(Event event) {
        if (event.getType().equals(EventType.REGISTER)) {

        }
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {
        if (adminEvent.getOperationType().equals(OperationType.CREATE) && adminEvent.getResourcePath().startsWith("users/")) {
            log.infof("=======> Created user!!");
        } else if (adminEvent.getOperationType().equals(OperationType.ACTION) && adminEvent.getResourcePath().startsWith("users/") && adminEvent.getResourcePath().endsWith("reset-password")) {
            log.infof("=======> Modified user password!!");
        }
    }

    @Override
    public void close() {

    }

    private void createUser(String username) {

    }
}