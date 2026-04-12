CREATE SCHEMA dbrepo;

-- procedure to create query store
CREATE TABLE dbrepo.queries
(
    id               VARCHAR(36)  NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    created          TIMESTAMP    NOT NULL             DEFAULT NOW(),
    executed         TIMESTAMP    NOT NULL             default NOW(),
    created_by       VARCHAR(36),
    query            text         NOT NULL,
    query_normalized text         NOT NULL,
    is_persisted     boolean      NOT NULL,
    query_hash       VARCHAR(255) NOT NULL,
    result_hash      VARCHAR(255),
    result_number    bigint
);
SELECT periods.add_system_time_period('dbrepo.queries', 'row_start', 'row_end');
SELECT periods.add_system_versioning('dbrepo.queries');

-- procedure to persist query in query store
CREATE OR REPLACE PROCEDURE dbrepo.persist(IN query_id VARCHAR(36), IN persisted BOOLEAN)
    LANGUAGE plpgsql AS
$$
BEGIN
    UPDATE dbrepo.queries SET is_persisted = persisted WHERE id = query_id;
END
$$;

-- procedure to store query
CREATE OR REPLACE PROCEDURE dbrepo.store_query(IN query TEXT, IN normalized_query TEXT, IN executed TIMESTAMP,
                                               OUT query_id VARCHAR(36))
    LANGUAGE plpgsql AS
$$
DECLARE
    _query_hash    VARCHAR(64);
    _result_hash   VARCHAR(64);
    _result        TEXT := concat('CREATE TEMPORARY TABLE result AS ', normalized_query);
    _result_number BIGINT;
BEGIN
    IF query IS NULL OR normalized_query IS NULL OR executed IS NULL THEN
        RAISE EXCEPTION 'input cannot be null';
    END IF;
    SELECT * FROM hash_query(normalized_query) INTO _query_hash;
    EXECUTE _result;
    SELECT * FROM hash_table('result') INTO _result_hash;
    IF _query_hash IS NULL THEN
        INSERT INTO dbrepo.queries (created_by, query, query_normalized, is_persisted, query_hash, result_hash,
                                    result_number, executed)
        SELECT CURRENT_USER,
               query,
               normalized_query,
               false,
               _query_hash,
               _result_hash,
               _result_number,
               executed
        WHERE NOT EXISTS (SELECT id FROM dbrepo.queries WHERE query_hash = _query_hash AND result_hash IS NULL);
        query_id := (SELECT id FROM dbrepo.queries WHERE query_hash = _query_hash AND result_hash IS NULL);
    ELSE
        INSERT INTO dbrepo.queries (created_by, query, query_normalized, is_persisted, query_hash, result_hash,
                                    result_number, executed)
        SELECT CURRENT_USER,
               query,
               query,
               false,
               _query_hash,
               _result_hash,
               _result_number,
               executed
        WHERE NOT EXISTS (SELECT id FROM dbrepo.queries WHERE query_hash = _query_hash AND result_hash = _query_hash);
        query_id := (SELECT id FROM dbrepo.queries WHERE query_hash = _query_hash AND result_hash = _query_hash);
    END IF;
END
$$;

-- procedure to hash a data table
CREATE PROCEDURE hash_table(IN name VARCHAR(255), OUT hash VARCHAR(64))
    LANGUAGE plpgsql AS
$$
DECLARE
    _result TEXT := concat(
            'SELECT replace(sha256(string_agg(concat_ws('','', (select string_agg(column_name::text, '','' order by column_name) from information_schema.columns WHERE table_name = ',
            name, ')), '','')::bytea)::text, ''\x'', '''') FROM ', name);
BEGIN
    EXECUTE _result INTO hash;
END;
$$;

-- procedure to query system-versioned data
CREATE PROCEDURE hash_query(IN normalized_query TEXT, OUT hash VARCHAR(64))
    LANGUAGE plpgsql AS
$$
BEGIN
    SELECT replace(sha256(normalized_query::bytea)::text, '\x', '') INTO hash;
END;
$$;