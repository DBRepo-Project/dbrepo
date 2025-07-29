-- Migration 111: Add replica URLs table for databases
-- This migration adds support for storing replica URLs for databases

CREATE TABLE mdb_databases_replica_urls (
    database_id VARCHAR(36) NOT NULL,
    replica_url TEXT NOT NULL,
    PRIMARY KEY (database_id, replica_url),
    FOREIGN KEY (database_id) REFERENCES mdb_databases(id) ON DELETE CASCADE
); 