package at.ac.tuwien.ifs.dbrepo.gateway;

import at.ac.tuwien.ifs.dbrepo.config.KeycloakConfig;
import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.gateway.impl.KeycloakGatewayImpl;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PublisherAccountPermissionTest {
    @Test void publisherNeedsAnEnabledCurrentAccountAndEffectiveWriteRole() throws Exception {
        Keycloak client=mock(Keycloak.class,RETURNS_DEEP_STUBS);
        KeycloakConfig config=mock(KeycloakConfig.class);
        when(config.getKeycloakRealm()).thenReturn("dbrepo");
        KeycloakGateway gateway=new KeycloakGatewayImpl(config,mock(MetadataMapper.class),client);
        UserRepresentation publisher=new UserRepresentation();
        publisher.setId("fixture");publisher.setUsername("publisher");publisher.setEnabled(true);
        var users=client.realm("dbrepo").users();
        when(users.searchByUsername("publisher",true)).thenReturn(List.of(publisher));
        when(users.get("fixture").roles().realmLevel().listEffective())
                .thenReturn(List.of(new RoleRepresentation("insert-table-data","",false)));
        assertDoesNotThrow(()->gateway.validatePublisher("publisher"));
        publisher.setEnabled(false);
        assertThrows(NotAllowedException.class,()->gateway.validatePublisher("publisher"));
        publisher.setEnabled(true);
        when(users.get("fixture").roles().realmLevel().listEffective()).thenReturn(List.of());
        assertThrows(NotAllowedException.class,()->gateway.validatePublisher("publisher"));
        when(users.searchByUsername("publisher",true)).thenReturn(List.of());
        assertThrows(NotAllowedException.class,()->gateway.validatePublisher("publisher"));
    }
}
