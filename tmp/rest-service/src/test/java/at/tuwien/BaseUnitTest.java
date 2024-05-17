package at.tuwien;

import at.tuwien.api.container.internal.PrivilegedContainerDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.table.constraints.ConstraintsDto;
import at.tuwien.api.database.table.internal.PrivilegedTableDto;
import at.tuwien.test.AbstractUnitTest;
import at.tuwien.test.BaseTest;
import org.springframework.test.context.TestPropertySource;

import java.util.LinkedList;
import java.util.List;

@TestPropertySource(locations = "classpath:application.properties")
public abstract class BaseUnitTest extends AbstractUnitTest {

    public final static String USER_LOCAL_ADMIN_USERNAME = "admin";
    public final static String USER_LOCAL_ADMIN_PASSWORD = "admin";

    public final static PrivilegedContainerDto CONTAINER_1_PRIVILEGED_DTO = PrivilegedContainerDto.builder()
            .id(CONTAINER_1_ID)
            .name(CONTAINER_1_NAME)
            .internalName(CONTAINER_1_INTERNALNAME)
            .image(CONTAINER_1_IMAGE_DTO)
            .created(CONTAINER_1_CREATED)
            .host(CONTAINER_1_HOST)
            .port(CONTAINER_1_PORT)
            .sidecarHost(CONTAINER_1_SIDECAR_HOST)
            .sidecarPort(CONTAINER_1_SIDECAR_PORT)
            .username(CONTAINER_1_PRIVILEGED_USERNAME)
            .password(CONTAINER_1_PRIVILEGED_PASSWORD)
            .build();

    public final static PrivilegedDatabaseDto DATABASE_1_PRIVILEGED_DTO = PrivilegedDatabaseDto.builder()
            .id(DATABASE_1_ID)
            .name(DATABASE_1_NAME)
            .internalName(DATABASE_1_INTERNALNAME)
            .container(CONTAINER_1_PRIVILEGED_DTO)
            .build();

    public final static PrivilegedTableDto TABLE_1_PRIVILEGED_DTO = PrivilegedTableDto.builder()
            .id(TABLE_1_ID)
            .tdbid(DATABASE_1_ID)
            .database(DATABASE_1_PRIVILEGED_DTO)
            .created(TABLE_1_CREATED)
            .internalName(TABLE_1_INTERNALNAME)
            .isVersioned(TABLE_1_VERSIONED)
            .description(TABLE_1_DESCRIPTION)
            .name(TABLE_1_NAME)
            .queueName(TABLE_1_QUEUE_NAME)
            .routingKey(TABLE_1_ROUTING_KEY)
            .identifiers(new LinkedList<>())
            .columns(new LinkedList<>() /* TABLE_1_COLUMNS_DTO */)
            .constraints(ConstraintsDto.builder().build() /* TABLE_1_CONSTRAINTS */)
            .createdBy(USER_1_ID)
            .owner(USER_1_DTO)
            .build();

}
