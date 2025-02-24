#!/usr/bin/env python3
import os
import uuid

from dbrepo.RestClient import RestClient

endpoint = os.getenv('METADATA_SERVICE_ENDPOINT', 'https://dbrepo1.ec.tuwien.ac.at')
username = os.getenv('SYSTEM_USERNAME', 'admin')
password = os.getenv('SYSTEM_PASSWORD', 'f24870437f82adf567c0b03179f15e21')
client = RestClient(endpoint=endpoint, username=username, password=password)

plan: [str] = []


def update_concepts() -> None:
    plan.append("-- concepts")
    plan.append("BEGIN;")
    for concept in client.get_concepts():
        old_id: int = concept.id
        new_id: uuid = uuid.uuid4()
        plan.append(f"UPDATE mdb_columns_concepts SET id = '{new_id}' WHERE id = '{old_id}';")
        plan.append(f"UPDATE mdb_concepts SET id = '{new_id}' WHERE id = '{old_id}';")
    plan.append("COMMIT;")


def update_ontologies() -> None:
    plan.append("-- ontologies")
    plan.append("BEGIN;")
    for ontology in client.get_ontologies():
        old_id = ontology.id
        new_id: uuid = uuid.uuid4()
        plan.append(f"UPDATE mdb_ontologies SET id = '{new_id}' WHERE id = '{old_id}';")
    plan.append("COMMIT;")


def update_units() -> None:
    plan.append("-- units")
    plan.append("BEGIN;")
    for unit in client.get_units():
        old_id: int = unit.id
        new_id: uuid = uuid.uuid4()
        plan.append(f"UPDATE mdb_columns_units SET id = '{new_id}' WHERE id = '{old_id}';")
        plan.append(f"UPDATE mdb_units SET id = '{new_id}' WHERE id = '{old_id}';")
    plan.append("COMMIT;")


def update_images() -> None:
    plan.append("-- images")
    plan.append("BEGIN;")
    for _image in client.get_images():
        old_id: int = _image.id
        image = client.get_image(old_id)
        new_id: uuid = uuid.uuid4()
        plan.append(f"UPDATE mdb_images SET id = '{new_id}' WHERE id = '{old_id}';")
        plan.append(f"UPDATE mdb_image_operators SET image_id = '{new_id}' WHERE image_id = '{old_id}';")
        plan.append(f"UPDATE mdb_image_types SET image_id = '{new_id}' WHERE image_id = '{old_id}';")
        plan.append(f"UPDATE mdb_containers SET image_id = '{new_id}' WHERE image_id = '{old_id}';")
        for operator in image.operators:
            o_old_id: int = operator.id
            o_new_id: uuid = uuid.uuid4()
            plan.append(f"UPDATE mdb_image_operators SET id = '{o_new_id}' WHERE id = '{o_old_id}';")
        for data_type in image.data_types:
            d_old_id: int = data_type.id
            d_new_id: uuid = uuid.uuid4()
            plan.append(f"UPDATE mdb_image_types SET id = '{d_new_id}' WHERE id = '{d_old_id}';")
    plan.append("COMMIT;")


def update_containers() -> None:
    plan.append("-- containers")
    plan.append("BEGIN;")
    for containers in client.get_containers():
        old_id: int = containers.id
        new_id: uuid = uuid.uuid4()
        plan.append(f"UPDATE mdb_containers SET id = '{new_id}' WHERE id = '{old_id}';")
        plan.append(f"UPDATE mdb_databases SET cid = '{new_id}' WHERE cid = '{old_id}';")
    plan.append("COMMIT;")


def update_databases() -> None:
    plan.append("-- databases")
    plan.append("BEGIN;")
    for _database in client.get_databases():
        database = client.get_database(database_id=_database.id)
        old_id: int = database.id
        new_id: uuid = uuid.uuid4()
        plan.append(f"UPDATE mdb_tables SET tDBID = '{new_id}' WHERE tDBID = '{old_id}';")
        plan.append(f"UPDATE mdb_have_access SET database_id = '{new_id}' WHERE database_id = '{old_id}';")
        plan.append(f"UPDATE mdb_view SET vdbid = '{new_id}' WHERE vdbid = '{old_id}';")
        plan.append(f"UPDATE mdb_identifiers SET dbid = '{new_id}' WHERE dbid = '{old_id}';")
        plan.append(f"UPDATE mdb_access SET aDBID = '{new_id}' WHERE aDBID = '{old_id}';")
        for view in database.views:
            v_old_id: int = view.id
            v_new_id: uuid = uuid.uuid4()
            plan.append(f"UPDATE mdb_identifiers SET vid = '{v_new_id}' WHERE vid = '{v_old_id}';")
            plan.append(f"UPDATE mdb_view_columns SET id = UUID(), view_id = '{v_new_id}' WHERE id = '{v_old_id}';")
        for table in database.tables:
            tbl_old_id: int = table.id
            tbl_new_id: uuid = uuid.uuid4()
            plan.append(f"UPDATE mdb_identifiers SET tid = '{tbl_new_id}' WHERE tid = '{tbl_old_id}';")
            plan.append(f"UPDATE mdb_columns SET tID = '{tbl_new_id}' WHERE tID = '{tbl_old_id}';")
            plan.append(f"UPDATE mdb_constraints_primary_key SET pkid = UUID(), tID = '{tbl_new_id}' WHERE tID = '{tbl_old_id}';")
            plan.append(f"UPDATE mdb_constraints_unique SET tid = '{tbl_new_id}' WHERE tid = '{tbl_old_id}';")
            plan.append(
                f"UPDATE mdb_constraints_checks SET id = UUID(), tid = '{tbl_new_id}' WHERE tid = '{tbl_old_id}';")
            for fk in table.constraints.foreign_keys:
                fk_old_id: int = fk.id
                fk_new_id: uuid = uuid.uuid4()
                plan.append(
                    f"UPDATE mdb_constraints_foreign_key SET fkid = '{fk_new_id}', tid = '{tbl_new_id}' WHERE fkid = '{fk_old_id}';")
                for fkref in fk.references:
                    fkref_old_id = fkref.id
                    plan.append(
                        f"UPDATE mdb_constraints_foreign_key_reference SET id = UUID(), fkid = '{fk_new_id}' WHERE fkid = '{fkref_old_id}';")
            for pk in table.constraints.primary_key:
                pk_old_id: int = pk.id
                plan.append(
                    f"UPDATE mdb_constraints_primary_key SET pkid = UUID(), tID = '{tbl_new_id}' WHERE tID = {pk_old_id};")
            for uk in table.constraints.uniques:
                uk_old_id: int = uk.id
                uk_new_id: uuid = uuid.uuid4()
                plan.append(
                    f"UPDATE mdb_constraints_unique SET uid = '{uk_new_id}', tid = '{tbl_new_id}' WHERE uid = '{uk_old_id}';")
                plan.append(
                    f"UPDATE mdb_constraints_unique_columns SET id = UUID(), uid = '{uk_new_id}' WHERE uid = '{uk_old_id}';")
            for column in table.columns:
                col_old_id: int = column.id
                col_new_id: uuid = uuid.uuid4()
                plan.append(f"UPDATE mdb_columns SET ID = '{col_new_id}' WHERE ID = '{col_old_id}';")
                plan.append(f"UPDATE mdb_constraints_unique_columns SET cid = '{col_new_id}' WHERE cid = '{col_old_id}';")
                plan.append(f"UPDATE mdb_constraints_primary_key SET cid = '{col_new_id}' WHERE cid = '{col_old_id}';")
                plan.append(
                    f"UPDATE mdb_constraints_foreign_key_reference SET cid = '{col_new_id}' WHERE cid = '{col_old_id}';")
                plan.append(
                    f"UPDATE mdb_constraints_foreign_key_reference SET rcid = '{col_new_id}' WHERE rcid = '{col_old_id}';")
                plan.append(f"UPDATE mdb_columns_concepts SET cID = '{col_new_id}' WHERE cID = '{col_old_id}';")
                plan.append(f"UPDATE mdb_columns_units SET cID = '{col_new_id}' WHERE cID = '{col_old_id}';")
                plan.append(f"UPDATE mdb_columns_sets SET column_id = '{col_new_id}' WHERE column_id = '{col_old_id}';")
                plan.append(f"UPDATE mdb_columns_enums SET column_id = '{col_new_id}' WHERE column_id = '{col_old_id}';")
                for set in column.sets:
                    s_old_id: int = set.id
                    s_new_id: uuid = uuid.uuid4()
                    plan.append(f"UPDATE mdb_columns_sets SET id = '{s_new_id}' WHERE id = '{s_old_id}';")
                for enum in column.enums:
                    e_old_id: int = enum.id
                    e_new_id: uuid = uuid.uuid4()
                    plan.append(f"UPDATE mdb_columns_enums SET id = '{e_new_id}' WHERE id = '{e_old_id}';")
            plan.append(f"UPDATE mdb_tables SET ID = '{tbl_new_id}' WHERE ID = '{tbl_old_id}';")
        plan.append(f"UPDATE mdb_databases SET id = '{new_id}' WHERE id = '{old_id}';")
    plan.append("COMMIT;")


def update_messages() -> None:
    plan.append("-- messages")
    plan.append("BEGIN;")
    for message in client.get_messages():
        old_id = message.id
        new_id: uuid = uuid.uuid4()
        plan.append(f"UPDATE mdb_messages SET id = '{new_id}' WHERE id = '{old_id}';")
    plan.append("COMMIT;")


def update_identifiers() -> None:
    plan.append("-- identifiers")
    plan.append("BEGIN;")
    for _identifier in client.get_identifiers():
        identifier = client.get_identifier(identifier_id=_identifier.id)
        i_old_id: int = identifier.id
        i_new_id: uuid = uuid.uuid4()
        plan.append(f"UPDATE mdb_identifiers SET ID = '{i_new_id}' WHERE id = '{i_old_id}';")
        plan.append(f"UPDATE mdb_identifier_titles SET pid = '{i_new_id}' WHERE pid = '{i_old_id}';")
        plan.append(f"UPDATE mdb_identifier_descriptions SET pid = '{i_new_id}' WHERE pid = '{i_old_id}';")
        plan.append(f"UPDATE mdb_identifier_creators SET pid = '{i_new_id}' WHERE pid = '{i_old_id}';")
        plan.append(f"UPDATE mdb_identifier_funders SET pid = '{i_new_id}' WHERE pid = '{i_old_id}';")
        plan.append(f"UPDATE mdb_identifier_licenses SET pid = '{i_new_id}' WHERE pid = '{i_old_id}';")
        for title in identifier.titles:
            t_old_id = title.id
            t_new_id: uuid = uuid.uuid4()
            plan.append(f"UPDATE mdb_identifier_titles SET id = '{t_new_id}' WHERE id = '{t_old_id}';")
        for description in identifier.descriptions:
            d_old_id = description.id
            d_new_id: uuid = uuid.uuid4()
            plan.append(f"UPDATE mdb_identifier_descriptions SET id = '{d_new_id}' WHERE id = '{d_old_id}';")
        for creator in identifier.creators:
            c_old_id = creator.id
            c_new_id: uuid = uuid.uuid4()
            plan.append(f"UPDATE mdb_identifier_creators SET id = '{c_new_id}' WHERE id = '{c_old_id}';")
        for funder in identifier.funders:
            f_old_id = funder.id
            f_new_id: uuid = uuid.uuid4()
            plan.append(f"UPDATE mdb_identifier_funders SET id = '{f_new_id}' WHERE id = '{f_old_id}';")
    plan.append("COMMIT;")


if __name__ == '__main__':
    plan.append("SET FOREIGN_KEY_CHECKS=0;")
    plan.append("BEGIN;")
    plan.append(f"INSERT INTO mdb_have_access SELECT uu.id as user_id, d.id as database_id, 'WRITE_ALL' as access_type, NOW() as created FROM mdb_databases d, mdb_users uu WHERE NOT EXISTS(SELECT 1 FROM mdb_have_access a JOIN mdb_users u ON a.user_id = u.id AND u.is_internal = TRUE) AND uu.is_internal = TRUE;")
    plan.append("COMMIT;")
    update_concepts()
    update_units()
    update_messages()
    update_ontologies()
    update_images()
    update_containers()
    update_databases()
    update_identifiers()
    update_messages()
    plan.append("SET FOREIGN_KEY_CHECKS=1;")
    print("\n".join(plan))
