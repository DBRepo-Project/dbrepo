import io.swagger.client.ApiException;
import io.swagger.client.api.TableDataEndpointApi;
import io.swagger.client.api.TableEndpointApi;
import io.swagger.client.model.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class InsertDataApp {

    public static void main(String[] args) {
        final TableEndpointApi tableEndpointApi = new TableEndpointApi();
        /* create table */
        final TableCreateDto tableCreateRequest = new TableCreateDto();
        tableCreateRequest.setName("Power");
        tableCreateRequest.setDescription("Power consumption in the Pilot Factory");
        final List<ColumnCreateDto> columns = new LinkedList<>();
        columns.add(column("UUID", ColumnCreateDto.TypeEnum.STRING, null, true, true, false));
        columns.add(column("Point", ColumnCreateDto.TypeEnum.STRING, null, true, true, false));
        columns.add(column("Value", ColumnCreateDto.TypeEnum.DECIMAL, null, true, true, false));
        columns.add(column("Unit", ColumnCreateDto.TypeEnum.STRING, null, true, true, false));
        columns.add(column("Timestamp", ColumnCreateDto.TypeEnum.TIMESTAMP, 1L, true, true, false));
        tableCreateRequest.setColumns(columns);
        final TableBriefDto table;
        try {
            table = tableEndpointApi.create(tableCreateRequest, 1L, 1L);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    private static void insertTuple(Long containerId, Long databaseId, Long tableId) throws ApiException {
        final TableDataEndpointApi tableDataEndpointApi = new TableDataEndpointApi();
        /* insert data */
        final TableCsvDto tuple = new TableCsvDto();
        tuple.data(new HashMap<String, Object>(){{
            put("uuid", "56873eb0-aacb-11ed-afa1-0242ac120002");
            put("point", "A");
            put("value", 54.3212);
            put("unit", "W");
            put("timestamp", Instant.now());
        }});
        tableDataEndpointApi.insert(tuple, containerId, databaseId, tableId);
    }

    private static void insertBulk(Long containerId, Long databaseId, Long tableId) throws ApiException {
        final TableDataEndpointApi tableDataEndpointApi = new TableDataEndpointApi();
        /* insert data */
        final ImportDto csv = new ImportDto();
        csv.setLocation("/path/to/data.csv");
        csv.setQuote("\"");
        csv.setNullElement("NA");
        csv.setSeparator(",");
        tableDataEndpointApi.importCsv(csv, containerId, databaseId, tableId);
    }

    private static ColumnCreateDto column(String name, ColumnCreateDto.TypeEnum type, Long dateFormatId,
                                          Boolean primaryKey, Boolean unique, Boolean nulled) {
        final ColumnCreateDto columnCreateRequest = new ColumnCreateDto();
        columnCreateRequest.setName(name);
        columnCreateRequest.setType(type);
        columnCreateRequest.setNullAllowed(nulled);
        columnCreateRequest.setPrimaryKey(primaryKey);
        columnCreateRequest.setUnique(unique);
        columnCreateRequest.setDfid(dateFormatId);
        return columnCreateRequest;
    }

}
