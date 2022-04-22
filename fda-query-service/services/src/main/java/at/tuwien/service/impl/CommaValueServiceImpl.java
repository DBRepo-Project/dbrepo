package at.tuwien.service.impl;

import at.tuwien.service.CommaValueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CommaValueServiceImpl implements CommaValueService {

//    @Override
//    @Transactional
//    public InputStreamResource export(Long containerId, Long databaseId, Long tableId, Instant timestamp)
//            throws TableNotFoundException, DatabaseNotFoundException, DatabaseConnectionException,
//            TableMalformedException, ImageNotSupportedException, PaginationException, FileStorageException,
//            ContainerNotFoundException {
//        /* find */
//        final Container container = containerService.find(containerId);
//        final Table table = tableService.find(databaseId, tableId);
//        final QueryResultDto result = queryService.findAll(containerId, databaseId, tableId, timestamp, null, null);
//        /* write */
//        final Resource csv = dataMapper.resultTableToResource(result, table);
//        final InputStreamResource resource;
//        try {
//            resource = new InputStreamResource(csv.getInputStream());
//        } catch (IOException e) {
//            log.error("Failed to map resource");
//            throw new FileStorageException("Failed to map resource", e);
//        }
//        log.trace("produced csv {}", csv);
//        return resource;
//    }

//    @Override
//    @Transactional
//    public InputStreamResource export(Long containerId, Long databaseId, Long tableId) throws TableNotFoundException,
//            DatabaseConnectionException, TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException,
//            FileStorageException, PaginationException, ContainerNotFoundException {
//        return export(containerId, databaseId, tableId, Instant.now());
//    }

}
