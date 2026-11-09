#!/usr/bin/env python3
"""Write reviewed Query Store procedure replacements for explicitly named research databases."""
import argparse
import os
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
ROUTINES = ("store_query", "_store_query", "hash_query")
RESOURCES = ROOT / "dbrepo-data-service/services/src/main/resources/query-store"


def migration(databases):
    result = ["-- Pause Query Store calls and back up existing routine definitions before applying this file.",
              "DELIMITER $dbrepo$",
              "SET @_dbrepo_mode = @@SESSION.sql_mode$dbrepo$",
              "SET @_dbrepo_client = @@SESSION.character_set_client$dbrepo$",
              "SET @_dbrepo_collation = @@SESSION.collation_connection$dbrepo$"]
    for database in databases:
        if not re.fullmatch(r"[A-Za-z0-9_]+", database):
            raise ValueError("Use the internal research database name")
        result.append(f"USE `{database}`$dbrepo$")
        for routine in ROUTINES:
            sql = (RESOURCES / (routine + ".sql")).read_text().strip().rstrip(";")
            body = re.sub(r"^CREATE(?: DEFINER\s*=\s*'root')? PROCEDURE\s+", "", sql)
            assert body != sql
            signature, implementation = body.split(" BEGIN", 1)
            fallback = "'root@%'" if "DEFINER" in sql[:sql.index("PROCEDURE")] else "CURRENT_USER()"
            where = f"FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA=DATABASE() AND ROUTINE_NAME='{routine}' AND ROUTINE_TYPE='PROCEDURE'"
            result.extend([
                f"SET @_dbrepo_definer = COALESCE((SELECT DEFINER {where}), {fallback})$dbrepo$",
                f"SET SESSION sql_mode = COALESCE((SELECT SQL_MODE {where}), @_dbrepo_mode)$dbrepo$",
                f"SET SESSION character_set_client = COALESCE((SELECT CHARACTER_SET_CLIENT {where}), @_dbrepo_client)$dbrepo$",
                f"SET SESSION collation_connection = COALESCE((SELECT COLLATION_CONNECTION {where}), @_dbrepo_collation)$dbrepo$",
                f"SET @_dbrepo_security = COALESCE((SELECT SECURITY_TYPE {where}), 'DEFINER')$dbrepo$",
                "SET @_dbrepo_host = SUBSTRING_INDEX(@_dbrepo_definer, '@', -1)$dbrepo$",
                "SET @_dbrepo_user = LEFT(@_dbrepo_definer, CHAR_LENGTH(@_dbrepo_definer)-CHAR_LENGTH(@_dbrepo_host)-1)$dbrepo$",
                "SET @_dbrepo_ddl = CONCAT('CREATE OR REPLACE DEFINER=`', REPLACE(@_dbrepo_user, '`', '``'), '`@`', REPLACE(@_dbrepo_host, '`', '``'), '` PROCEDURE ', "
                    + "CONVERT(0x" + signature.encode().hex() + " USING utf8mb4), ' SQL SECURITY ', @_dbrepo_security, ' BEGIN', CONVERT(0x" + implementation.encode().hex() + " USING utf8mb4))$dbrepo$",
                "PREPARE dbrepo_routine FROM @_dbrepo_ddl$dbrepo$",
                "EXECUTE dbrepo_routine$dbrepo$",
                "DEALLOCATE PREPARE dbrepo_routine$dbrepo$",
            ])
    result.extend(["SET SESSION sql_mode = @_dbrepo_mode$dbrepo$",
                   "SET SESSION character_set_client = @_dbrepo_client$dbrepo$",
                   "SET SESSION collation_connection = @_dbrepo_collation$dbrepo$", "DELIMITER ;"])
    return "\n".join(result) + "\n"


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--database", action="append", required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    sql = migration(args.database)
    with os.fdopen(os.open(args.output, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600), "w") as stream:
        stream.write(sql)
    print("Migration written. No database connection or SQL execution was performed.")
