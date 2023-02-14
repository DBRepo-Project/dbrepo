import io.swagger.client.ApiException;
import io.swagger.client.api.QueryEndpointApi;
import io.swagger.client.model.*;

public class QueryDataApp {

    public static void main(String[] args) throws ApiException {
        final QueryEndpointApi queryEndpointApi = new QueryEndpointApi();
        /* execute query */
        final ExecuteStatementDto executeStatementRequest = new ExecuteStatementDto();
        executeStatementRequest.setStatement("SELECT `uuid`, `point`, `value`, `unit`, `timestamp` FROM `power` WHERE `point` = \"A\"");
        final QueryResultDto response = queryEndpointApi.execute(executeStatementRequest, 1L, 1L, 0L, 10L, null, null);
        response.getId();
        response.getResult();
        response.getResultNumber();
    }

}
