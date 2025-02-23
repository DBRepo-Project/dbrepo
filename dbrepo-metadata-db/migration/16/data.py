#!/usr/bin/env python3
import os
import uuid

from dbrepo.RestClient import RestClient

endpoint = os.getenv('METADATA_SERVICE_ENDPOINT', 'http://localhost')
username = os.getenv('SYSTEM_USERNAME', 'admin')
password = os.getenv('SYSTEM_PASSWORD', 'admin')
client = RestClient(endpoint=endpoint, username=username, password=password)

plan: [str] = []


def update_concepts() -> None:
    plan.append("-- concepts")
    plan.append("BEGIN;")
    for concept in client.get_concepts():
        old_id: int = concept.id
        new_id: uuid = uuid.uuid4()
        plan.append(f"UPDATE mdb_columns_concepts SET id = '{new_id}' WHERE id = {old_id};")
        plan.append(f"UPDATE mdb_concepts SET id = '{new_id}' WHERE id = {old_id};")
    plan.append("COMMIT;")


def update_ontologies() -> None:
    plan.append("-- ontologies")
    plan.append("BEGIN;")
    plan.append(f"UPDATE mdb_ontologies SET id = UUID();")
    plan.append("COMMIT;")


def update_units() -> None:
    plan.append("-- units")
    plan.append("BEGIN;")
    for unit in client.get_units():
        old_id: int = unit.id
        new_id: uuid = uuid.uuid4()
        plan.append(f"UPDATE mdb_columns_units SET id = '{new_id}' WHERE id = {old_id};")
        plan.append(f"UPDATE mdb_units SET id = '{new_id}' WHERE id = {old_id};")
    plan.append("COMMIT;")


def update_images() -> None:
    plan.append("-- images")
    plan.append("BEGIN;")
    for image in client.get_images():
        old_id: int = image.id
        new_id: uuid = uuid.uuid4()
        plan.append(f"UPDATE mdb_images SET id = '{new_id}' WHERE id = {old_id};")
        plan.append(f"UPDATE mdb_image_operators SET id = UUID(), image_id = '{new_id}' WHERE image_id = {old_id};")
        plan.append(f"UPDATE mdb_image_types SET id = UUID(), image_id = '{new_id}' WHERE image_id = {old_id};")
        plan.append(f"UPDATE mdb_containers SET id = UUID(), image_id = '{new_id}' WHERE image_id = {old_id};")
    plan.append("COMMIT;")


def update_containers() -> None:
    plan.append("-- containers")
    plan.append("BEGIN;")
    for containers in client.get_containers():
        old_id: int = containers.id
        new_id: uuid = uuid.uuid4()
        plan.append(f"UPDATE mdb_containers SET id = '{new_id}' WHERE id = {old_id};")
        plan.append(f"UPDATE mdb_databases SET cid = '{new_id}' WHERE cid = {old_id};")
    plan.append("COMMIT;")


def update_databases() -> None:
    plan.append("-- databases")
    plan.append("BEGIN;")
    for _database in client.get_databases():
        database = client.get_database(database_id=_database.id)
        old_id: int = database.id
        new_id: uuid = uuid.uuid4()
        plan.append(f"UPDATE mdb_tables SET tDBID = '{new_id}' WHERE tDBID = {old_id};")
        plan.append(f"UPDATE mdb_have_access SET database_id = '{new_id}' WHERE database_id = {old_id};")
        plan.append(f"UPDATE mdb_view SET vdbid = '{new_id}' WHERE vdbid = {old_id};")
        plan.append(f"UPDATE mdb_identifiers SET dbid = '{new_id}' WHERE dbid = {old_id};")
        plan.append(f"UPDATE mdb_access SET aDBID = '{new_id}' WHERE aDBID = {old_id};")
        for view in database.views:
            v_old_id: int = view.id
            v_new_id: uuid = uuid.uuid4()
            plan.append(f"UPDATE mdb_identifiers SET vid = '{v_new_id}' WHERE vid = {v_old_id};")
            plan.append(f"UPDATE mdb_view_columns SET id = UUID(), view_id = '{v_new_id}' WHERE id = {v_old_id};")
        for table in database.tables:
            tbl_old_id: int = table.id
            tbl_new_id: uuid = uuid.uuid4()
            plan.append(f"UPDATE mdb_identifiers SET tid = '{tbl_new_id}' WHERE tid = {tbl_old_id};")
            plan.append(f"UPDATE mdb_constraints_checks SET id = UUID(), tid = '{tbl_new_id}' WHERE tid = {tbl_old_id};")
            for fk in table.constraints.foreign_keys:
                fk_old_id: int = fk.id
                fk_new_id: uuid = uuid.uuid4()
                plan.append(f"UPDATE mdb_constraints_foreign_key SET id = '{fk_new_id}', tid = '{tbl_new_id}' WHERE id = {fk_old_id};")
                for fkref in fk.references:
                    plan.append(f"UPDATE mdb_constraints_foreign_key_reference SET id = UUID(), fkid = '{fk_new_id}' WHERE fkid = {fkref};")
            for pk in table.constraints.primary_key:
                pk_old_id: int = pk.id
                plan.append(f"UPDATE mdb_constraints_primary_key SET pkid = UUID(), tID = '{tbl_new_id}' WHERE tID = {pk_old_id};")
            for uk in table.constraints.uniques:
                uk_old_id: int = uk.id
                uk_new_id: uuid = uuid.uuid4()
                plan.append(f"UPDATE mdb_constraints_unique SET uid = '{uk_new_id}', tid = '{tbl_new_id}' WHERE uid = {uk_old_id};")
                plan.append(f"UPDATE mdb_constraints_unique_columns SET id = UUID(), uid = '{uk_new_id}' WHERE uid = {uk_old_id};")
            for column in table.columns:
                col_old_id: int = column.id
                col_new_id: uuid = uuid.uuid4()
                plan.append(f"UPDATE mdb_columns SET ID = '{col_new_id}' WHERE ID = {col_old_id};")
                plan.append(f"UPDATE mdb_constraints_unique_columns SET cid = '{col_new_id}' WHERE cid = {col_old_id};")
                plan.append(f"UPDATE mdb_constraints_primary_key SET cid = '{col_new_id}' WHERE cid = {col_old_id};")
                plan.append(f"UPDATE mdb_constraints_foreign_key_reference SET cid = '{col_new_id}' WHERE cid = {col_old_id};")
                plan.append(f"UPDATE mdb_constraints_foreign_key_reference SET rcid = '{col_new_id}' WHERE rcid = {col_old_id};")
                plan.append(f"UPDATE mdb_columns_concepts SET cID = '{col_new_id}' WHERE cID = {col_old_id};")
                plan.append(f"UPDATE mdb_columns_units SET cID = '{col_new_id}' WHERE cID = {col_old_id};")
                plan.append(f"UPDATE mdb_columns_sets SET column_id = '{col_new_id}' WHERE column_id = {col_old_id};")
                plan.append(f"UPDATE mdb_columns_enums SET column_id = '{col_new_id}' WHERE column_id = {col_old_id};")
            plan.append(f"UPDATE mdb_tables SET ID = '{tbl_new_id}' WHERE ID = {tbl_old_id};")
        plan.append(f"UPDATE mdb_databases SET id = '{new_id}' WHERE id = {old_id};")
    plan.append("COMMIT;")

def update_messages() -> None:
    plan.append("-- messages")
    plan.append("BEGIN;")
    plan.append(f"UPDATE mdb_messages SET ID = UUID();")
    plan.append("COMMIT;")

def update_identifiers() -> None:
    plan.append("-- identifiers")
    plan.append("BEGIN;")
    for identified in client.get_identifiers():
        i_old_id: int = identified.id
        i_new_id: uuid = uuid.uuid4()
        plan.append(f"UPDATE mdb_identifiers SET ID = '{i_new_id}' WHERE id = {i_old_id};")
        plan.append(f"UPDATE mdb_identifier_creators SET id = UUID(), pid = '{i_new_id}' WHERE pid = {i_old_id};")
        plan.append(f"UPDATE mdb_identifier_descriptions SET id = UUID(), pid = '{i_new_id}' WHERE pid = {i_old_id};")
        plan.append(f"UPDATE mdb_identifier_titles SET id = UUID(), pid = '{i_new_id}' WHERE pid = {i_old_id};")
        plan.append(f"UPDATE mdb_identifier_funders SET id = UUID(), pid = '{i_new_id}' WHERE pid = {i_old_id};")
        plan.append(f"UPDATE mdb_identifier_licenses SET pid = '{i_new_id}' WHERE pid = {i_old_id};")
    plan.append("COMMIT;")


if __name__ == '__main__':
    plan.append("SET FOREIGN_KEY_CHECKS=0;")
    update_concepts()
    update_units()
    update_messages()
    update_ontologies()
    update_images()
    update_containers()
    update_databases()
    update_identifiers()
    plan.append("SET FOREIGN_KEY_CHECKS=1;")
    print("\n".join(plan))
