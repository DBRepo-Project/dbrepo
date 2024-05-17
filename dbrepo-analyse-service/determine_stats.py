import logging

from flask import current_app

from pandas import DataFrame, isna
from dbrepo.RestClient import RestClient

from api.dto import TableStat, ColumnStat


def determine_stats(database_id: int, table_id: int) -> TableStat:
    client = RestClient(endpoint=current_app.config['GATEWAY_SERVICE_ENDPOINT'],
                        username=current_app.config['ADMIN_USERNAME'], password=current_app.config['ADMIN_PASSWORD'])
    df: DataFrame = client.get_table_data(database_id=database_id, table_id=table_id, page=0, size=1000, df=True)
    stats = TableStat(columns=dict())
    for name, dtype in df.dtypes.items():
        # Check if the column has a numeric data type
        if dtype.kind in "fi":
            val_min = None if isna(df[name].min()) else df[name].min()
            val_max = None if isna(df[name].max()) else df[name].max()
            mean = None if isna(df[name].mean()) else df[name].mean()
            median = None if isna(df[name].median()) else df[name].median()
            std_dev = None if isna(df[name].std()) else df[name].std()
            stats.columns[str(name)] = ColumnStat(val_min=val_min, val_max=val_max, mean=mean, median=median,
                                                  std_dev=std_dev)
            logging.debug(f"statistical props of the first 1000 rows: <min={val_min}, max={val_max}, mean={mean}, "
                          f"median={median}, std_dev={std_dev}>")
    return stats
