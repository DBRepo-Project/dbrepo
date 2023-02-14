import io.swagger.client.ApiException;
import io.swagger.client.api.ContainerEndpointApi;
import io.swagger.client.api.DatabaseEndpointApi;
import io.swagger.client.model.*;

public class CreateDatabaseApp {

    public static void main(String[] args) throws InterruptedException {
        final ContainerEndpointApi containerEndpointApi = new ContainerEndpointApi();
        final DatabaseEndpointApi databaseEndpointApi = new DatabaseEndpointApi();
        /* create container */
        final ContainerCreateRequestDto containerCreateRequest = new ContainerCreateRequestDto();
        containerCreateRequest.setName("Pilot Factory Data");
        containerCreateRequest.setRepository("mariadb");
        containerCreateRequest.setTag("10.5");
        final ContainerBriefDto container;
        try {
            container = containerEndpointApi.create1(containerCreateRequest);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
        /* start container */
        final ContainerChangeDto containerChangeRequest = new ContainerChangeDto();
        containerChangeRequest.action(ContainerChangeDto.ActionEnum.START);
        try {
            containerEndpointApi.modify(containerChangeRequest, container.getId());
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
        Thread.sleep(5 * 1000) /* wait 5 seconds */;
        /* create database */
        final DatabaseCreateDto databaseCreateRequest = new DatabaseCreateDto();
        databaseCreateRequest.setName("Pilot Factory Data");
        databaseCreateRequest.setIsPublic(true);
        final DatabaseBriefDto database;
        try {
            database = databaseEndpointApi.create(databaseCreateRequest, container.getId());
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

}
