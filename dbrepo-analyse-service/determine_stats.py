from dataclasses import dataclass, field
from dataclasses_json import dataclass_json

from pandas import DataFrame
from sqlalchemy import create_engine, text


@dataclass_json
@dataclass(init=True, eq=True)
class TableStats:
    columns: dict[str, {"val_min": float, "val_max": float, "mean": float, "median": float,
                        "std_dev": float}] = field(default_factory=dict)


def determine_stats(db, os, **kwargs) -> TableStats:
    database_id = kwargs.get("database_id")
    table_id = kwargs.get("table_id")

    try:
        with db.engine.connect() as connection:
            database_name = connection.execute(
                text(f"SELECT internal_name FROM mdb_databases WHERE id={database_id}")
            ).fetchone()[0]
            table_name = connection.execute(
                text(f"SELECT internal_name FROM mdb_tables WHERE id={table_id}")
            ).fetchone()[0]
    except Exception:
        raise OSError(f"Failed to get database name and table name")

    if not database_name or not table_name:
        raise OSError(f"Failed to get database name and table name")

    data_db_host = kwargs.get("data_db_host", "data-db")
    data_db_port = kwargs.get("data_db_port", 3306)
    # Generate data db connection on the fly: database name is varying according to the id given
    data_db_uri = (
        f"mysql+pymysql://root:dbrepo@{data_db_host}:{data_db_port}/{database_name}"
    )
    data_db_engine = create_engine(data_db_uri)

    with data_db_engine.connect() as connection:
        result = connection.execute(text(f"SELECT * FROM {table_name}"))
        rows = result.fetchall()

    df = DataFrame(rows, columns=result.keys())
    stats = TableStats()
    for column, dtype in df.dtypes.items():
        # Check if the column has a numeric data type
        if dtype.kind in "fi":
            # Calculate the statistics for the current column
            column_stats = {
                "val_min": df[column].min(),
                "val_max": df[column].max(),
                "mean": df[column].mean(),
                "median": df[column].median(),
                "std_dev": df[column].std(),
            }
            stats.columns[column] = {"val_min": float(df[column].min()), "val_max": float(df[column].max()),
                                     "mean": float(df[column].mean()), "median": float(df[column].median()),
                                     "std_dev": float(df[column].std())}

            # Store statistical properties to the metadata db and index to OS
            # TODO: use prepared statements to eliminate SQL injection
            update_query = text(
                f"""
                UPDATE mdb_columns
                SET
                    val_min = '{column_stats["val_min"]}',
                    val_max = '{column_stats["val_max"]}',
                    mean    = '{column_stats["mean"]}',
                    median  = '{column_stats["median"]}',
                    std_dev = '{column_stats["std_dev"]}'
                WHERE
                    tID = '{table_id}' AND internal_name = '{column}'
            """
            )
            # We need an extra select query to fetch the column ID for OpenSearch
            select_query = text(
                f"""
                SELECT id
                FROM mdb_columns
                WHERE tID = '{table_id}' AND internal_name = '{column}'
            """
            )
            with db.engine.begin() as connection:
                connection.execute(update_query)
                result = connection.execute(select_query)
                column_id = result.fetchone()[0]
                connection.commit()

            # Fetch the existing document
            existing_document = os.get(index="database", id=database_id)["_source"]

            # Loop over OS response and append the statistics for each column
            for tidx, table in enumerate(existing_document["tables"]):
                if table["id"] == table_id:
                    for cidx, column in enumerate(table["columns"]):
                        if column["id"] == column_id:
                            existing_document["tables"][tidx]["columns"][cidx].update(
                                column_stats
                            )
                            # No need to keep searching if column id matches
                            break

            # Index and force refresh
            os.index(
                index="database",
                id=database_id,
                body=existing_document,
                refresh=True,
            )

    return stats
