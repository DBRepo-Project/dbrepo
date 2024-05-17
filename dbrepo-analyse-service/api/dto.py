from typing import Optional

from pydantic import BaseModel


class ColumnStat(BaseModel):
    val_min: Optional[float]
    val_max: Optional[float]
    mean: Optional[float]
    median: Optional[float]
    std_dev: Optional[float]


class TableStat(BaseModel):
    columns: dict[str, ColumnStat]
