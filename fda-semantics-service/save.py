import logging
import os
import mariadb


def insert_mdb_concepts(uri, c_name) -> int:
    try:
        # Connecting to metadatabase
        database = os.getenv("METADATA_DB", "fda")
        username = os.getenv("METADATA_USERNAME", "root")
        password = os.getenv("METADATA_PASSWORD", "dbrepo")
        conn = mariadb.connect(database=database, user=username, host="metadata-db", password=password)
        cursor = conn.cursor()

        # Insert tblnames into table mdb_TABLES
        cursor.execute(
            "INSERT IGNORE INTO mdb_concepts (URI, name, created) VALUES (%s, %s, current_timestamp)",
            (uri, c_name,))
        logging.info("Created concept in metadata database")
        conn.commit()
        conn.close()
        return 1
    except Exception as e:
        logging.error("Error while connecting to metadata database", e)


def insert_mdb_columns_concepts(cdbid, tid, cid, uri) -> int:
    try:
        # Connecting to metadatabase
        database = os.getenv("METADATA_DB", "fda")
        username = os.getenv("METADATA_USERNAME", "root")
        password = os.getenv("METADATA_PASSWORD", "dbrepo")
        conn = mariadb.connect(database=database, user=username, host="metadata-db", password=password)
        cursor = conn.cursor()

        # Insert tblnames into table mdb_TABLES
        cursor.execute("INSERT IGNORE INTO mdb_columns_concepts (cDBID, tID, cID, uri) VALUES (%s, %s, %s, %s)",
                       (cdbid, tid, cid, uri,))
        conn.commit()
        conn.close()
        return 1
    except Exception as e:
        logging.error("Error while connecting to metadata database", e)


def delete_mdb_columns_concepts(cdbid, tid, cid) -> int:
    try:
        # Connecting to metadatabase
        database = os.getenv("METADATA_DB", "fda")
        username = os.getenv("METADATA_USERNAME", "root")
        password = os.getenv("METADATA_PASSWORD", "dbrepo")
        conn = mariadb.connect(database=database, user=username, host="metadata-db", password=password)
        cursor = conn.cursor()

        logging.info("Deleting column concept assignment cDBID=%s AND tID=%s AND cID=%s", cdbid, tid, cid)

        # Insert tblnames into table mdb_TABLES
        cursor.execute("DELETE FROM mdb_columns_concepts WHERE cDBID=%s AND tID=%s AND cID=%s",
                       (cdbid, tid, cid,))
        conn.commit()
        conn.close()
        return 1
    except Exception as e:
        logging.error("Error while connecting to metadata database", e)
