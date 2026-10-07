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
    private static final String A="https://a.example", B="https://b.example", C="https://c.example";
    private static final Instant T=Instant.parse("2026-01-01T10:03:00.123456Z");
    private final UUID aId=UUID.randomUUID(), bId=UUID.randomUUID(), cId=UUID.randomUUID();
    private final Table aTable=Table.builder().id(UUID.randomUUID()).internalName("measurements").creationLocation(A).build();
    private final Table bTable=Table.builder().id(UUID.randomUUID()).internalName("measurements").creationLocation(A).build();
    private final Table cTable=Table.builder().id(UUID.randomUUID()).internalName("measurements").creationLocation(A).build();

    private Connection connection(String schema) throws SQLException {
        return DriverManager.getConnection("jdbc:mariadb://127.0.0.1:"+System.getenv("TUPLE_VERSION_SQL_PORT")+"/"+schema,
                "root", System.getenv("TUPLE_VERSION_SQL_PASSWORD"));
    }

    private Database database(String schema, UUID id, Table table) {
        return Database.builder().id(id).internalName(schema).tables(List.of(table)).views(List.of()).build();
    }

    @BeforeEach void setup() throws Exception {
        aTable.setReplicaUrls(Map.of(B,bTable.getId(),C,cTable.getId()));
        bTable.setReplicaUrls(Map.of(A,aTable.getId(),C,cTable.getId()));
        cTable.setReplicaUrls(Map.of(A,aTable.getId(),B,bTable.getId()));
        try (var c=connection(""); var s=c.createStatement()) {
            for (String schema:List.of("subset_history_a","subset_history_b","subset_history_c")) {
                s.execute("DROP DATABASE IF EXISTS "+schema); s.execute("CREATE DATABASE "+schema);
            }
        }
        for (var entry:Map.of("subset_history_a",aTable,"subset_history_b",bTable,"subset_history_c",cTable).entrySet()) {
            try (var c=connection(entry.getKey()); var s=c.createStatement()) {
                s.execute("CREATE TABLE measurements(pk INT,value INT,replication_key VARCHAR(255),"
                        +"ROW_START TIMESTAMP(6) GENERATED ALWAYS AS ROW START,ROW_END TIMESTAMP(6) GENERATED ALWAYS AS ROW END,"
                        +"PERIOD FOR SYSTEM_TIME(ROW_START,ROW_END)) WITH SYSTEM VERSIONING");
                TupleVersionHistory.prepare(c,entry.getValue()); TupleVersionHistory.prepareImported(c,entry.getValue());
                s.execute("UPDATE tuple_visibility_counter SET sequence=5");
            }
        }
    }

    private void retain(Connection c, Table table, Instant version, int key, int value) throws Exception {
        TupleVersionHistory.retain(c,table,version,Map.of("pk",key,"value",value,"replication_key","K"+key));
    }

    private void interval(Connection c, String site, UUID db, Table table, Instant version, int key,
                          Instant start, Instant end, long sequence, Long endSequence) throws Exception {
        ReplicationTimestampServiceMariaDbImpl.upsertTimestamp(c,TupleReplicationTimestampDto.builder()
                .siteUrl(site).databaseId(db).tableId(table.getId()).replicationId("K"+key).masterSiteTs(version)
                .rowStart(start).rowEnd(end).visibilityStart(sequence).visibilityEnd(endSequence).build());
    }

    private List<Integer> values(Connection c,String sql) throws Exception {
        final List<Integer> values=new ArrayList<>();
        try(var s=c.createStatement();var rows=s.executeQuery(sql)){while(rows.next())values.add(rows.getInt(1));}
        return values;
    }

    @Test void emptyReplicaNeedsNoPriorTupleDeliveryAndMissingEvidenceStillFailsClosed() throws Exception {
        try (var a=connection("subset_history_a"); var b=connection("subset_history_b"); var ddl=b.createStatement()) {
            final var empty=SubsetHistory.capture(a,database("subset_history_a",aId,aTable),A,"select value from measurements",T);
            ddl.execute("DROP TABLE tuple_replication_versions,tuple_replication_timestamps,tuple_visibility_counter,"
                    +TupleVersionHistory.importedName(bTable));
            assertEquals(List.of(),values(b,SubsetHistory.replay(b,database("subset_history_b",bId,bTable),
                    A,T,empty.context(),"select value from measurements",B)));
            a.setAutoCommit(false);
            retain(a,aTable,T.minusSeconds(60),1,20);
            interval(a,A,aId,aTable,T.minusSeconds(60),1,T.minusSeconds(60),null,1,null);
            a.commit();a.setAutoCommit(true);
            final var nonempty=SubsetHistory.capture(a,database("subset_history_a",aId,aTable),A,"select value from measurements",T);
            assertThrows(SubsetHistoryIncompleteException.class,()->SubsetHistory.replay(b,database("subset_history_b",bId,bTable),
                    A,T,nonempty.context(),"select value from measurements",B));
        }
    }

    @Test void delayedInsertReproducesTheActualExecutionSiteNotTheTargetsOldState() throws Exception {
        final List<Instant> versions=Collections.nCopies(5,T.minusSeconds(60));
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
            a.setAutoCommit(false);
            for(int i=0;i<5;i++) interval(a,B,bId,bTable,versions.get(i),i,
                    i<3?T.minusSeconds(30):T.plusSeconds(60),null,i+1,null);
            a.commit(); a.setAutoCommit(true);
            assertEquals(List.of(0,1,2),values(a,SubsetHistory.replay(a,database("subset_history_a",aId,aTable),
                    B,T,queryB.context(),"select value from measurements order by pk",A)));
            try(var c=connection("subset_history_c")) {
                c.setAutoCommit(false);
                for(int i=0;i<5;i++) {
                    retain(c,cTable,versions.get(i),i,i);
                    interval(c,A,aId,aTable,versions.get(i),i,T.minusSeconds(60),null,i+1,null);
                    interval(c,B,bId,bTable,versions.get(i),i,i<3?T.minusSeconds(30):T.plusSeconds(60),null,i+1,null);
                }
                c.commit();c.setAutoCommit(true);
                assertEquals(List.of(0,1,2,3,4),values(c,SubsetHistory.replay(c,database("subset_history_c",cId,cTable),
                        A,T,queryA.context(),"select value from measurements order by pk",C)));
                assertEquals(List.of(0,1,2),values(c,SubsetHistory.replay(c,database("subset_history_c",cId,cTable),
                        B,T,queryB.context(),"select value from measurements order by pk",C)));
            }
        }
    }

    @Test void updatesCommittedAfterTheObservedCutCannotChangeTheResultEvenWithAnEarlierNativeTimestamp() throws Exception {
        try(var a=connection("subset_history_a")) {
            a.setAutoCommit(false);
            final Instant original=T.minusSeconds(60),later=T.minusSeconds(30);
            retain(a,aTable,original,1,20);
            interval(a,A,aId,aTable,original,1,T.minusSeconds(60),null,2,null);
            a.commit();a.setAutoCommit(true);
            final var query=SubsetHistory.capture(a,database("subset_history_a",aId,aTable),A,"select value from measurements",T);
            a.setAutoCommit(false);
            interval(a,A,aId,aTable,original,1,T.minusSeconds(60),T.minusSeconds(30),2,6L);
            retain(a,aTable,later,1,10);
            interval(a,A,aId,aTable,later,1,T.minusSeconds(30),null,6,null);
            a.commit();a.setAutoCommit(true);
            assertEquals(List.of(20),values(a,SubsetHistory.replay(a,database("subset_history_a",aId,aTable),A,T,
                    query.context(),"select value from measurements",A)));
        }
    }

    @Test void unknownNativeReplicaVersionsCannotSilentlyBecomeAnEmptySubset() throws Exception {
        try(var b=connection("subset_history_b");var s=b.createStatement()) {
            s.execute("INSERT INTO measurements(pk,value,replication_key) VALUES(1,20,'K1')");
            assertThrows(SubsetHistoryIncompleteException.class,()->SubsetHistory.capture(b,
                    database("subset_history_b",bId,bTable),B,"select value from measurements",Instant.now().plusSeconds(1)));
        }
    }

    @Test void emptyResultsAndTechnicalColumnExclusionUseTheSameHistoricalRelation() throws Exception {
        try(var a=connection("subset_history_a")) {
            final var query=SubsetHistory.capture(a,database("subset_history_a",aId,aTable),A,"select * from measurements",T);
            try(var s=a.createStatement();var rows=s.executeQuery(query.sql())) {
                assertFalse(rows.next()); assertEquals(2,rows.getMetaData().getColumnCount());
                assertEquals("pk",rows.getMetaData().getColumnLabel(1));
            }
        }
    }

    @Test void enumAndSetKeepTheirNativeOrderingAndNumericMeaning() throws Exception {
        try(var a=connection("subset_history_a");var s=a.createStatement()) {
            s.execute("SET SESSION system_versioning_alter_history=KEEP");
            s.execute("ALTER TABLE measurements ADD category ENUM('z','a'), ADD tags SET('z','a')");
            s.execute("DROP TABLE "+TupleVersionHistory.importedName(aTable));
            TupleVersionHistory.prepareImported(a,aTable);
            s.execute("INSERT INTO measurements(pk,value,replication_key,category,tags) VALUES(1,20,'K1','z','z')");
            a.setAutoCommit(false);
            final Instant imported=T.minusSeconds(60);
            TupleVersionHistory.retain(a,aTable,imported,Map.of("pk",2,"value",20,"replication_key","K2","category","a","tags","a"));
            interval(a,A,aId,aTable,imported,2,T.minusSeconds(60),null,1,null);
            a.commit();a.setAutoCommit(true);
            final Instant selected=Instant.now();
            final var query=SubsetHistory.capture(a,database("subset_history_a",aId,aTable),A,
                    "select pk,category+0,tags+0 from measurements order by category,pk",selected);
            try(var rows=s.executeQuery(query.sql())) {
                assertTrue(rows.next());assertEquals(1,rows.getInt(1));assertEquals(1,rows.getInt(2));assertEquals(1,rows.getInt(3));
                assertTrue(rows.next());assertEquals(2,rows.getInt(1));assertEquals(2,rows.getInt(2));assertEquals(2,rows.getInt(3));
                assertFalse(rows.next());
            }
        }
    }

    @Test void replayAndSparkConnectionsUseUtcRegardlessOfTheJvmTimeZone() throws Exception {
        try(var a=connection("subset_history_a");var s=a.createStatement()) {
            a.setAutoCommit(false);final Instant version=T.minusSeconds(60);retain(a,aTable,version,1,20);
            interval(a,A,aId,aTable,version,1,T.minusSeconds(60),null,1,null);a.commit();a.setAutoCommit(true);
            final var query=SubsetHistory.capture(a,database("subset_history_a",aId,aTable),A,"select value from measurements",T);
            s.execute("SET time_zone='-03:00'");
            assertEquals(List.of(20),values(a,SubsetHistory.replay(a,database("subset_history_a",aId,aTable),A,T,
                    query.context(),"select value from measurements",A)));
        }
        final var container=at.ac.tuwien.ifs.dbrepo.core.entity.cache.Container.builder().host("127.0.0.1")
                .port(Integer.valueOf(System.getenv("TUPLE_VERSION_SQL_PORT")))
                .image(at.ac.tuwien.ifs.dbrepo.core.entity.cache.Image.builder().jdbcMethod("mariadb").build()).build();
        final String url=new DataConnector(){}.getSparkJdbcUrl(container,"subset_history_a");
        try(var c=DriverManager.getConnection(url,"root",System.getenv("TUPLE_VERSION_SQL_PASSWORD"));
            var s=c.createStatement();var r=s.executeQuery("SELECT @@session.time_zone")) {
            assertTrue(r.next());assertEquals("+00:00",r.getString(1));
        }
    }

    @Test void delayedUpdateAndLaterDeleteSelectTheOriginalSitesValuesVersion() throws Exception {
        final Instant old=T.minusSeconds(180),updated=T.minusSeconds(60);
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

    @Test void masterTimestampMapsToTheRightNativeVersionDespiteDifferentArrivalTimes() throws Exception {
        try (var a=connection("subset_history_a");var b=connection("subset_history_b")) {
            final Instant first=nativeChange(a,aTable,A,aId,10,null,true);
            nativeChange(b,bTable,B,bId,10,first,true);
            final Instant second=nativeChange(a,aTable,A,aId,20,null,false);
            final Instant selected;
            try(var s=a.createStatement();var r=s.executeQuery("SELECT UTC_TIMESTAMP(6)")) {
                assertTrue(r.next());selected=r.getTimestamp(1,TupleVersionHistory.utc()).toInstant();
            }
            final var original=SubsetHistory.capture(a,database("subset_history_a",aId,aTable),A,
                    "select value from measurements",selected);
            final Instant third=nativeChange(a,aTable,A,aId,30,null,false);
            final Instant localSecond=nativeChange(b,bTable,B,bId,20,second,false);
            nativeChange(b,bTable,B,bId,30,third,false);
            assertNotEquals(second,localSecond);
            b.setAutoCommit(false);
            for(Instant master:List.of(first,second,third)) {
                for(var timestamp:TupleVersionHistory.visibility(a,aTable,"K1",master)) {
                    ReplicationTimestampServiceMariaDbImpl.upsertTimestamp(b,timestamp);
                }
            }
            b.commit();b.setAutoCommit(true);
            assertEquals(List.of(20),values(b,SubsetHistory.replay(b,database("subset_history_b",bId,bTable),
                    A,selected,original.context(),"select value from measurements",B)));
            assertEquals(List.of(30),values(b,"SELECT value FROM measurements"));
        }
    }

    private Instant nativeChange(Connection c,Table table,String site,UUID database,int value,Instant master,boolean insert)
            throws Exception {
        c.setAutoCommit(false);
        try(var s=c.createStatement()) {
            s.executeUpdate(insert ? "INSERT INTO measurements VALUES(1,"+value+",'K1',DEFAULT,DEFAULT)"
                    : "UPDATE measurements SET value="+value+" WHERE replication_key='K1'");
            final Instant local;
            try(var r=s.executeQuery("SELECT ROW_START FROM measurements WHERE replication_key='K1'")) {
                assertTrue(r.next());local=r.getTimestamp(1,TupleVersionHistory.utc()).toInstant();
            }
            final var tuple=at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleWithTimestampsDto.builder()
                    .replicationKey("K1").insertedAt(local).build();
            TupleVersionHistory.record(c,site,database,table.getId(),tuple,
                    insert?org.springframework.http.HttpMethod.POST:org.springframework.http.HttpMethod.PUT,master==null?local:master);
            c.commit();c.setAutoCommit(true);
            return local;
        }
    }

    @Test void historicalViewDefinitionAndJoinsAreRewrittenToTheSameVersionRelations() throws Exception {
        try(var a=connection("subset_history_a")) {
            a.setAutoCommit(false);final Instant version=T.minusSeconds(60);retain(a,aTable,version,1,20);
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
