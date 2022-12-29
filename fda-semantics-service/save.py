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
            "INSERT IGNORE INTO mdb_concepts (uri, name, created) VALUES (%s, %s, current_timestamp)",
            (uri, c_name,))
        logging.info("Created concept in metadata database")
        conn.commit()
        conn.close()
        return 1
    except Exception as e:
        logging.error("Error while connecting to metadata database", e)

def insert_mdb_units(uri, c_name) -> int:
    try:
        # Connecting to metadatabase
        database = os.getenv("METADATA_DB", "fda")
        username = os.getenv("METADATA_USERNAME", "root")
        password = os.getenv("METADATA_PASSWORD", "dbrepo")
        conn = mariadb.connect(database=database, user=username, host="metadata-db", password=password)
        cursor = conn.cursor()

        # Insert tblnames into table mdb_TABLES
        cursor.execute(
            "INSERT IGNORE INTO mdb_units (uri, name, created) VALUES (%s, %s, current_timestamp)",
            (uri, c_name,))
        logging.info("Created unit in metadata database")
        conn.commit()
        conn.close()
        return 1
    except Exception as e:
        logging.error("Error while connecting to metadata database", e)
