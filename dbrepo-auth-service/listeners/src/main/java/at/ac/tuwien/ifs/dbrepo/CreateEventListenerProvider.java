package at.ac.tuwien.ifs.dbrepo;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateEventListenerProvider implements EventListenerProvider {

    private static final Logger log = LoggerFactory.getLogger(CreateEventListenerProvider.class);

    private final KeycloakSession session;
    private final RealmProvider model;

    public CreateEventListenerProvider(KeycloakSession session) {
        this.session = session;
        this.model = session.realms();
    }

    @Override
    public void onEvent(Event event) {
        log.atDebug()
                .setMessage("received event: " + event.getType())
                .addKeyValue("event.type", event.getType())
                .addKeyValue("event.realm_id", event.getRealmId())
                .addKeyValue("event.user_id", event.getUserId())
                .log();
        if (EventType.REGISTER.equals(event.getType()) || EventType.IDENTITY_PROVIDER_LOGIN.equals(event.getType())) {
            final RealmModel realm = this.model.getRealm(event.getRealmId());
            sendUserData(this.session.users().getUserById(realm, event.getUserId()));
        }
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {
        log.atDebug()
                .setMessage("received admin event: " + adminEvent.getResourceType())
                .addKeyValue("event.realm_id", adminEvent.getRealmId())
                .addKeyValue("event.operation_type", adminEvent.getOperationType())
                .addKeyValue("event.resource_type", adminEvent.getResourceType())
                .addKeyValue("event.resource_path", adminEvent.getResourcePath())
                .log();
        if (ResourceType.USER.equals(adminEvent.getResourceType())
                && OperationType.CREATE.equals(adminEvent.getOperationType())) {
            final RealmModel realm = this.model.getRealm(adminEvent.getRealmId());
            sendUserData(this.session.users().getUserById(realm, adminEvent.getResourcePath().substring(6)));
        }
    }

    private void sendUserData(UserModel user) {
        final String userData = "{" +
                quoteAttr("id", user.getId()) + ", " +
                quoteAttr("username", user.getUsername()) + ", " +
                quoteAttr("ldap_id", user.getFirstAttribute("LDAP_ID")) + ", " +
                quoteAttr("given_name", user.getFirstName()) + ", " +
                quoteAttr("family_name", user.getLastName()) +
                "}";
        try {
            Client.postService(userData);
            log.atInfo()
                    .addKeyValue("id", user.getId())
                    .addKeyValue("username", user.getUsername())
                    .addKeyValue("ldap_id", user.getFirstAttribute("LDAP_ID"))
                    .addKeyValue("given_name", user.getFirstName())
                    .addKeyValue("family_name", user.getLastName())
                    .setMessage("Created new user in metadata service: " + user.getId())
                    .log();
        } catch (Exception e) {
            log.error("Failed to call metadata service: {}", e.getMessage());
        }
    }

    private static String quoteAttr(String key, String value) {
        if (value == null || value.isBlank() || value.isEmpty() || value.contentEquals(" ")) {
            return "\"" + key + "\": null";
        }
        return "\"" + key + "\": \"" + value + "\"";
    }

    @Override
    public void close() {
    }
}
