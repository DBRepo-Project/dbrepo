package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.ViewNotificationDto;
import at.ac.tuwien.ifs.dbrepo.service.ViewService;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ViewServiceMariaDbImpl implements ViewService {

    private final ReplicationService replicationService;

    @Override
    public void handleViewReplication(ViewNotificationDto viewNotificationDto) {
        replicationService.sendViewReplicationToInstances(viewNotificationDto);
    }

    @Override
    public String rewriteViewQueryWithReplicationTimestamps(ViewDto viewDto) {
        try {
            if (viewDto == null || viewDto.getQuery() == null) {
                return null;
            }

            final String creationLocation = viewDto.getCreationLocation();
            if (creationLocation == null || creationLocation.isEmpty()) {
                // Nothing to rewrite if there is no creation location
                return viewDto.getQuery();
            }

            final Instant createdTimestamp = viewDto.getCreated() != null ? viewDto.getCreated() : Instant.now();
            final String executionTimestampStr = createdTimestamp
                    .atZone(ZoneId.of("UTC"))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            String originalQuery = viewDto.getQuery();
            String modifiedQuery = originalQuery;

            // Extract table names (basic FROM/JOIN scanner similar to subset service)
            Set<String> tableNames = extractTableNames(originalQuery);

            for (String tableName : tableNames) {
                // Build JOIN clause targeting tuple_replication_timestamps for this table
                String alias = tableName; // keep original table name as reference
                String joinClause = String.format(
                        "JOIN tuple_replication_timestamps trt_%s ON trt_%s.site_url = '%s' " +
                        "AND trt_%s.replication_id = %s.replication_key " +
                        "AND trt_%s.row_start <= '%s' " +
                        "AND trt_%s.row_end > '%s'",
                        alias, alias, creationLocation,
                        alias, tableName,
                        alias, executionTimestampStr,
                        alias, executionTimestampStr
                );

                modifiedQuery = insertJoinAfterBaseTable(modifiedQuery, tableName, alias, joinClause);
            }

            log.info("Original view query: {}", originalQuery);
            log.info("Rewritten view query: {}", modifiedQuery);

            return modifiedQuery;
        } catch (Exception e) {
            log.warn("Failed to rewrite view query: {}", e.getMessage());
            return viewDto.getQuery();
        }
    }

    private String insertJoinAfterBaseTable(String query, String tableName, String tableAlias, String joinClause) {
        Pattern fromPattern = Pattern.compile("(?i)FROM\\s+`?([a-zA-Z_][a-zA-Z0-9_]*)`?");
        Matcher fromMatcher = fromPattern.matcher(query);

        if (fromMatcher.find()) {
            int tableEnd = fromMatcher.end();
            String beforeJoin = query.substring(0, tableEnd);
            String afterJoin = query.substring(tableEnd);

            String joinWithSpace = afterJoin.trim().startsWith("JOIN") ? joinClause : " " + joinClause;
            return beforeJoin + joinWithSpace + afterJoin;
        }

        // If no FROM clause match was found, return original query unchanged
        return query;
    }

    private Set<String> extractTableNames(String sqlQuery) {
        Set<String> tableNames = new HashSet<>();
        Pattern pattern = Pattern.compile("(?i)(?:FROM|JOIN)\\s+`?([a-zA-Z_][a-zA-Z0-9_]*)`?");
        Matcher matcher = pattern.matcher(sqlQuery);
        while (matcher.find()) {
            tableNames.add(matcher.group(1));
        }
        return tableNames;
    }
}

