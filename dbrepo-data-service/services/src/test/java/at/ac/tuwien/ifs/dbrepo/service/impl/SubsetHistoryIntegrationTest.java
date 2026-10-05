package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.TupleReplicationTimestampDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.View;
import at.ac.tuwien.ifs.dbrepo.core.exception.SubsetHistoryIncompleteException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "TUPLE_VERSION_SQL_PORT", matches = "[0-9]+")
class SubsetHistoryIntegrationTest {
    private static final String A="https://a.example", B="https://b.example";
    private static final Instant T=Instant.parse("2026-01-01T10:03:00.123456Z");
    private final UUID aId=UUID.randomUUID(), bId=UUID.randomUUID();
    private final Table aTable=Table.builder().id(UUID.randomUUID()).internalName("measurements").creationLocation(A).build();
    private final Table bTable=Table.builder().id(UUID.randomUUID()).internalName("measurements").creationLocation(A).build();

    private Connection connection(String schema) throws SQLException {
        return DriverManager.getConnection("jdbc:mariadb://127.0.0.1:"+System.getenv("TUPLE_VERSION_SQL_PORT")+"/"+schema,
                "root", System.getenv("TUPLE_VERSION_SQL_PASSWORD"));
    }

    private Database database(String schema, UUID id, Table table) {
        return Database.builder().id(id).internalName(schema).tables(List.of(table)).views(List.of()).build();
    }

    @BeforeEach void setup() throws Exception {
        try (var c=connection(""); var s=c.createStatement()) {
            for (String schema:List.of("subset_history_a","subset_history_b")) {
                s.execute("DROP DATABASE IF EXISTS "+schema); s.execute("CREATE DATABASE "+schema);
            }
        }
        for (var entry:Map.of("subset_history_a",aTable,"subset_history_b",bTable).entrySet()) {
            try (var c=connection(entry.getKey()); var s=c.createStatement()) {
                s.execute("CREATE TABLE measurements(pk INT,value INT,replication_key VARCHAR(255),"
                        +"ROW_START TIMESTAMP(6) GENERATED ALWAYS AS ROW START,ROW_END TIMESTAMP(6) GENERATED ALWAYS AS ROW END,"
                        +"PERIOD FOR SYSTEM_TIME(ROW_START,ROW_END)) WITH SYSTEM VERSIONING");
                TupleVersionHistory.prepare(c,entry.getValue()); TupleVersionHistory.prepareImported(c,entry.getValue());
                s.execute("UPDATE tuple_visibility_counter SET sequence=5");
            }
        }
    }

    private void retain(Connection c, Table table, UUID version, int key, int value) throws Exception {
        TupleVersionHistory.retain(c,table,version,Map.of("pk",key,"value",value,"replication_key","K"+key));
    }

    private void interval(Connection c, String site, UUID db, Table table, UUID version, int key,
                          Instant start, Instant end, long sequence, Long endSequence) throws Exception {
        ReplicationTimestampServiceMariaDbImpl.upsertTimestamp(c,TupleReplicationTimestampDto.builder()
                .siteUrl(site).databaseId(db).tableId(table.getId()).replicationId("K"+key).versionId(version)
                .rowStart(start).rowEnd(end).visibilityStart(sequence).visibilityEnd(endSequence).build());
    }

    private List<Integer> values(Connection c,String sql) throws Exception {
        final List<Integer> values=new ArrayList<>();
        try(var s=c.createStatement();var rows=s.executeQuery(sql)){while(rows.next())values.add(rows.getInt(1));}
        return values;
    }

    @Test void delayedInsertReproducesTheActualExecutionSiteNotTheTargetsOldState() throws Exception {
        final List<UUID> versions=new ArrayList<>(); for(int i=0;i<5;i++)versions.add(UUID.randomUUID());
        try(var a=connection("subset_history_a");var b=connection("subset_history_b")) {
            a.setAutoCommit(false);b.setAutoCommit(false);
            for(int i=0;i<5;i++) {
                retain(a,aTable,versions.get(i),i,i);
                interval(a,A,aId,aTable,versions.get(i),i,T.minusSeconds(60),null,i+1,null);
                if(i<3){retain(b,bTable,versions.get(i),i,i);
                    interval(b,A,aId,aTable,versions.get(i),i,T.minusSeconds(60),null,i+1,null);
                    interval(b,B,bId,bTable,versions.get(i),i,T.minusSeconds(30),null,i+1,null);}
            }
            a.commit();b.commit();a.setAutoCommit(true);b.setAutoCommit(true);
            final var queryA=SubsetHistory.capture(a,database("subset_history_a",aId,aTable),A,"select value from measurements order by pk",T);
            final var queryB=SubsetHistory.capture(b,database("subset_history_b",bId,bTable),B,"select value from measurements order by pk",T);
            assertEquals(List.of(0,1,2,3,4),values(a,queryA.sql())); assertEquals(List.of(0,1,2),values(b,queryB.sql()));
            assertThrows(SubsetHistoryIncompleteException.class,()->SubsetHistory.replay(b,database("subset_history_b",bId,bTable),
                    A,T,queryA.context(),"select value from measurements order by pk",B));
            b.setAutoCommit(false);
            for(int i=3;i<5;i++) {retain(b,bTable,versions.get(i),i,i);
                interval(b,A,aId,aTable,versions.get(i),i,T.minusSeconds(60),null,i+1,null);
                interval(b,B,bId,bTable,versions.get(i),i,T.plusSeconds(60),null,i+1,null);}
            b.commit(); b.setAutoCommit(true);
            assertEquals(List.of(0,1,2,3,4),values(b,SubsetHistory.replay(b,database("subset_history_b",bId,bTable),
                    A,T,queryA.context(),"select value from measurements order by pk",B)));
            assertEquals(List.of(0,1,2),values(b,SubsetHistory.replay(b,database("subset_history_b",bId,bTable),
                    B,T,queryB.context(),"select value from measurements order by pk",B)));
        }
    }

    @Test void delayedUpdateAndLaterDeleteSelectTheOriginalSitesValuesVersion() throws Exception {
        final UUID old=UUID.randomUUID(),updated=UUID.randomUUID();
        try(var a=connection("subset_history_a");var b=connection("subset_history_b")) {
            a.setAutoCommit(false);b.setAutoCommit(false);
            for(var c:List.of(a,b)) {
                final Table local=c==a?aTable:bTable;
                retain(c,local,old,1,10); retain(c,local,updated,1,20);
                interval(c,A,aId,aTable,old,1,T.minusSeconds(180),T.minusSeconds(60),1,2L);
                interval(c,A,aId,aTable,updated,1,T.minusSeconds(60),null,2,null);
            }
            interval(b,B,bId,bTable,old,1,T.minusSeconds(120),T.plusSeconds(60),1,2L);
            interval(b,B,bId,bTable,updated,1,T.plusSeconds(60),null,2,null);
            a.commit();b.commit();a.setAutoCommit(true);b.setAutoCommit(true);
            final var original=SubsetHistory.capture(a,database("subset_history_a",aId,aTable),A,"select value from measurements",T);
            assertEquals(List.of(20),values(b,SubsetHistory.replay(b,database("subset_history_b",bId,bTable),A,T,
                    original.context(),"select value from measurements",B)));
            b.setAutoCommit(false);interval(b,A,aId,aTable,updated,1,T.minusSeconds(60),T.plusSeconds(300),2,6L);b.commit();b.setAutoCommit(true);
            assertEquals(List.of(20),values(b,SubsetHistory.replay(b,database("subset_history_b",bId,bTable),A,T,
                    original.context(),"select value from measurements",B)));
        }
    }

    @Test void historicalViewDefinitionAndJoinsAreRewrittenToTheSameVersionRelations() throws Exception {
        try(var a=connection("subset_history_a")) {
            a.setAutoCommit(false);final UUID version=UUID.randomUUID();retain(a,aTable,version,1,20);
            interval(a,A,aId,aTable,version,1,T.minusSeconds(60),null,1,null);a.commit();a.setAutoCommit(true);
            final var db=database("subset_history_a",aId,aTable);
            db.setViews(List.of(View.builder().internalName("recorded").query("select pk,value from measurements").build()));
            final String query="select v.value from recorded v join measurements m on v.pk=m.pk";
            final var original=SubsetHistory.capture(a,db,A,query,T);
            db.setViews(List.of(View.builder().internalName("recorded").query("select 999 as value").build()));
            assertEquals(List.of(20),values(a,SubsetHistory.replay(a,db,A,T,original.context(),query,A)));
        }
    }
}
