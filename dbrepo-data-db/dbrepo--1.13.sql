CREATE SCHEMA dbrepo;

-- procedure to create query store
CREATE TABLE dbrepo.queries
(
    id               VARCHAR(36) NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    created          TIMESTAMP   NOT NULL             DEFAULT NOW(),
    executed         TIMESTAMP   NOT NULL             default NOW(),
    created_by       VARCHAR(36),
    query            text        NOT NULL,
    query_normalized text        NOT NULL,
    is_persisted     boolean     NOT NULL,
    query_hash       VARCHAR(64) NOT NULL,
    result_hash      VARCHAR(64),
    result_number    bigint
);
SELECT periods.add_system_time_period('dbrepo.queries', 'row_start', 'row_end');
SELECT periods.add_system_versioning('dbrepo.queries');

-- procedure to persist query in query store
CREATE PROCEDURE dbrepo.persist(IN query_id VARCHAR(36), IN persisted BOOLEAN)
    LANGUAGE plpgsql AS
$$
BEGIN
    UPDATE dbrepo.queries SET is_persisted = persisted WHERE id = query_id;
END
$$;

-- procedure to store query
CREATE PROCEDURE dbrepo.store_query(IN query TEXT, IN query_normalized TEXT, IN executed TIMESTAMP,
                                    OUT query_id VARCHAR(36))
    LANGUAGE plpgsql AS
$$
DECLARE
    _query_hash    VARCHAR(64);
    _result_hash   VARCHAR(64);
    _result_number BIGINT;
BEGIN
    IF query IS NULL OR query_normalized IS NULL OR executed IS NULL THEN
        RAISE EXCEPTION 'input cannot be null';
    END IF;
    CALL dbrepo.hash_query_statement(query_normalized, _query_hash);
    CALL dbrepo.hash_query_result(query_normalized, _result_hash);
    IF _result_hash IS NULL THEN
        INSERT INTO dbrepo.queries (created_by, query, query_normalized, is_persisted, query_hash, result_hash,
                                    result_number, executed)
        SELECT CURRENT_USER,
               query,
               query_normalized,
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
               query_normalized,
               false,
               _query_hash,
               _result_hash,
               _result_number,
               executed
        WHERE NOT EXISTS (SELECT id FROM dbrepo.queries WHERE query_hash = _query_hash AND result_hash = _result_hash);
        query_id := (SELECT id FROM dbrepo.queries WHERE query_hash = _query_hash AND result_hash = _result_hash);
    END IF;
END
$$;

-- procedure to hash a data table
CREATE PROCEDURE dbrepo.hash_query_result(IN query_normalized TEXT, OUT hash VARCHAR(64))
    LANGUAGE plpgsql AS
$$
DECLARE
    _statement TEXT := concat('CREATE TEMPORARY TABLE result AS ', query_normalized);
BEGIN
    DROP TABLE IF EXISTS result;
    EXECUTE _statement;
    SELECT replace(sha256(string_agg(concat_ws(',', (select string_agg(column_name::text, ',' ORDER BY column_name)
                                                     FROM information_schema.columns
                                                     WHERE table_name = 'result')), ',')::bytea)::text, '\x', '')
    FROM result
    INTO hash;
END;
$$;

-- procedure to query system-versioned data
CREATE PROCEDURE dbrepo.hash_query_statement(IN query_normalized TEXT, OUT hash VARCHAR(64))
    LANGUAGE plpgsql AS
$$
BEGIN
    SELECT replace(sha256(query_normalized::bytea)::text, '\x', '') INTO hash;
END;
$$;