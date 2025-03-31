package at.tuwien.service;


import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.database.table.TableDto;

import java.util.List;
import java.util.Map;


public interface TableService {

    List<TableBriefDto> getAllTables(Long dbId);
    TableDto getTableSchemas(Long dbId, Long tId);
    List<Map<String, Object>> getTableData(Long dbId, Long tId, Long size);
}
