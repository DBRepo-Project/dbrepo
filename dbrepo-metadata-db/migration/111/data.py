#!/usr/bin/env python3
import os
from typing import List

import pandas
from dbrepo.RestClient import RestClient

endpoint = os.getenv('METADATA_SERVICE_ENDPOINT', 'http://localhost')
username = os.getenv('SYSTEM_USERNAME', 'admin')
password = os.getenv('SYSTEM_PASSWORD', 'admin')
client = RestClient(endpoint=endpoint, username=username, password=password)

plan: List[str] = []

if __name__ == '__main__':
    plan.append("SET FOREIGN_KEY_CHECKS=0;")
    plan.append("BEGIN;")
    for index, row in pandas.read_csv('./mdb_users.csv').iterrows():
        user_id = row['id']
        username = row['username']
        plan.append(f"UPDATE `mdb_databases` SET `owned_by` = '{username}' WHERE `owned_by` = '{user_id}';")
        plan.append(f"UPDATE `mdb_databases` SET `contact_person` = '{username}' WHERE `contact_person` = '{user_id}';")
        plan.append(f"UPDATE `mdb_have_access` SET `username` = '{username}' WHERE `username` = '{user_id}';")
        plan.append(f"UPDATE `mdb_identifiers` SET `owned_by` = '{username}' WHERE `owned_by` = '{user_id}';")
    plan.append("COMMIT;")
    plan.append("SET FOREIGN_KEY_CHECKS=1;")
    print("\n".join(plan))
