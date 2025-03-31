from __future__ import annotations

from typing import Optional, List

from pydantic import BaseModel


class Datasource(BaseModel):
    uid: str
    type: str
    id: Optional[int] = None
    orgId: Optional[int] = None
    name: Optional[str] = None
    typeLogoUrl: Optional[str] = None
    access: Optional[str] = None
    url: Optional[str] = None
    user: Optional[str] = None
    basicAuth: Optional[bool] = None
    withCredentials: Optional[bool] = None
    isDefault: Optional[bool] = None
    version: Optional[int] = None
    readOnly: Optional[bool] = None
    jsonData: Optional[dict] = None
    secureJsonFields: Optional[dict] = None
    basicAuthUser: Optional[str] = None
    basicAuthPassword: Optional[str] = None
    password: Optional[str] = None


class CreateDatasource(BaseModel):
    name: str
    type: str
    access: str
    url: str
    basicAuth: bool
    version: int
    readOnly: bool
    jsonData: Optional[dict] = None
    secureJsonData: Optional[dict] = None
    basicAuthUser: Optional[str] = None


class Permission(BaseModel):
    permission: str


class CreateDatasourceRequest(BaseModel):
    database_internal_name: str
    readonly: bool
    type: str


class User(BaseModel):
    id: str
    username: str
    roles: List[str]
