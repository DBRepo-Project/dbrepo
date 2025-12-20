---
author: Martin Weise
---

The REST API manages all of DBRepo. This documentation is also available as [Swagger UI](/infrastructures/dbrepo/1.13/rest/).

# REST API

> Version 1.13.1

The merged REST API of DBRepo for users, developers and data stewards to be accessed publicly. Have a look at
the [source code](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services) for non-public endpoints
that are used between the services themselves.


## Path Table

| Method | Path | Description |
| --- | --- | --- |
| GET | [/api/v1/database/{databaseId}/view/{viewId}/data](#getapiv1databasedatabaseidviewviewiddata) | Get view data |
| HEAD | [/api/v1/database/{databaseId}/view/{viewId}/data](#headapiv1databasedatabaseidviewviewiddata) | Get view data |
| GET | [/api/v1/database/{databaseId}/table/{tableId}/data](#getapiv1databasedatabaseidtabletableiddata) | Get table data |
| PUT | [/api/v1/database/{databaseId}/table/{tableId}/data](#putapiv1databasedatabaseidtabletableiddata) | Update tuple |
| POST | [/api/v1/database/{databaseId}/table/{tableId}/data](#postapiv1databasedatabaseidtabletableiddata) | Insert tuple |
| DELETE | [/api/v1/database/{databaseId}/table/{tableId}/data](#deleteapiv1databasedatabaseidtabletableiddata) | Delete tuple |
| HEAD | [/api/v1/database/{databaseId}/table/{tableId}/data](#headapiv1databasedatabaseidtabletableiddata) | Get table data |
| GET | [/api/v1/database/{databaseId}/subset/{subsetId}/data](#getapiv1databasedatabaseidsubsetsubsetiddata) | Get subset data |
| HEAD | [/api/v1/database/{databaseId}/subset/{subsetId}/data](#headapiv1databasedatabaseidsubsetsubsetiddata) | Get subset data |
| GET | [/api/v1/database/{databaseId}/grant/{username}](#getapiv1databasedatabaseidgrantusername) | Get grants |
| HEAD | [/api/v1/database/{databaseId}/grant/{username}](#headapiv1databasedatabaseidgrantusername) | Get grants |
| PUT | [/api/v1/database/{databaseId}/subset/{queryId}](#putapiv1databasedatabaseidsubsetqueryid) | Persist subset |
| POST | [/api/v1/upload](#postapiv1upload) | Uploads a multipart file |
| POST | [/api/v1/database/{databaseId}/table/{tableId}/data/import](#postapiv1databasedatabaseidtabletableiddataimport) | Import dataset |
| GET | [/api/v1/database/{databaseId}/subset](#getapiv1databasedatabaseidsubset) | Find subsets |
| POST | [/api/v1/database/{databaseId}/subset](#postapiv1databasedatabaseidsubset) | Create subset |
| GET | [/api/v1/image/{imageId}/analyse/schema/{key}](#getapiv1imageimageidanalyseschemakey) | Analyse schema |
| GET | [/api/v1/database/{databaseId}/table/{tableId}/history](#getapiv1databasedatabaseidtabletableidhistory) | Get history |
| GET | [/api/v1/database/{databaseId}/subset/{subsetId}](#getapiv1databasedatabaseidsubsetsubsetid) | Find subset |
| GET | [/api/v1/user/{username}](#getapiv1userusername) | Get user |
| PUT | [/api/v1/user/{username}](#putapiv1userusername) | Update user |
| HEAD | [/api/v1/user/{username}](#headapiv1userusername) | Get user |
| GET | [/api/v1/database](#getapiv1database) | List databases |
| POST | [/api/v1/database](#postapiv1database) | Create database |
| HEAD | [/api/v1/database](#headapiv1database) | List databases |
| GET | [/api/v1/database/{databaseId}/access/{username}](#getapiv1databasedatabaseidaccessusername) | Find/Check access |
| PUT | [/api/v1/database/{databaseId}/access/{username}](#putapiv1databasedatabaseidaccessusername) | Modify access |
| POST | [/api/v1/database/{databaseId}/access/{username}](#postapiv1databasedatabaseidaccessusername) | Give access |
| DELETE | [/api/v1/database/{databaseId}/access/{username}](#deleteapiv1databasedatabaseidaccessusername) | Delete access |
| HEAD | [/api/v1/database/{databaseId}/access/{username}](#headapiv1databasedatabaseidaccessusername) | Find/Check access |
| PUT | [/api/v1/message/{messageId}](#putapiv1messagemessageid) | Update message |
| DELETE | [/api/v1/message/{messageId}](#deleteapiv1messagemessageid) | Delete message |
| GET | [/api/v1/image/{imageId}](#getapiv1imageimageid) | Find image |
| PUT | [/api/v1/image/{imageId}](#putapiv1imageimageid) | Update image |
| DELETE | [/api/v1/image/{imageId}](#deleteapiv1imageimageid) | Delete image |
| GET | [/api/v1/identifier/{identifierId}](#getapiv1identifieridentifierid) | Find identifier |
| PUT | [/api/v1/identifier/{identifierId}](#putapiv1identifieridentifierid) | Save identifier |
| DELETE | [/api/v1/identifier/{identifierId}](#deleteapiv1identifieridentifierid) | Delete identifier |
| PUT | [/api/v1/identifier/{identifierId}/publish](#putapiv1identifieridentifieridpublish) | Publish identifier |
| PUT | [/api/v1/database/{databaseId}/visibility](#putapiv1databasedatabaseidvisibility) | Update database visibility |
| GET | [/api/v1/database/{databaseId}/view/{viewId}](#getapiv1databasedatabaseidviewviewid) | Get view |
| PUT | [/api/v1/database/{databaseId}/view/{viewId}](#putapiv1databasedatabaseidviewviewid) | Update view |
| DELETE | [/api/v1/database/{databaseId}/view/{viewId}](#deleteapiv1databasedatabaseidviewviewid) | Delete view |
| GET | [/api/v1/database/{databaseId}/table/{tableId}](#getapiv1databasedatabaseidtabletableid) | Find table |
| PUT | [/api/v1/database/{databaseId}/table/{tableId}](#putapiv1databasedatabaseidtabletableid) | Update table |
| DELETE | [/api/v1/database/{databaseId}/table/{tableId}](#deleteapiv1databasedatabaseidtabletableid) | Delete table |
| PUT | [/api/v1/database/{databaseId}/table/{tableId}/statistic](#putapiv1databasedatabaseidtabletableidstatistic) | Update statistics |
| PUT | [/api/v1/database/{databaseId}/table/{tableId}/column/{columnId}](#putapiv1databasedatabaseidtabletableidcolumncolumnid) | Update semantics |
| PUT | [/api/v1/database/{databaseId}/owner](#putapiv1databasedatabaseidowner) | Update database owner |
| PUT | [/api/v1/database/{databaseId}/metadata/view](#putapiv1databasedatabaseidmetadataview) | Update database view schemas |
| PUT | [/api/v1/database/{databaseId}/metadata/table](#putapiv1databasedatabaseidmetadatatable) | Update database table schemas |
| GET | [/api/v1/database/{databaseId}/image](#getapiv1databasedatabaseidimage) | Get database preview image |
| PUT | [/api/v1/database/{databaseId}/image](#putapiv1databasedatabaseidimage) | Update database preview image |
| GET | [/api/v1/message](#getapiv1message) | List messages |
| POST | [/api/v1/message](#postapiv1message) | Create message |
| GET | [/api/v1/image](#getapiv1image) | List images |
| POST | [/api/v1/image](#postapiv1image) | Create image |
| GET | [/api/v1/identifier](#getapiv1identifier) | List identifiers |
| POST | [/api/v1/identifier](#postapiv1identifier) | Create identifier |
| GET | [/api/v1/database/{databaseId}/view](#getapiv1databasedatabaseidview) | List views |
| POST | [/api/v1/database/{databaseId}/view](#postapiv1databasedatabaseidview) | Create view |
| GET | [/api/v1/database/{databaseId}/table](#getapiv1databasedatabaseidtable) | List tables |
| POST | [/api/v1/database/{databaseId}/table](#postapiv1databasedatabaseidtable) | Create table |
| GET | [/api/v1/container](#getapiv1container) | List containers |
| POST | [/api/v1/container](#postapiv1container) | Create container |
| GET | [/api/v1/user](#getapiv1user) | List users |
| GET | [/api/v1/oai](#getapiv1oai) | Get record |
| GET | [/api/v1/message/message/{messageId}](#getapiv1messagemessagemessageid) | Find message |
| GET | [/api/v1/license](#getapiv1license) | List licenses |
| GET | [/api/v1/identifier/retrieve](#getapiv1identifierretrieve) | Retrieve PID metadata |
| GET | [/api/v1/database/{databaseId}](#getapiv1databasedatabaseid) | Find database |
| GET | [/api/v1/container/{containerId}](#getapiv1containercontainerid) | Find container |
| DELETE | [/api/v1/container/{containerId}](#deleteapiv1containercontainerid) | Delete container |
| GET | [/api/v1/search](#getapiv1search) | Performs a fuzzy search |
| POST | [/api/v1/search/{field_type}](#postapiv1searchfield_type) | Performs a general search |
| GET | [/api/v1/search/{field_type}/fields](#getapiv1searchfield_typefields) | Get searchable fields |
| GET | [/api/v1/search/{index}](#getapiv1searchindex) | Gets the index |

## Reference Table

| Name | Path | Description |
| --- | --- | --- |
| basicAuth | [#/components/securitySchemes/basicAuth](#componentssecurityschemesbasicauth) |  |
| bearerAuth | [#/components/securitySchemes/bearerAuth](#componentssecurityschemesbearerauth) |  |
| DatabaseAccessDto | [#/components/schemas/DatabaseAccessDto](#componentsschemasdatabaseaccessdto) |  |
| UserBriefDto | [#/components/schemas/UserBriefDto](#componentsschemasuserbriefdto) |  |
| TupleUpdateDto | [#/components/schemas/TupleUpdateDto](#componentsschemastupleupdatedto) |  |
| QueryPersistDto | [#/components/schemas/QueryPersistDto](#componentsschemasquerypersistdto) |  |
| CreatorBriefDto | [#/components/schemas/CreatorBriefDto](#componentsschemascreatorbriefdto) |  |
| IdentifierBriefDto | [#/components/schemas/IdentifierBriefDto](#componentsschemasidentifierbriefdto) |  |
| IdentifierDescriptionDto | [#/components/schemas/IdentifierDescriptionDto](#componentsschemasidentifierdescriptiondto) |  |
| IdentifierTitleDto | [#/components/schemas/IdentifierTitleDto](#componentsschemasidentifiertitledto) |  |
| QueryDto | [#/components/schemas/QueryDto](#componentsschemasquerydto) |  |
| TupleDto | [#/components/schemas/TupleDto](#componentsschemastupledto) |  |
| ImportDto | [#/components/schemas/ImportDto](#componentsschemasimportdto) |  |
| ConditionalDto | [#/components/schemas/ConditionalDto](#componentsschemasconditionaldto) |  |
| FilterDto | [#/components/schemas/FilterDto](#componentsschemasfilterdto) |  |
| JoinDto | [#/components/schemas/JoinDto](#componentsschemasjoindto) |  |
| OrderDto | [#/components/schemas/OrderDto](#componentsschemasorderdto) |  |
| SubsetColumnDto | [#/components/schemas/SubsetColumnDto](#componentsschemassubsetcolumndto) |  |
| SubsetDto | [#/components/schemas/SubsetDto](#componentsschemassubsetdto) |  |
| ColumnAnalysisResultDto | [#/components/schemas/ColumnAnalysisResultDto](#componentsschemascolumnanalysisresultdto) |  |
| SchemaAnalysisResultDto | [#/components/schemas/SchemaAnalysisResultDto](#componentsschemasschemaanalysisresultdto) |  |
| TableHistoryDto | [#/components/schemas/TableHistoryDto](#componentsschemastablehistorydto) |  |
| TupleDeleteDto | [#/components/schemas/TupleDeleteDto](#componentsschemastupledeletedto) |  |
| UserAttributesDto | [#/components/schemas/UserAttributesDto](#componentsschemasuserattributesdto) |  |
| UserDto | [#/components/schemas/UserDto](#componentsschemasuserdto) |  |
| DatabaseBriefDto | [#/components/schemas/DatabaseBriefDto](#componentsschemasdatabasebriefdto) |  |
| UserUpdateDto | [#/components/schemas/UserUpdateDto](#componentsschemasuserupdatedto) |  |
| BannerMessageUpdateDto | [#/components/schemas/BannerMessageUpdateDto](#componentsschemasbannermessageupdatedto) |  |
| BannerMessageBriefDto | [#/components/schemas/BannerMessageBriefDto](#componentsschemasbannermessagebriefdto) |  |
| ImageChangeDto | [#/components/schemas/ImageChangeDto](#componentsschemasimagechangedto) |  |
| DataTypeDto | [#/components/schemas/DataTypeDto](#componentsschemasdatatypedto) |  |
| ImageDto | [#/components/schemas/ImageDto](#componentsschemasimagedto) |  |
| OperatorDto | [#/components/schemas/OperatorDto](#componentsschemasoperatordto) |  |
| IdentifierSaveDto | [#/components/schemas/IdentifierSaveDto](#componentsschemasidentifiersavedto) |  |
| LicenseDto | [#/components/schemas/LicenseDto](#componentsschemaslicensedto) |  |
| SaveIdentifierCreatorDto | [#/components/schemas/SaveIdentifierCreatorDto](#componentsschemassaveidentifiercreatordto) |  |
| SaveIdentifierDescriptionDto | [#/components/schemas/SaveIdentifierDescriptionDto](#componentsschemassaveidentifierdescriptiondto) |  |
| SaveIdentifierFunderDto | [#/components/schemas/SaveIdentifierFunderDto](#componentsschemassaveidentifierfunderdto) |  |
| SaveIdentifierTitleDto | [#/components/schemas/SaveIdentifierTitleDto](#componentsschemassaveidentifiertitledto) |  |
| SaveRelatedIdentifierDto | [#/components/schemas/SaveRelatedIdentifierDto](#componentsschemassaverelatedidentifierdto) |  |
| CreatorDto | [#/components/schemas/CreatorDto](#componentsschemascreatordto) |  |
| IdentifierDto | [#/components/schemas/IdentifierDto](#componentsschemasidentifierdto) |  |
| IdentifierFunderDto | [#/components/schemas/IdentifierFunderDto](#componentsschemasidentifierfunderdto) |  |
| LinksDto | [#/components/schemas/LinksDto](#componentsschemaslinksdto) |  |
| RelatedIdentifierDto | [#/components/schemas/RelatedIdentifierDto](#componentsschemasrelatedidentifierdto) |  |
| DatabaseModifyVisibilityDto | [#/components/schemas/DatabaseModifyVisibilityDto](#componentsschemasdatabasemodifyvisibilitydto) |  |
| ViewUpdateDto | [#/components/schemas/ViewUpdateDto](#componentsschemasviewupdatedto) |  |
| ViewBriefDto | [#/components/schemas/ViewBriefDto](#componentsschemasviewbriefdto) |  |
| TableUpdateDto | [#/components/schemas/TableUpdateDto](#componentsschemastableupdatedto) |  |
| TableBriefDto | [#/components/schemas/TableBriefDto](#componentsschemastablebriefdto) |  |
| ColumnSemanticsUpdateDto | [#/components/schemas/ColumnSemanticsUpdateDto](#componentsschemascolumnsemanticsupdatedto) |  |
| ColumnDto | [#/components/schemas/ColumnDto](#componentsschemascolumndto) |  |
| EnumDto | [#/components/schemas/EnumDto](#componentsschemasenumdto) |  |
| SetDto | [#/components/schemas/SetDto](#componentsschemassetdto) |  |
| DatabaseTransferDto | [#/components/schemas/DatabaseTransferDto](#componentsschemasdatabasetransferdto) |  |
| DatabaseModifyImageDto | [#/components/schemas/DatabaseModifyImageDto](#componentsschemasdatabasemodifyimagedto) |  |
| CreateAccessDto | [#/components/schemas/CreateAccessDto](#componentsschemascreateaccessdto) |  |
| BannerMessageCreateDto | [#/components/schemas/BannerMessageCreateDto](#componentsschemasbannermessagecreatedto) |  |
| ImageCreateDto | [#/components/schemas/ImageCreateDto](#componentsschemasimagecreatedto) |  |
| CreateIdentifierCreatorDto | [#/components/schemas/CreateIdentifierCreatorDto](#componentsschemascreateidentifiercreatordto) |  |
| CreateIdentifierDescriptionDto | [#/components/schemas/CreateIdentifierDescriptionDto](#componentsschemascreateidentifierdescriptiondto) |  |
| CreateIdentifierDto | [#/components/schemas/CreateIdentifierDto](#componentsschemascreateidentifierdto) |  |
| CreateIdentifierFunderDto | [#/components/schemas/CreateIdentifierFunderDto](#componentsschemascreateidentifierfunderdto) |  |
| CreateIdentifierTitleDto | [#/components/schemas/CreateIdentifierTitleDto](#componentsschemascreateidentifiertitledto) |  |
| CreateRelatedIdentifierDto | [#/components/schemas/CreateRelatedIdentifierDto](#componentsschemascreaterelatedidentifierdto) |  |
| CreateDatabaseDto | [#/components/schemas/CreateDatabaseDto](#componentsschemascreatedatabasedto) |  |
| CreateViewDto | [#/components/schemas/CreateViewDto](#componentsschemascreateviewdto) |  |
| CreateForeignKeyDto | [#/components/schemas/CreateForeignKeyDto](#componentsschemascreateforeignkeydto) |  |
| CreateTableColumnDto | [#/components/schemas/CreateTableColumnDto](#componentsschemascreatetablecolumndto) |  |
| CreateTableConstraintsDto | [#/components/schemas/CreateTableConstraintsDto](#componentsschemascreatetableconstraintsdto) |  |
| CreateTableDto | [#/components/schemas/CreateTableDto](#componentsschemascreatetabledto) |  |
| CreateContainerDto | [#/components/schemas/CreateContainerDto](#componentsschemascreatecontainerdto) |  |
| ContainerDto | [#/components/schemas/ContainerDto](#componentsschemascontainerdto) |  |
| ApiErrorDto | [#/components/schemas/ApiErrorDto](#componentsschemasapierrordto) |  |
| OaiListRecordsParameters | [#/components/schemas/OaiListRecordsParameters](#componentsschemasoailistrecordsparameters) |  |
| BannerMessageDto | [#/components/schemas/BannerMessageDto](#componentsschemasbannermessagedto) |  |
| ImageBriefDto | [#/components/schemas/ImageBriefDto](#componentsschemasimagebriefdto) |  |
| LdCreatorDto | [#/components/schemas/LdCreatorDto](#componentsschemasldcreatordto) |  |
| LdDatasetDto | [#/components/schemas/LdDatasetDto](#componentsschemaslddatasetdto) |  |
| ViewColumnDto | [#/components/schemas/ViewColumnDto](#componentsschemasviewcolumndto) |  |
| ViewDto | [#/components/schemas/ViewDto](#componentsschemasviewdto) |  |
| ColumnBriefDto | [#/components/schemas/ColumnBriefDto](#componentsschemascolumnbriefdto) |  |
| ConstraintsDto | [#/components/schemas/ConstraintsDto](#componentsschemasconstraintsdto) |  |
| ForeignKeyBriefDto | [#/components/schemas/ForeignKeyBriefDto](#componentsschemasforeignkeybriefdto) |  |
| ForeignKeyDto | [#/components/schemas/ForeignKeyDto](#componentsschemasforeignkeydto) |  |
| ForeignKeyReferenceDto | [#/components/schemas/ForeignKeyReferenceDto](#componentsschemasforeignkeyreferencedto) |  |
| PrimaryKeyDto | [#/components/schemas/PrimaryKeyDto](#componentsschemasprimarykeydto) |  |
| TableDto | [#/components/schemas/TableDto](#componentsschemastabledto) |  |
| UniqueDto | [#/components/schemas/UniqueDto](#componentsschemasuniquedto) |  |
| ContainerBriefDto | [#/components/schemas/ContainerBriefDto](#componentsschemascontainerbriefdto) |  |
| ApiError | [#/components/schemas/ApiError](#componentsschemasapierror) |  |
| IndexDto | [#/components/schemas/IndexDto](#componentsschemasindexdto) |  |
| IndexFieldDto | [#/components/schemas/IndexFieldDto](#componentsschemasindexfielddto) |  |
| IndexFieldsDto | [#/components/schemas/IndexFieldsDto](#componentsschemasindexfieldsdto) |  |
| SearchRequestDto | [#/components/schemas/SearchRequestDto](#componentsschemassearchrequestdto) |  |

## Path Details

***

### [GET]/api/v1/database/{databaseId}/view/{viewId}/data

- Summary  
Get view data

- Description  
Gets data from a view of a database. For private databases, the user needs at least *READ* access to the associated database.

- Security  
basicAuth  
bearerAuth  

#### Parameters(Query)

```ts
page?: integer
```

```ts
size?: integer
```

```ts
timestamp?: string
```

#### Headers

```ts
Accept: string
```

#### Responses

- 200 Retrieved view data

`application/json`

```ts
{
  "type": "string"
}
```

`text/csv`

- 400 Request pagination is malformed

- 403 Not allowed to retrieve view data

- 404 Failed to find view in metadata database

- 406 Failed to format data

- 409 View schema could not be mapped

- 503 Failed to establish connection with the metadata service

***

### [HEAD]/api/v1/database/{databaseId}/view/{viewId}/data

- Summary  
Get view data

- Description  
Gets data from a view of a database. For private databases, the user needs at least *READ* access to the associated database.

- Security  
basicAuth  
bearerAuth  

#### Parameters(Query)

```ts
page?: integer
```

```ts
size?: integer
```

```ts
timestamp?: string
```

#### Headers

```ts
Accept: string
```

#### Responses

- 200 Retrieved view data

`application/json`

```ts
{
  "type": "string"
}
```

`text/csv`

- 400 Request pagination is malformed

- 403 Not allowed to retrieve view data

- 404 Failed to find view in metadata database

- 406 Failed to format data

- 409 View schema could not be mapped

- 503 Failed to establish connection with the metadata service

***

### [GET]/api/v1/database/{databaseId}/table/{tableId}/data

- Summary  
Get table data

- Description  
Gets data from a table with id. For a table in a private database, the user needs to have at least *READ* access to the associated database. Requests with HTTP method **GET** return the full dataset, requests with HTTP method **HEAD** only the number of tuples in the `X-Count` header.

- Security  
basicAuth  
bearerAuth  

#### Parameters(Query)

```ts
timestamp?: string
```

```ts
page?: integer
```

```ts
size?: integer
```

#### Headers

```ts
Accept: string
```

#### Responses

- 200 Get table data

`application/json`

```ts
{
  "type": "string"
}
```

`text/csv`

- 400 Request pagination or table data select query is malformed

- 403 Not allowed to get table data

- 404 Failed to find table in metadata database

- 406 Failed to format data

- 503 Failed to establish connection with the metadata service

***

### [PUT]/api/v1/database/{databaseId}/table/{tableId}/data

- Summary  
Update tuple

- Description  
Updates a data tuple into a table, then the table statistics are updated. The user needs to have at least *WRITE_OWN* access to the associated database. Requires role `insert-table-data`.

- Security  
basicAuth  
bearerAuth  

#### RequestBody

- application/json

```ts
{
  // The key-value data map
  data: {
  }
  // The map of conditions
  keys: {
  }
}
```

#### Responses

- 202 Updated table data

- 400 Request pagination or table data select query is malformed

- 403 Update table data not allowed

- 404 Failed to find table in metadata database

- 503 Failed to establish connection with the metadata service

***

### [POST]/api/v1/database/{databaseId}/table/{tableId}/data

- Summary  
Insert tuple

- Description  
Inserts a data tuple into a table, then the table statistics are updated. The user needs to have at least *WRITE_OWN* access to the associated database. Requires role `insert-table-data`.

- Security  
basicAuth  
bearerAuth  

#### RequestBody

- application/json

```ts
{
  // The key-value data map
  data: {
  }
}
```

#### Responses

- 201 Created table data

- 400 Request pagination or table data select query is malformed

- 403 Create table data not allowed

- 404 Failed to find table in metadata database or blob in storage service

- 503 Failed to establish connection with the metadata service or storage service

***

### [DELETE]/api/v1/database/{databaseId}/table/{tableId}/data

- Summary  
Delete tuple

- Description  
Deletes a data tuple into a table, then the table statistics are updated. The user needs to have at least *WRITE_OWN* access to the associated database. Requires role `delete-table-data`.

- Security  
basicAuth  
bearerAuth  

#### RequestBody

- application/json

```ts
{
  // The map of conditions
  keys: {
  }
}
```

#### Responses

- 202 Deleted table data

- 400 Request pagination or table data select query is malformed

- 403 Delete table data not allowed

- 404 Failed to find table in metadata database

- 503 Failed to establish connection with the metadata service

***

### [HEAD]/api/v1/database/{databaseId}/table/{tableId}/data

- Summary  
Get table data

- Description  
Gets data from a table with id. For a table in a private database, the user needs to have at least *READ* access to the associated database. Requests with HTTP method **GET** return the full dataset, requests with HTTP method **HEAD** only the number of tuples in the `X-Count` header.

- Security  
basicAuth  
bearerAuth  

#### Parameters(Query)

```ts
timestamp?: string
```

```ts
page?: integer
```

```ts
size?: integer
```

#### Headers

```ts
Accept: string
```

#### Responses

- 200 Get table data

`application/json`

```ts
{
  "type": "string"
}
```

`text/csv`

- 400 Request pagination or table data select query is malformed

- 403 Not allowed to get table data

- 404 Failed to find table in metadata database

- 406 Failed to format data

- 503 Failed to establish connection with the metadata service

***

### [GET]/api/v1/database/{databaseId}/subset/{subsetId}/data

- Summary  
Get subset data

- Description  
Gets data of subset with id. For private databases, the user needs at least *READ* grant to the associated database. Requests with HTTP method **GET** return the subset dataset. Requests with HTTP method **HEAD** only the number of rows in the subset dataset in the `X-Count` header, the subset id in the `X-Id` header and the `sha256`-result set hash in the `X-Result-Hash` header. Requests with HTTP method **GET** additionally return the result set in the response body.

- Security  
bearerAuth  
basicAuth  

#### Parameters(Query)

```ts
timestamp?: string
```

```ts
page?: integer
```

```ts
size?: integer
```

#### Headers

```ts
Accept: string
```

#### Responses

- 200 Retrieved subset data

`application/json`

```ts
{
  "type": "string"
}
```

`text/csv`

- 400 Invalid pagination

- 403 Not allowed to retrieve subset data

- 404 Failed to find database in metadata database or query in query store of the data database

- 406 Failed to format data

- 422 Failed to re-execute query

- 503 Failed to communicate with database

***

### [HEAD]/api/v1/database/{databaseId}/subset/{subsetId}/data

- Summary  
Get subset data

- Description  
Gets data of subset with id. For private databases, the user needs at least *READ* grant to the associated database. Requests with HTTP method **GET** return the subset dataset. Requests with HTTP method **HEAD** only the number of rows in the subset dataset in the `X-Count` header, the subset id in the `X-Id` header and the `sha256`-result set hash in the `X-Result-Hash` header. Requests with HTTP method **GET** additionally return the result set in the response body.

- Security  
bearerAuth  
basicAuth  

#### Parameters(Query)

```ts
timestamp?: string
```

```ts
page?: integer
```

```ts
size?: integer
```

#### Headers

```ts
Accept: string
```

#### Responses

- 200 Retrieved subset data

`application/json`

```ts
{
  "type": "string"
}
```

`text/csv`

- 400 Invalid pagination

- 403 Not allowed to retrieve subset data

- 404 Failed to find database in metadata database or query in query store of the data database

- 406 Failed to format data

- 422 Failed to re-execute query

- 503 Failed to communicate with database

***

### [GET]/api/v1/database/{databaseId}/grant/{username}

- Summary  
Get grants

- Description  
Get the grant permissions for a user of a given database.

- Security  
basicAuth  
bearerAuth  

#### Responses

- 200 Access found

`application/json`

```ts
{
  // The user name
  username: string
  user: {
    id?: string
    // Only contains lowercase characters
    username: string
    name?: string
    orcid?: string
    qualified_name?: string
    given_name?: string
    family_name?: string
  }
  type: enum[read, write_own, write_all]
}[]
```

- 401 Not authenticated

- 403 Not database owner or foreign user

- 404 Failed to find access

- 409 Grants malformed

***

### [HEAD]/api/v1/database/{databaseId}/grant/{username}

- Summary  
Get grants

- Description  
Get the grant permissions for a user of a given database.

- Security  
basicAuth  
bearerAuth  

#### Responses

- 200 Access found

`application/json`

```ts
{
  // The user name
  username: string
  user: {
    id?: string
    // Only contains lowercase characters
    username: string
    name?: string
    orcid?: string
    qualified_name?: string
    given_name?: string
    family_name?: string
  }
  type: enum[read, write_own, write_all]
}[]
```

- 401 Not authenticated

- 403 Not database owner or foreign user

- 404 Failed to find access

- 409 Grants malformed

***

### [PUT]/api/v1/database/{databaseId}/subset/{queryId}

- Summary  
Persist subset

- Description  
Persists a subset with id. Requires role `persist-query`.

- Security  
bearerAuth  
basicAuth  

#### RequestBody

- application/json

```ts
{
  // If false, the query is marked for deletion at a later point in time
  persist: boolean
}
```

#### Responses

- 202 Persisted subset

`application/json`

```ts
{
  // The query id
  id: string
  owner: {
    id?: string
    // Only contains lowercase characters
    username: string
    name?: string
    orcid?: string
    qualified_name?: string
    given_name?: string
    family_name?: string
  }
  // The timestamp when the query was executed
  execution: string
  // The mapped SQL query
  query: string
  // The query type
  type?: enum[query, view]
  identifiers: {
    id: string
    type: enum[database, subset, table, view]
    creators: {
      id: string
      affiliation?: string
      creator_name: string
      name_type?: enum[Personal, Organizational]
      name_identifier?: string
      name_identifier_scheme?: enum[ORCID, ROR, ISNI, GRID]
      affiliation_identifier?: string
      affiliation_identifier_scheme?: enum[ROR, GRID, ISNI]
    }[]
    titles: {
      id: string
      title?: string
      language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
      type?: enum[AlternativeTitle, Subtitle, TranslatedTitle, Other]
    }[]
    descriptions: {
      id: string
      description?: string
      language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
      type?: enum[Abstract, Methods, SeriesInformation, TableOfContents, TechnicalInfo, Other]
    }[]
    doi?: string
    publisher: string
    status: enum[draft, published]
    created?: string
    database_id: string
    query_id?: string
    table_id?: string
    view_id?: string
    publication_year: integer
    // The owner username
    owned_by: string
  }[]
  // The database id
  database_id: string
  // The normalized SQL query as executed
  query_normalized: string
  // The sha256-hash of the mapped query
  query_hash: string
  // The sha256-hash of the result
  result_hash?: string
  // The row count of the result
  result_number?: integer
  // If false, the query is marked for deletion at a later point in time
  is_persisted: boolean
}
```

- 400 Malformed select query

- 403 Not allowed to persist subset

- 404 Failed to find database in metadata database or query in query store of the data database

- 417 Failed to persist subset

- 503 Failed to communicate with database

***

### [POST]/api/v1/upload

- Summary  
Uploads a multipart file

- Description  
Uploads a multipart file to the Storage Service. Requires role `upload-file`.

- Security  
basicAuth  
bearerAuth  

#### RequestBody

- application/json

```ts
{
  file: string
}
```

#### Responses

- 201 Uploaded the file

- 204 File already present

- 503 Failed to establish connection with the storage service

***

### [POST]/api/v1/database/{databaseId}/table/{tableId}/data/import

- Summary  
Import dataset

- Description  
Imports a dataset in a table. Then update the table statistics. The user needs to have at least *WRITE_OWN* access to the associated database when importing into a owned table. Otherwise *WRITE_ALL* access in needed. Requires role `insert-table-data`.

- Security  
basicAuth  
bearerAuth  

#### Headers

```ts
Authorization: string
```

#### RequestBody

- application/json

```ts
{
  // The key of the S3 binary object in the storage service
  location: string
  // If true, the first line contains the column names, otherwise it contains only data
  header: boolean
  // The column delimiter of the dataset
  separator: string
  // The quote symbol around values in the dataset
  quote?: string
  // The newline symbol
  line_termination?: string
}
```

#### Responses

- 202 Imported dataset successfully

- 400 Dataset and/or query are malformed

- 403 Import table dataset not allowed

- 404 Failed to find table in metadata database

- 503 Failed to establish connection with the metadata service

***

### [GET]/api/v1/database/{databaseId}/subset

- Summary  
Find subsets

- Description  
Finds subsets in the query store. When the database schema is marked as hidden, the user needs to be authorized, have at least read-access to the database. The result can be optionally filtered by setting `persisted`. When set to *true*, only persisted queries are returned, otherwise only non-persisted queries are returned.

- Security  
basicAuth  
bearerAuth  

#### Parameters(Query)

```ts
persisted?: boolean
```

#### Responses

- 200 Found subsets

`application/json`

```ts
{
  // The query id
  id: string
  owner: {
    id?: string
    // Only contains lowercase characters
    username: string
    name?: string
    orcid?: string
    qualified_name?: string
    given_name?: string
    family_name?: string
  }
  // The timestamp when the query was executed
  execution: string
  // The mapped SQL query
  query: string
  // The query type
  type?: enum[query, view]
  identifiers: {
    id: string
    type: enum[database, subset, table, view]
    creators: {
      id: string
      affiliation?: string
      creator_name: string
      name_type?: enum[Personal, Organizational]
      name_identifier?: string
      name_identifier_scheme?: enum[ORCID, ROR, ISNI, GRID]
      affiliation_identifier?: string
      affiliation_identifier_scheme?: enum[ROR, GRID, ISNI]
    }[]
    titles: {
      id: string
      title?: string
      language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
      type?: enum[AlternativeTitle, Subtitle, TranslatedTitle, Other]
    }[]
    descriptions: {
      id: string
      description?: string
      language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
      type?: enum[Abstract, Methods, SeriesInformation, TableOfContents, TechnicalInfo, Other]
    }[]
    doi?: string
    publisher: string
    status: enum[draft, published]
    created?: string
    database_id: string
    query_id?: string
    table_id?: string
    view_id?: string
    publication_year: integer
    // The owner username
    owned_by: string
  }[]
  // The database id
  database_id: string
  // The normalized SQL query as executed
  query_normalized: string
  // The sha256-hash of the mapped query
  query_hash: string
  // The sha256-hash of the result
  result_hash?: string
  // The row count of the result
  result_number?: integer
  // If false, the query is marked for deletion at a later point in time
  is_persisted: boolean
}[]
```

- 403 Not allowed to find subsets

- 404 Failed to find database or user in metadata database or query in query store of the data database

- 503 Failed to communicate with database

***

### [POST]/api/v1/database/{databaseId}/subset

- Summary  
Create subset

- Description  
Creates a subset in the query store of the data database. Can also be used without authentication if (and only if) the database is marked as public (i.e. when `is_public` = `is_schema_public` is set to `true`). Otherwise at least read access is required.

- Security  
basicAuth  
bearerAuth  

#### Parameters(Query)

```ts
timestamp?: string
```

```ts
page?: integer
```

```ts
size?: integer
```

#### RequestBody

- application/json

```ts
{
  columns: {
    // The id of the column
    id: string
    // The column alias
    alias?: string
  }[]
  joins: {
    // The type of join
    type: enum[inner, left, right, cross]
    conditionals: {
      // The id of the column
      column_id: string
      // The id of the foreign column
      foreign_column_id: string
    }[]
    // The id of the data source
    datasource_id: string
  }[]
  filters: {
    // The filter type
    type: enum[where, or, and]
    // The filter value
    value?: string
    // The column id
    column_id: string
    // The operator id
    operator_id: string
  }[]
  orders: {
    // The sort direction
    direction?: enum[asc, desc]
    // The column id
    column_id: string
  }[]
  datasource_ids?: string[]
}
```

#### Responses

- 201 Created subset

`application/json`

```ts
{
  "type": "string"
}
```

- 400 Malformed select query

- 403 Not allowed to find subset

- 404 Failed to find database in metadata database or query in query store of the data database

- 406 Failed to format data

- 417 Failed to insert query into query store of data database

- 422 Failed to execute query

- 501 Failed to execute query as it contains non-supported keywords

- 503 Failed to communicate with database

***

### [GET]/api/v1/image/{imageId}/analyse/schema/{key}

- Summary  
Analyse schema

- Description  
Analyses a dataset stored at the Storage Service and attempts to map the datatypes, requires role `analyse-datatypes`.

- Security  
basicAuth  

#### Responses

- 200 Analysed schema successfully

`application/json`

```ts
{
  // The column delimiter of the dataset
  delimiter: string
  // The quote symbol around values in the dataset
  quote: string
  // The escape symbol
  escape: string
  // The comment symbol
  comment: string
  columns: {
    // The column name
    name: string
    // The column data type
    datatype: enum[char, varchar, binary, varbinary, tinyblob, tinytext, text, blob, mediumtext, mediumblob, longtext, longblob, enum, set, serial, bit, tinyint, bool, smallint, mediumint, int, bigint, float, double, decimal, date, datetime, timestamp, time, year]
    // The size determines the number of digits before the comma: x=size-d where size >= d
    size?: integer
    // The digits behind the comma
    d?: integer
    enums?: string[]
    sets?: string[]
    // If set to true, the column value can be null
    null_allowed?: boolean
    // The column is a candidate to be part of a composite primary key
    primary_key?: boolean
  }[]
  // The newline symbol
  newline_delimiter: string
  // The number of rows to skip
  skip_rows: integer
  // Detected the first line as header
  has_header: boolean
  // The format of the date
  date_format?: string
  // The format of the timestamp
  timestamp_format?: string
}
```

- 400 Schema is malformed or does not fit the image

- 404 Failed to find image or dataset

- 503 Failed to establish connection to metadata service

***

### [GET]/api/v1/database/{databaseId}/table/{tableId}/history

- Summary  
Get history

- Description  
Gets the insert/delete operations history performed. For tables in private databases, the user needs to have at least *READ* access to the associated database.

- Security  
basicAuth  
bearerAuth  

#### Parameters(Query)

```ts
size?: integer
```

#### Responses

- 200 Found table history

`application/json`

```ts
{
  // The timestamp
  timestamp: string
  // The event type
  event: enum[insert, delete]
  // The total number of rows affected by the event
  total: integer
}[]
```

- 400 Invalid pagination size request, must be > 0

- 403 Find table history not allowed

- 404 Failed to find table history in data database

- 503 Failed to establish connection with the metadata service

***

### [GET]/api/v1/database/{databaseId}/subset/{subsetId}

- Summary  
Find subset

- Description  
Finds a subset in the data database.  When the database schema is marked as hidden, the user needs to be authorized, have at least read-access to the database.  Requests with HTTP header `Accept=application/json` return the metadata, requests with HTTP header `Accept=text/csv` return the data as downloadable file.

- Security  
basicAuth  
bearerAuth  

#### Parameters(Query)

```ts
timestamp?: string
```

#### Responses

- 200 Found subset

`application/json`

```ts
{
  // The query id
  id: string
  owner: {
    id?: string
    // Only contains lowercase characters
    username: string
    name?: string
    orcid?: string
    qualified_name?: string
    given_name?: string
    family_name?: string
  }
  // The timestamp when the query was executed
  execution: string
  // The mapped SQL query
  query: string
  // The query type
  type?: enum[query, view]
  identifiers: {
    id: string
    type: enum[database, subset, table, view]
    creators: {
      id: string
      affiliation?: string
      creator_name: string
      name_type?: enum[Personal, Organizational]
      name_identifier?: string
      name_identifier_scheme?: enum[ORCID, ROR, ISNI, GRID]
      affiliation_identifier?: string
      affiliation_identifier_scheme?: enum[ROR, GRID, ISNI]
    }[]
    titles: {
      id: string
      title?: string
      language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
      type?: enum[AlternativeTitle, Subtitle, TranslatedTitle, Other]
    }[]
    descriptions: {
      id: string
      description?: string
      language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
      type?: enum[Abstract, Methods, SeriesInformation, TableOfContents, TechnicalInfo, Other]
    }[]
    doi?: string
    publisher: string
    status: enum[draft, published]
    created?: string
    database_id: string
    query_id?: string
    table_id?: string
    view_id?: string
    publication_year: integer
    // The owner username
    owned_by: string
  }[]
  // The database id
  database_id: string
  // The normalized SQL query as executed
  query_normalized: string
  // The sha256-hash of the mapped query
  query_hash: string
  // The sha256-hash of the result
  result_hash?: string
  // The row count of the result
  result_number?: integer
  // If false, the query is marked for deletion at a later point in time
  is_persisted: boolean
}
```

`text/csv`

- 400 Malformed select query

- 403 Not allowed to find subset

- 404 Failed to find database in metadata database or query in query store of the data database

- 406 Failed to find acceptable representation

- 503 Failed to communicate with database

***

### [GET]/api/v1/user/{username}

- Summary  
Get user

- Description  
Gets own user information from the metadata database. Requires authentication. Foreign user information can only be obtained if additional role `find-foreign-user` is present. Finding information about internal users results in a 404 error.

- Security  
bearerAuth  
basicAuth  

#### Responses

- 200 Found user

`application/json`

```ts
{
  id?: string
  name?: string
  username: string
  password?: string
  attributes: {
    theme: string
    orcid?: string
    affiliation?: string
    language: string
  }
  qualified_name?: string
  given_name?: string
  family_name?: string
}
```

- 403 Find user is not permitted

- 404 User was not found

***

### [PUT]/api/v1/user/{username}

- Summary  
Update user

- Description  
Updates user with given username. Requires role `modify-user-information`.

- Security  
bearerAuth  
basicAuth  

#### RequestBody

- application/json

```ts
{
  firstname?: string
  lastname?: string
  affiliation?: string
  orcid?: string
  theme: string
  language: string
}
```

#### Responses

- 202 Modified user information

`application/json`

```ts
{
  id?: string
  name?: string
  username: string
  password?: string
  attributes: {
    theme: string
    orcid?: string
    affiliation?: string
    language: string
  }
  qualified_name?: string
  given_name?: string
  family_name?: string
}
```

- 400 Modify user query is malformed

- 403 Not allowed to modify user metadata

- 404 Failed to find database/user in metadata database

- 503 Failed to modify user at auth service

***

### [HEAD]/api/v1/user/{username}

- Summary  
Get user

- Description  
Gets own user information from the metadata database. Requires authentication. Foreign user information can only be obtained if additional role `find-foreign-user` is present. Finding information about internal users results in a 404 error.

- Security  
bearerAuth  
basicAuth  

#### Responses

- 200 Found user

`application/json`

```ts
{
  id?: string
  name?: string
  username: string
  password?: string
  attributes: {
    theme: string
    orcid?: string
    affiliation?: string
    language: string
  }
  qualified_name?: string
  given_name?: string
  family_name?: string
}
```

- 403 Find user is not permitted

- 404 User was not found

***

### [GET]/api/v1/database

- Summary  
List databases

- Description  
Lists all databases in the metadata database. Requests with HTTP method **GET** return the list of databases, requests with HTTP method **HEAD** only the number in the `X-Count` header.

#### Parameters(Query)

```ts
internal_name?: string
```

#### Responses

- 200 List of databases

`application/json`

```ts
{
  id: string
  // The name
  name: string
  // The comment
  description?: string
  identifiers: {
    id: string
    type: enum[database, subset, table, view]
    creators: {
      id: string
      affiliation?: string
      creator_name: string
      name_type?: enum[Personal, Organizational]
      name_identifier?: string
      name_identifier_scheme?: enum[ORCID, ROR, ISNI, GRID]
      affiliation_identifier?: string
      affiliation_identifier_scheme?: enum[ROR, GRID, ISNI]
    }[]
    titles: {
      id: string
      title?: string
      language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
      type?: enum[AlternativeTitle, Subtitle, TranslatedTitle, Other]
    }[]
    descriptions: {
      id: string
      description?: string
      language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
      type?: enum[Abstract, Methods, SeriesInformation, TableOfContents, TechnicalInfo, Other]
    }[]
    doi?: string
    publisher: string
    status: enum[draft, published]
    created?: string
    database_id: string
    query_id?: string
    table_id?: string
    view_id?: string
    publication_year: integer
    // The owner username
    owned_by: string
  }[]
  // The machine-friendly database name
  internal_name: string
  // The visibility; if true, The will be displayed publicly and is searchable
  is_public: boolean
  // The insights; if true, The schema will be displayed publicly and is searchable
  is_schema_public: boolean
  contact: {
    id?: string
    // Only contains lowercase characters
    username: string
    name?: string
    orcid?: string
    qualified_name?: string
    given_name?: string
    family_name?: string
  }
  // The owner username
  owned_by: string
  // The preview image
  preview_image?: string
}[]
```

***

### [POST]/api/v1/database

- Summary  
Create database

- Description  
Creates a database in the container with id. Requires roles `create-database`.

- Security  
bearerAuth  
basicAuth  

#### RequestBody

- application/json

```ts
{
  // The name
  name: string
  // The container id
  container_id: string
  // The visibility; if true, The will be displayed publicly and is searchable
  is_public: boolean
  // The insights; if true, The schema will be displayed publicly and is searchable
  is_schema_public: boolean
}
```

#### Responses

- 201 Created a new database

`application/json`

```ts
{
  id: string
  // The name
  name: string
  // The comment
  description?: string
  identifiers: {
    id: string
    type: enum[database, subset, table, view]
    creators: {
      id: string
      affiliation?: string
      creator_name: string
      name_type?: enum[Personal, Organizational]
      name_identifier?: string
      name_identifier_scheme?: enum[ORCID, ROR, ISNI, GRID]
      affiliation_identifier?: string
      affiliation_identifier_scheme?: enum[ROR, GRID, ISNI]
    }[]
    titles: {
      id: string
      title?: string
      language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
      type?: enum[AlternativeTitle, Subtitle, TranslatedTitle, Other]
    }[]
    descriptions: {
      id: string
      description?: string
      language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
      type?: enum[Abstract, Methods, SeriesInformation, TableOfContents, TechnicalInfo, Other]
    }[]
    doi?: string
    publisher: string
    status: enum[draft, published]
    created?: string
    database_id: string
    query_id?: string
    table_id?: string
    view_id?: string
    publication_year: integer
    // The owner username
    owned_by: string
  }[]
  // The machine-friendly database name
  internal_name: string
  // The visibility; if true, The will be displayed publicly and is searchable
  is_public: boolean
  // The insights; if true, The schema will be displayed publicly and is searchable
  is_schema_public: boolean
  contact: {
    id?: string
    // Only contains lowercase characters
    username: string
    name?: string
    orcid?: string
    qualified_name?: string
    given_name?: string
    family_name?: string
  }
  // The owner username
  owned_by: string
  // The preview image
  preview_image?: string
}
```

- 400 Database create query is malformed or image is not supported

- 403 Database create permission is missing or grant permissions at broker service failed

- 404 Failed to fin container/user/database in metadata database

- 409 Query store could not be created

- 423 Database quota exceeded

- 502 Connection to search service failed

- 503 Failed to save in search service

***

### [HEAD]/api/v1/database

- Summary  
List databases

- Description  
Lists all databases in the metadata database. Requests with HTTP method **GET** return the list of databases, requests with HTTP method **HEAD** only the number in the `X-Count` header.

#### Parameters(Query)

```ts
internal_name?: string
```

#### Responses

- 200 List of databases

`application/json`

```ts
{
  id: string
  // The name
  name: string
  // The comment
  description?: string
  identifiers: {
    id: string
    type: enum[database, subset, table, view]
    creators: {
      id: string
      affiliation?: string
      creator_name: string
      name_type?: enum[Personal, Organizational]
      name_identifier?: string
      name_identifier_scheme?: enum[ORCID, ROR, ISNI, GRID]
      affiliation_identifier?: string
      affiliation_identifier_scheme?: enum[ROR, GRID, ISNI]
    }[]
    titles: {
      id: string
      title?: string
      language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
      type?: enum[AlternativeTitle, Subtitle, TranslatedTitle, Other]
    }[]
    descriptions: {
      id: string
      description?: string
      language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
      type?: enum[Abstract, Methods, SeriesInformation, TableOfContents, TechnicalInfo, Other]
    }[]
    doi?: string
    publisher: string
    status: enum[draft, published]
    created?: string
    database_id: string
    query_id?: string
    table_id?: string
    view_id?: string
    publication_year: integer
    // The owner username
    owned_by: string
  }[]
  // The machine-friendly database name
  internal_name: string
  // The visibility; if true, The will be displayed publicly and is searchable
  is_public: boolean
  // The insights; if true, The schema will be displayed publicly and is searchable
  is_schema_public: boolean
  contact: {
    id?: string
    // Only contains lowercase characters
    username: string
    name?: string
    orcid?: string
    qualified_name?: string
    given_name?: string
    family_name?: string
  }
  // The owner username
  owned_by: string
  // The preview image
  preview_image?: string
}[]
```

***

### [GET]/api/v1/database/{databaseId}/access/{username}

- Summary  
Find/Check access

- Description  
Finds or checks access of a user with given username to a database with given id. Requests with HTTP method **GET** return the access object, requests with HTTP method **HEAD** only the status. When the user has at least *READ* access, the status 200 is returned, 403 otherwise. Requires role `check-database-access` or `check-foreign-database-access`.

- Security  
bearerAuth  
basicAuth  

#### Responses

- 200 Found database access

`application/json`

```ts
{
  // The user name
  username: string
  user: {
    id?: string
    // Only contains lowercase characters
    username: string
    name?: string
    orcid?: string
    qualified_name?: string
    given_name?: string
    family_name?: string
  }
  type: enum[read, write_own, write_all]
}
```

- 403 No access to this database

- 404 Database not found

***

### [PUT]/api/v1/database/{databaseId}/access/{username}

- Summary  
Modify access

- Description  
Modifies access of a user with given username to database with given id. Requires role `update-database-access`.

- Security  
bearerAuth  
basicAuth  

#### RequestBody

- application/json

```ts
{
  // The access type
  type: enum[read, write_own, write_all]
}
```

#### Responses

- 202 Modified access

- 400 Modify access query or database connection is malformed

- 403 Modify access not permitted when no access is granted in the first place

- 404 Database or user not found

- 502 Access could not be updated due to connection error in the data service

- 503 Access could not be updated in the data service

***

### [POST]/api/v1/database/{databaseId}/access/{username}

- Summary  
Give access

- Description  
Give a user with given username access to some database with given id. Requires role `create-database-access`.

- Security  
bearerAuth  
basicAuth  

#### RequestBody

- application/json

```ts
{
  // The access type
  type: enum[read, write_own, write_all]
}
```

#### Responses

- 202 Granting access succeeded

`application/json`

```ts
{
  // The user name
  username: string
  user: {
    id?: string
    // Only contains lowercase characters
    username: string
    name?: string
    orcid?: string
    qualified_name?: string
    given_name?: string
    family_name?: string
  }
  type: enum[read, write_own, write_all]
}
```

- 400 Granting access query or database connection is malformed

- 403 Failed giving access

- 404 Database or user not found

- 502 Access could not be created due to connection error

- 503 Access could not be created in the data service

***

### [DELETE]/api/v1/database/{databaseId}/access/{username}

- Summary  
Delete access

- Description  
Delete access of a user with given username to a database with id. Requires role `delete-database-access`.

- Security  
bearerAuth  
basicAuth  

#### Responses

- 202 Deleted access

- 400 Modify access query or database connection is malformed

- 403 Revoke of access not permitted as no access was found

- 404 User, database with access was not found

- 502 Access could not be created due to connection error

- 503 Access could not be revoked in the data service

***

### [HEAD]/api/v1/database/{databaseId}/access/{username}

- Summary  
Find/Check access

- Description  
Finds or checks access of a user with given username to a database with given id. Requests with HTTP method **GET** return the access object, requests with HTTP method **HEAD** only the status. When the user has at least *READ* access, the status 200 is returned, 403 otherwise. Requires role `check-database-access` or `check-foreign-database-access`.

- Security  
bearerAuth  
basicAuth  

#### Responses

- 200 Found database access

`application/json`

```ts
{
  // The user name
  username: string
  user: {
    id?: string
    // Only contains lowercase characters
    username: string
    name?: string
    orcid?: string
    qualified_name?: string
    given_name?: string
    family_name?: string
  }
  type: enum[read, write_own, write_all]
}
```

- 403 No access to this database

- 404 Database not found

***

### [PUT]/api/v1/message/{messageId}

- Summary  
Update message

- Description  
Updates a message with id. Requires role `update-maintenance-message`.

- Security  
bearerAuth  
basicAuth  

#### RequestBody

- application/json

```ts
{
  type: enum[error, warning, info]
  message: string
  link?: string
  link_text?: string
  display_start?: string
  display_end?: string
}
```

#### Responses

- 202 Updated message

`application/json`

```ts
{
  type: enum[error, warning, info]
  message: string
  link?: string
  link_text?: string
}
```

- 404 Could not find message

***

### [DELETE]/api/v1/message/{messageId}

- Summary  
Delete message

- Description  
Deletes a message with id. Requires role `delete-maintenance-message`.

- Security  
bearerAuth  
basicAuth  

#### Responses

- 202 Deleted message

`application/json`

- 404 Could not find message

***

### [GET]/api/v1/image/{imageId}

- Summary  
Find image

- Description  
Finds a container image in the metadata database.

#### Responses

- 200 Found image

`application/json`

```ts
{
  // The image id
  id: string
  // The image name
  name: string
  version: string
  operators: {
    // The operator id
    id: string
    // The machine-friendly name of the operator
    value: string
    // The documentation link
    documentation: string
    // The user-friendly name of the operator
    display_name: string
  }[]
  default: boolean
  data_types: {
    // The id of the data type
    id: string
    // The machine-friendly value of the data type
    value: string
    // The documentation link
    documentation: string
    // The human-friendly name of the data type
    display_name: string
    // The minimum size
    size_min?: integer
    // The maximum size
    size_max?: integer
    // The default size
    size_default?: integer
    // If true, the size parameter cannot be empty
    size_required?: boolean
    // The step increment
    size_step?: integer
    // The minimum d
    d_min?: integer
    // The maximum d
    d_max?: integer
    // The default d
    d_default?: integer
    // If true, the d parameter cannot be empty
    d_required?: boolean
    // The d increment
    d_step?: integer
    // The user-friendly description of the data format
    data_hint?: string
    // The user-friendly description of the data type
    type_hint?: string
    // frontend needs to quote this data type
    is_quoted: boolean
    // frontend can build this data type
    is_buildable: boolean
  }[]
}
```

- 404 Image could not be found

***

### [PUT]/api/v1/image/{imageId}

- Summary  
Update image

- Description  
Updates container image in the metadata database. Requires role `modify-image`.

- Security  
bearerAuth  
basicAuth  

#### RequestBody

- application/json

```ts
{
  // The URL of the registry without protocol
  registry: string
  // The SQL dialect class name
  dialect: string
  // The default image port to access the database
  default_port: integer
  // The driver class name
  driver_class: string
  // The method used by JDBC
  jdbc_method: string
}
```

#### Responses

- 202 Updated image successfully

`application/json`

```ts
{
  // The image id
  id: string
  // The image name
  name: string
  version: string
  operators: {
    // The operator id
    id: string
    // The machine-friendly name of the operator
    value: string
    // The documentation link
    documentation: string
    // The user-friendly name of the operator
    display_name: string
  }[]
  default: boolean
  data_types: {
    // The id of the data type
    id: string
    // The machine-friendly value of the data type
    value: string
    // The documentation link
    documentation: string
    // The human-friendly name of the data type
    display_name: string
    // The minimum size
    size_min?: integer
    // The maximum size
    size_max?: integer
    // The default size
    size_default?: integer
    // If true, the size parameter cannot be empty
    size_required?: boolean
    // The step increment
    size_step?: integer
    // The minimum d
    d_min?: integer
    // The maximum d
    d_max?: integer
    // The default d
    d_default?: integer
    // If true, the d parameter cannot be empty
    d_required?: boolean
    // The d increment
    d_step?: integer
    // The user-friendly description of the data format
    data_hint?: string
    // The user-friendly description of the data type
    type_hint?: string
    // frontend needs to quote this data type
    is_quoted: boolean
    // frontend can build this data type
    is_buildable: boolean
  }[]
}
```

- 404 Image could not be found

***

### [DELETE]/api/v1/image/{imageId}

- Summary  
Delete image

- Description  
Deletes a container image in the metadata database. Requires role `delete-image`.

- Security  
bearerAuth  
basicAuth  

#### Responses

- 202 Deleted image successfully

- 404 Image could not be found

***

### [GET]/api/v1/identifier/{identifierId}

- Summary  
Find identifier

- Description  
Finds an identifier with id. The response format depends on the HTTP `Accept` header set on the request.

#### Headers

```ts
Accept: string
```

#### Responses

- 200 Found identifier successfully

`application/json`

```ts
{
  id: string
  links: {
    self: string
    data?: string
    self_html: string
    dashboard_html?: string
  }
  type: enum[database, subset, table, view]
  titles: {
    id: string
    title?: string
    language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
    type?: enum[AlternativeTitle, Subtitle, TranslatedTitle, Other]
  }[]
  descriptions: {
    id: string
    description?: string
    language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
    type?: enum[Abstract, Methods, SeriesInformation, TableOfContents, TechnicalInfo, Other]
  }[]
  funders: {
    id: string
    funder_name: string
    funder_identifier?: string
    funder_identifier_type?: enum[Crossref Funder ID, ROR, GND, ISNI, Other]
    scheme_uri?: string
    award_number?: string
    award_title?: string
  }[]
  query: string
  execution?: string
  doi?: string
  publisher: string
  owner: {
    id?: string
    // Only contains lowercase characters
    username: string
    name?: string
    orcid?: string
    qualified_name?: string
    given_name?: string
    family_name?: string
  }
  language: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
  licenses: {
    // The id
    identifier: string
    // The link to the license such as an SPDX identifier
    uri: string
    // A user-friendly short abstract of key details of the license
    description?: string
  }[]
  creators: {
    id: string
    firstname?: string
    lastname?: string
    affiliation?: string
    creator_name: string
    name_type?: enum[Personal, Organizational]
    name_identifier?: string
    name_identifier_scheme?: enum[ORCID, ROR, ISNI, GRID]
    name_identifier_scheme_uri?: string
    affiliation_identifier?: string
    affiliation_identifier_scheme?: enum[ROR, GRID, ISNI]
    affiliation_identifier_scheme_uri?: string
  }[]
  status: enum[draft, published]
  created?: string
  database_id: string
  query_id?: string
  table_id?: string
  view_id?: string
  query_normalized: string
  related_identifiers: {
    id: string
    value: string
    type: enum[DOI, URL, URN, ARK, arXiv, bibcode, EAN13, EISSN, Handle, IGSN, ISBN, ISTC, LISSN, LSID, PMID, PURL, UPC, w3id]
    relation: enum[IsCitedBy, Cites, IsSupplementTo, IsSupplementedBy, IsContinuedBy, Continues, IsDescribedBy, Describes, HasMetadata, IsMetadataFor, HasVersion, IsVersionOf, IsNewVersionOf, IsPreviousVersionOf, IsPartOf, HasPart, IsPublishedIn, IsReferencedBy, References, IsDocumentedBy, Documents, IsCompiledBy, Compiles, IsVariantFormOf, IsOriginalFormOf, IsIdenticalTo, IsReviewedBy, Reviews, IsDerivedFrom, IsSourceOf, IsRequiredBy, Requires, IsObsoletedBy, Obsoletes]
  }[]
  // query hash in sha512
  query_hash: string
  result_hash?: string
  result_number?: integer
  publication_day?: integer
  publication_month?: integer
  publication_year: integer
}
```

`application/ld+json`

```ts
{
  // The name
  name: string
  // The description
  description: string
  // The URL
  url: string
  identifier?: string[]
  license?: string
  creator: {
    // The name
    name: string
    // The unambiguous reference to a person's identifier
    sameAs?: string
    // The firstname
    givenName?: string
    // The lastname
    familyName?: string
    // The type
    @type: string
  }[]
  // The ciration URL
  citation: string
  hasPart: {
    // The name
    name: string
    // The description
    description: string
    // The URL
    url: string
    identifier?: string[]
    license?: string
    creator:#/components/schemas/LdCreatorDto[]
    // The ciration URL
    citation: string
    hasPart:#/components/schemas/LdDatasetDto[]
    // The temporal coverage
    temporalCoverage: string
    // The version
    version: string
    // The context schema URI
    @context: string
    // The type
    @type: string
  }[]
  // The temporal coverage
  temporalCoverage: string
  // The version
  version: string
  // The context schema URI
  @context: string
  // The type
  @type: string
}
```

`text/xml`

`text/bibliography`

`text/bibliography; style=apa`

`text/bibliography; style=ieee`

`text/bibliography; style=bibtex`

- 400 Identifier could not be exported, the requested style is not known

- 403 Not allowed to view identifier

- 404 Identifier could not be found

- 406 Failed to find acceptable representation

- 409 Exported resource was not found

- 410 Failed to retrieve from S3 endpoint

- 502 Connection to data service failed

- 503 Failed to find in data service

***

### [PUT]/api/v1/identifier/{identifierId}

- Summary  
Save identifier

- Description  
Saves an identifier with id as a draft identifier. Identifiers can only be created for objects the user has at least *READ* access in the associated database (requires role `create-identifier`) or for any object in any database (requires role `create-foreign-identifier`).

- Security  
bearerAuth  
basicAuth  

#### RequestBody

- application/json

```ts
{
  id: string
  type: enum[database, subset, table, view]
  doi?: string
  titles: {
    id: string
    title: string
    language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
    type?: enum[AlternativeTitle, Subtitle, TranslatedTitle, Other]
  }[]
  descriptions: {
    id: string
    description: string
    language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
    type?: enum[Abstract, Methods, SeriesInformation, TableOfContents, TechnicalInfo, Other]
  }[]
  funders: {
    id: string
    funder_name: string
    funder_identifier?: string
    funder_identifier_type?: enum[Crossref Funder ID, ROR, GND, ISNI, Other]
    scheme_uri?: string
    award_number?: string
    award_title?: string
  }[]
  licenses: {
    // The id
    identifier: string
    // The link to the license such as an SPDX identifier
    uri: string
    // A user-friendly short abstract of key details of the license
    description?: string
  }[]
  publisher: string
  language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
  creators: {
    id: string
    firstname?: string
    lastname?: string
    affiliation?: string
    creator_name: string
    name_type?: enum[Personal, Organizational]
    name_identifier?: string
    affiliation_identifier?: string
  }[]
  database_id: string
  query_id?: string
  view_id?: string
  table_id?: string
  publication_day?: integer
  publication_month?: integer
  publication_year: integer
  related_identifiers: {
    id: string
    value: string
    type: enum[DOI, URL, URN, ARK, arXiv, bibcode, EAN13, EISSN, Handle, IGSN, ISBN, ISTC, LISSN, LSID, PMID, PURL, UPC, w3id]
    relation: enum[IsCitedBy, Cites, IsSupplementTo, IsSupplementedBy, IsContinuedBy, Continues, IsDescribedBy, Describes, HasMetadata, IsMetadataFor, HasVersion, IsVersionOf, IsNewVersionOf, IsPreviousVersionOf, IsPartOf, HasPart, IsPublishedIn, IsReferencedBy, References, IsDocumentedBy, Documents, IsCompiledBy, Compiles, IsVariantFormOf, IsOriginalFormOf, IsIdenticalTo, IsReviewedBy, Reviews, IsDerivedFrom, IsSourceOf, IsRequiredBy, Requires, IsObsoletedBy, Obsoletes]
  }[]
}
```

#### Responses

- 202 Saved identifier

`application/json`

```ts
{
  id: string
  links: {
    self: string
    data?: string
    self_html: string
    dashboard_html?: string
  }
  type: enum[database, subset, table, view]
  titles: {
    id: string
    title?: string
    language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
    type?: enum[AlternativeTitle, Subtitle, TranslatedTitle, Other]
  }[]
  descriptions: {
    id: string
    description?: string
    language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
    type?: enum[Abstract, Methods, SeriesInformation, TableOfContents, TechnicalInfo, Other]
  }[]
  funders: {
    id: string
    funder_name: string
    funder_identifier?: string
    funder_identifier_type?: enum[Crossref Funder ID, ROR, GND, ISNI, Other]
    scheme_uri?: string
    award_number?: string
    award_title?: string
  }[]
  query: string
  execution?: string
  doi?: string
  publisher: string
  owner: {
    id?: string
    // Only contains lowercase characters
    username: string
    name?: string
    orcid?: string
    qualified_name?: string
    given_name?: string
    family_name?: string
  }
  language: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
  licenses: {
    // The id
    identifier: string
    // The link to the license such as an SPDX identifier
    uri: string
    // A user-friendly short abstract of key details of the license
    description?: string
  }[]
  creators: {
    id: string
    firstname?: string
    lastname?: string
    affiliation?: string
    creator_name: string
    name_type?: enum[Personal, Organizational]
    name_identifier?: string
    name_identifier_scheme?: enum[ORCID, ROR, ISNI, GRID]
    name_identifier_scheme_uri?: string
    affiliation_identifier?: string
    affiliation_identifier_scheme?: enum[ROR, GRID, ISNI]
    affiliation_identifier_scheme_uri?: string
  }[]
  status: enum[draft, published]
  created?: string
  database_id: string
  query_id?: string
  table_id?: string
  view_id?: string
  query_normalized: string
  related_identifiers: {
    id: string
    value: string
    type: enum[DOI, URL, URN, ARK, arXiv, bibcode, EAN13, EISSN, Handle, IGSN, ISBN, ISTC, LISSN, LSID, PMID, PURL, UPC, w3id]
    relation: enum[IsCitedBy, Cites, IsSupplementTo, IsSupplementedBy, IsContinuedBy, Continues, IsDescribedBy, Describes, HasMetadata, IsMetadataFor, HasVersion, IsVersionOf, IsNewVersionOf, IsPreviousVersionOf, IsPartOf, HasPart, IsPublishedIn, IsReferencedBy, References, IsDocumentedBy, Documents, IsCompiledBy, Compiles, IsVariantFormOf, IsOriginalFormOf, IsIdenticalTo, IsReviewedBy, Reviews, IsDerivedFrom, IsSourceOf, IsRequiredBy, Requires, IsObsoletedBy, Obsoletes]
  }[]
  // query hash in sha512
  query_hash: string
  result_hash?: string
  result_number?: integer
  publication_day?: integer
  publication_month?: integer
  publication_year: integer
}
```

- 400 Identifier form contains invalid request data

- 403 Insufficient access rights or authorities

- 404 Failed to find database, table or view

- 502 Connection to search service failed

- 503 Failed to save in search service

***

### [DELETE]/api/v1/identifier/{identifierId}

- Summary  
Delete identifier

- Description  
Deletes an identifier with id. Requires role `delete-identifier`.

- Security  
bearerAuth  
basicAuth  

#### Responses

- 202 Deleted identifier

- 403 Deleting identifier not permitted

- 404 Identifier or database could not be found

- 502 Connection to search service failed

- 503 Failed to delete in search service

***

### [PUT]/api/v1/identifier/{identifierId}/publish

- Summary  
Publish identifier

- Description  
Publishes an identifier with id. A published identifier cannot be changed anymore. Requires role `publish-identifier`.

- Security  
bearerAuth  
basicAuth  

#### Responses

- 202 Published identifier

`application/json`

```ts
{
  id: string
  links: {
    self: string
    data?: string
    self_html: string
    dashboard_html?: string
  }
  type: enum[database, subset, table, view]
  titles: {
    id: string
    title?: string
    language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
    type?: enum[AlternativeTitle, Subtitle, TranslatedTitle, Other]
  }[]
  descriptions: {
    id: string
    description?: string
    language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
    type?: enum[Abstract, Methods, SeriesInformation, TableOfContents, TechnicalInfo, Other]
  }[]
  funders: {
    id: string
    funder_name: string
    funder_identifier?: string
    funder_identifier_type?: enum[Crossref Funder ID, ROR, GND, ISNI, Other]
    scheme_uri?: string
    award_number?: string
    award_title?: string
  }[]
  query: string
  execution?: string
  doi?: string
  publisher: string
  owner: {
    id?: string
    // Only contains lowercase characters
    username: string
    name?: string
    orcid?: string
    qualified_name?: string
    given_name?: string
    family_name?: string
  }
  language: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
  licenses: {
    // The id
    identifier: string
    // The link to the license such as an SPDX identifier
    uri: string
    // A user-friendly short abstract of key details of the license
    description?: string
  }[]
  creators: {
    id: string
    firstname?: string
    lastname?: string
    affiliation?: string
    creator_name: string
    name_type?: enum[Personal, Organizational]
    name_identifier?: string
    name_identifier_scheme?: enum[ORCID, ROR, ISNI, GRID]
    name_identifier_scheme_uri?: string
    affiliation_identifier?: string
    affiliation_identifier_scheme?: enum[ROR, GRID, ISNI]
    affiliation_identifier_scheme_uri?: string
  }[]
  status: enum[draft, published]
  created?: string
  database_id: string
  query_id?: string
  table_id?: string
  view_id?: string
  query_normalized: string
  related_identifiers: {
    id: string
    value: string
    type: enum[DOI, URL, URN, ARK, arXiv, bibcode, EAN13, EISSN, Handle, IGSN, ISBN, ISTC, LISSN, LSID, PMID, PURL, UPC, w3id]
    relation: enum[IsCitedBy, Cites, IsSupplementTo, IsSupplementedBy, IsContinuedBy, Continues, IsDescribedBy, Describes, HasMetadata, IsMetadataFor, HasVersion, IsVersionOf, IsNewVersionOf, IsPreviousVersionOf, IsPartOf, HasPart, IsPublishedIn, IsReferencedBy, References, IsDocumentedBy, Documents, IsCompiledBy, Compiles, IsVariantFormOf, IsOriginalFormOf, IsIdenticalTo, IsReviewedBy, Reviews, IsDerivedFrom, IsSourceOf, IsRequiredBy, Requires, IsObsoletedBy, Obsoletes]
  }[]
  // query hash in sha512
  query_hash: string
  result_hash?: string
  result_number?: integer
  publication_day?: integer
  publication_month?: integer
  publication_year: integer
}
```

- 400 Identifier form contains invalid request data

- 403 Insufficient access rights or authorities

- 404 Failed to find database, table or view

- 502 Connection to search service failed

- 503 Failed to save in search service

***

### [PUT]/api/v1/database/{databaseId}/visibility

- Summary  
Update database visibility

- Description  
Updates the database with id on the visibility. Only the database owner can perform this operation. Requires role `modify-database-visibility`.

- Security  
bearerAuth  
basicAuth  

#### RequestBody

- application/json

```ts
{
  // The visibility; if true, The will be displayed publicly and is searchable
  is_public: boolean
  // The insights; if true, The schema will be displayed publicly and is searchable
  is_schema_public: boolean
  // If true, the dashboard will be managed
  is_dashboard_enabled: boolean
}
```

#### Responses

- 202 Visibility modified successfully

`application/json`

```ts
{
  id: string
  // The name
  name: string
  // The comment
  description?: string
  identifiers: {
    id: string
    type: enum[database, subset, table, view]
    creators: {
      id: string
      affiliation?: string
      creator_name: string
      name_type?: enum[Personal, Organizational]
      name_identifier?: string
      name_identifier_scheme?: enum[ORCID, ROR, ISNI, GRID]
      affiliation_identifier?: string
      affiliation_identifier_scheme?: enum[ROR, GRID, ISNI]
    }[]
    titles: {
      id: string
      title?: string
      language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
      type?: enum[AlternativeTitle, Subtitle, TranslatedTitle, Other]
    }[]
    descriptions: {
      id: string
      description?: string
      language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
      type?: enum[Abstract, Methods, SeriesInformation, TableOfContents, TechnicalInfo, Other]
    }[]
    doi?: string
    publisher: string
    status: enum[draft, published]
    created?: string
    database_id: string
    query_id?: string
    table_id?: string
    view_id?: string
    publication_year: integer
    // The owner username
    owned_by: string
  }[]
  // The machine-friendly database name
  internal_name: string
  // The visibility; if true, The will be displayed publicly and is searchable
  is_public: boolean
  // The insights; if true, The schema will be displayed publicly and is searchable
  is_schema_public: boolean
  contact: {
    id?: string
    // Only contains lowercase characters
    username: string
    name?: string
    orcid?: string
    qualified_name?: string
    given_name?: string
    family_name?: string
  }
  // The owner username
  owned_by: string
  // The preview image
  preview_image?: string
}
```

- 400 The visibility payload is malformed

- 403 Visibility modification is not permitted

- 404 Failed to find database in metadata database

- 502 Connection to search service failed

- 503 Failed to save in search service

***

### [GET]/api/v1/database/{databaseId}/view/{viewId}

- Summary  
Get view

- Description  
Gets a view with id in the metadata database.

- Security  
bearerAuth  
basicAuth  

#### Responses

- 200 Find view successfully

`application/json`

```ts
{
  // The id
  id: string
  // The user-friendly name
  name: string
  identifiers: {
    id: string
    links: {
      self: string
      data?: string
      self_html: string
      dashboard_html?: string
    }
    type: enum[database, subset, table, view]
    titles: {
      id: string
      title?: string
      language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
      type?: enum[AlternativeTitle, Subtitle, TranslatedTitle, Other]
    }[]
    descriptions: {
      id: string
      description?: string
      language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
      type?: enum[Abstract, Methods, SeriesInformation, TableOfContents, TechnicalInfo, Other]
    }[]
    funders: {
      id: string
      funder_name: string
      funder_identifier?: string
      funder_identifier_type?: enum[Crossref Funder ID, ROR, GND, ISNI, Other]
      scheme_uri?: string
      award_number?: string
      award_title?: string
    }[]
    query: string
    execution?: string
    doi?: string
    publisher: string
    owner: {
      id?: string
      // Only contains lowercase characters
      username: string
      name?: string
      orcid?: string
      qualified_name?: string
      given_name?: string
      family_name?: string
    }
    language: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
    licenses: {
      // The id
      identifier: string
      // The link to the license such as an SPDX identifier
      uri: string
      // A user-friendly short abstract of key details of the license
      description?: string
    }[]
    creators: {
      id: string
      firstname?: string
      lastname?: string
      affiliation?: string
      creator_name: string
      name_type?: enum[Personal, Organizational]
      name_identifier?: string
      name_identifier_scheme?: enum[ORCID, ROR, ISNI, GRID]
      name_identifier_scheme_uri?: string
      affiliation_identifier?: string
      affiliation_identifier_scheme?: enum[ROR, GRID, ISNI]
      affiliation_identifier_scheme_uri?: string
    }[]
    status: enum[draft, published]
    created?: string
    database_id: string
    query_id?: string
    table_id?: string
    view_id?: string
    query_normalized: string
    related_identifiers: {
      id: string
      value: string
      type: enum[DOI, URL, URN, ARK, arXiv, bibcode, EAN13, EISSN, Handle, IGSN, ISBN, ISTC, LISSN, LSID, PMID, PURL, UPC, w3id]
      relation: enum[IsCitedBy, Cites, IsSupplementTo, IsSupplementedBy, IsContinuedBy, Continues, IsDescribedBy, Describes, HasMetadata, IsMetadataFor, HasVersion, IsVersionOf, IsNewVersionOf, IsPreviousVersionOf, IsPartOf, HasPart, IsPublishedIn, IsReferencedBy, References, IsDocumentedBy, Documents, IsCompiledBy, Compiles, IsVariantFormOf, IsOriginalFormOf, IsIdenticalTo, IsReviewedBy, Reviews, IsDerivedFrom, IsSourceOf, IsRequiredBy, Requires, IsObsoletedBy, Obsoletes]
    }[]
    // query hash in sha512
    query_hash: string
    result_hash?: string
    result_number?: integer
    publication_day?: integer
    publication_month?: integer
    publication_year: integer
  }[]
  // The SQL statement used to create the view
  query: string
  owner:#/components/schemas/UserBriefDto
  columns: {
    // The id
    id: string
    // The user-friendly column name
    name: string
    // The column size, determines the number of digits before the comma as x=size-d where size >= d
    size?: integer
    // The column d
    d?: integer
    description?: string
    enums: {
      // The enum id
      id: string
      // The enum value
      value: string
    }[]
    sets: {
      // The set id
      id: string
      // The set value
      value: string
    }[]
    // The database id
    database_id: string
    // The ordinal position of the colum to order it
    ord: integer
    // The machine-friendly column name
    internal_name: string
    // The length of the index
    index_length?: integer
    // The length of the total data in the table (index + data)
    length?: integer
    // The column type name
    type: enum[char, varchar, binary, varbinary, tinyblob, tinytext, text, blob, mediumtext, mediumblob, longtext, longblob, enum, set, serial, bit, tinyint, bool, smallint, mediumint, int, bigint, float, double, decimal, date, datetime, timestamp, time, year]
    is_null_allowed: boolean
  }[]
  // The created timestamp
  created: string
  // The database id
  database_id: string
  // The machine-friendly name
  internal_name: string
  // The visibility; if true, The will be displayed publicly and is searchable
  is_public: boolean
  // The insights; if true, The schema will be displayed publicly and is searchable
  is_schema_public: boolean
  // If true, the view is default for the database
  initial_view?: boolean
  // The sha256-hash of the query
  query_hash: string
}
```

- 403 Find view is not permitted

- 404 Database, view or user could not be found

***

### [PUT]/api/v1/database/{databaseId}/view/{viewId}

- Summary  
Update view

- Description  
Updates a view with id. This can only be performed by the view owner or database owner. Requires role `create-database-view`.

- Security  
bearerAuth  
basicAuth  

#### RequestBody

- application/json

```ts
{
  // The visibility; if true, The will be displayed publicly and is searchable
  is_public: boolean
  // The insights; if true, The schema will be displayed publicly and is searchable
  is_schema_public: boolean
}
```

#### Responses

- 202 Update view successfully

`*/*`

```ts
{
  // The id
  id: string
  // The user-friendly name
  name: string
  // The SQL statement used to create the view
  query: string
  // The database id
  database_id: string
  // The machine-friendly name
  internal_name: string
  // The visibility; if true, The will be displayed publicly and is searchable
  is_public: boolean
  // The insights; if true, The schema will be displayed publicly and is searchable
  is_schema_public: boolean
  // If true, the view is default for the database
  initial_view?: boolean
  // The sha256-hash of the query
  query_hash: string
  // The owner username
  owned_by?: string
}
```

- 400 Update view query is malformed

- 403 Update not allowed

- 404 Database or View could not be found

- 502 Connection to search service failed

- 503 Failed to save in search service

***

### [DELETE]/api/v1/database/{databaseId}/view/{viewId}

- Summary  
Delete view

- Description  
Deletes a view with id. Requires role `delete-database-view`.

- Security  
bearerAuth  
basicAuth  

#### Responses

- 202 Delete view successfully

- 400 Delete view query is malformed

- 403 Deletion not allowed

- 404 Database, view or user could not be found

- 423 Delete view resulted in an invalid query statement

- 502 Connection to search service failed

- 503 Failed to save in search service

***

### [GET]/api/v1/database/{databaseId}/table/{tableId}

- Summary  
Find table

- Description  
Finds a table with id. When a table is hidden (i.e. when `is_public` is `false`), then the user needs to have at least read access and the role `find-table`. When the `system` role is present, the endpoint responds with additional connection metadata in the header.

- Security  
bearerAuth  
basicAuth  

#### Responses

- 200 Find table successfully

`application/json`

```ts
{
  // The id
  id: string
  // The user-friendly name
  name: string
  // The comment
  description?: string
  // The alias
  alias?: string
  identifiers: {
    id: string
    links: {
      self: string
      data?: string
      self_html: string
      dashboard_html?: string
    }
    type: enum[database, subset, table, view]
    titles: {
      id: string
      title?: string
      language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
      type?: enum[AlternativeTitle, Subtitle, TranslatedTitle, Other]
    }[]
    descriptions: {
      id: string
      description?: string
      language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
      type?: enum[Abstract, Methods, SeriesInformation, TableOfContents, TechnicalInfo, Other]
    }[]
    funders: {
      id: string
      funder_name: string
      funder_identifier?: string
      funder_identifier_type?: enum[Crossref Funder ID, ROR, GND, ISNI, Other]
      scheme_uri?: string
      award_number?: string
      award_title?: string
    }[]
    query: string
    execution?: string
    doi?: string
    publisher: string
    owner: {
      id?: string
      // Only contains lowercase characters
      username: string
      name?: string
      orcid?: string
      qualified_name?: string
      given_name?: string
      family_name?: string
    }
    language: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
    licenses: {
      // The id
      identifier: string
      // The link to the license such as an SPDX identifier
      uri: string
      // A user-friendly short abstract of key details of the license
      description?: string
    }[]
    creators: {
      id: string
      firstname?: string
      lastname?: string
      affiliation?: string
      creator_name: string
      name_type?: enum[Personal, Organizational]
      name_identifier?: string
      name_identifier_scheme?: enum[ORCID, ROR, ISNI, GRID]
      name_identifier_scheme_uri?: string
      affiliation_identifier?: string
      affiliation_identifier_scheme?: enum[ROR, GRID, ISNI]
      affiliation_identifier_scheme_uri?: string
    }[]
    status: enum[draft, published]
    created?: string
    database_id: string
    query_id?: string
    table_id?: string
    view_id?: string
    query_normalized: string
    related_identifiers: {
      id: string
      value: string
      type: enum[DOI, URL, URN, ARK, arXiv, bibcode, EAN13, EISSN, Handle, IGSN, ISBN, ISTC, LISSN, LSID, PMID, PURL, UPC, w3id]
      relation: enum[IsCitedBy, Cites, IsSupplementTo, IsSupplementedBy, IsContinuedBy, Continues, IsDescribedBy, Describes, HasMetadata, IsMetadataFor, HasVersion, IsVersionOf, IsNewVersionOf, IsPreviousVersionOf, IsPartOf, HasPart, IsPublishedIn, IsReferencedBy, References, IsDocumentedBy, Documents, IsCompiledBy, Compiles, IsVariantFormOf, IsOriginalFormOf, IsIdenticalTo, IsReviewedBy, Reviews, IsDerivedFrom, IsSourceOf, IsRequiredBy, Requires, IsObsoletedBy, Obsoletes]
    }[]
    // query hash in sha512
    query_hash: string
    result_hash?: string
    result_number?: integer
    publication_day?: integer
    publication_month?: integer
    publication_year: integer
  }[]
  owner:#/components/schemas/UserBriefDto
  columns: {
    // The id
    id: string
    // The user-friendly column name
    name: string
    // The data source alias name
    alias?: string
    // The column size, determines the number of digits before the comma as x=size-d where size >= d
    size?: integer
    // The column d
    d?: integer
    // The statistically average numerical value
    mean?: number
    // The statistically most middle numerical value
    median?: number
    description?: string
    enums: {
      // The enum id
      id: string
      // The enum value
      value: string
    }[]
    sets: {
      // The set id
      id: string
      // The set value
      value: string
    }[]
    // The database id
    database_id: string
    // The table id
    table_id: string
    // The ordinal position of the colum to order it
    ord: integer
    // The machine-friendly column name
    internal_name: string
    // The length of the index
    index_length?: integer
    // The length of the total data in the table (index + data)
    length?: integer
    // The column type name
    type: enum[char, varchar, binary, varbinary, tinyblob, tinytext, text, blob, mediumtext, mediumblob, longtext, longblob, enum, set, serial, bit, tinyint, bool, smallint, mediumint, int, bigint, float, double, decimal, date, datetime, timestamp, time, year]
    // The data length
    data_length?: integer
    // The maximum data length
    max_data_length?: integer
    // The number of rows
    num_rows?: integer
    // The statistically highest numerical value
    val_min?: number
    // The statistically lowest numerical value
    val_max?: number
    // The statistically determined standard deviation
    std_dev?: number
    // The concept URI
    concept_uri?: string
    // The unit URI
    unit_uri?: string
    is_null_allowed: boolean
  }[]
  constraints: {
    uniques: {
      // The unique key id
      id: string
      // The unique key name
      name: string
      table: {
        // The id
        id: string
        // The user-friendly table name
        name: string
        // The comment
        description?: string
        // The database id
        database_id: string
        // The machine-friendly table name
        internal_name: string
        // If true, The is using data versioning
        is_versioned: boolean
        // The visibility; if true, The will be displayed publicly and is searchable
        is_public: boolean
        // The insights; if true, The schema will be displayed publicly and is searchable
        is_schema_public: boolean
        // The owner username
        owned_by: string
      }
      columns: {
        // The column id
        id: string
        // The column name
        name: string
        // The data source alias name
        alias?: string
        // The database id
        database_id: string
        // The table id
        table_id: string
        // The machine-friendly internal name
        internal_name: string
        // The column type name
        type: enum[char, varchar, binary, varbinary, tinyblob, tinytext, text, blob, mediumtext, mediumblob, longtext, longblob, enum, set, serial, bit, tinyint, bool, smallint, mediumint, int, bigint, float, double, decimal, date, datetime, timestamp, time, year]
      }[]
    }[]
    checks?: string[]
    foreign_keys: {
      // The foreign key id
      id?: string
      // The foreign key name
      name: string
      references: {
        // The foreign key reference id
        id?: string
        column:#/components/schemas/ColumnBriefDto
        foreign_key: {
          // The foreign key id
          id?: string
        }
        referenced_column:#/components/schemas/ColumnBriefDto
      }[]
      table:#/components/schemas/TableBriefDto
      referenced_table:#/components/schemas/TableBriefDto
      // The integrity action when updating tuples
      on_update?: enum[restrict, cascade, set_null, no_action, set_default]
      // The integrity action when deleting tuples
      on_delete?: enum[restrict, cascade, set_null, no_action, set_default]
    }[]
    primary_key: {
      // The primary key id
      id?: string
      table:#/components/schemas/TableBriefDto
      column:#/components/schemas/ColumnBriefDto
    }[]
  }
  // The created timestamp
  created: string
  // The database id
  database_id: string
  // The machine-friendly name
  internal_name: string
  // If true, The is using data versioning
  is_versioned: boolean
  // The queue name
  queue_name: string
  // The queue type
  queue_type?: string
  // The routing key
  routing_key: string
  // The visibility; if true, The will be displayed publicly and is searchable
  is_public: boolean
  // The insights; if true, The schema will be displayed publicly and is searchable
  is_schema_public: boolean
  // The statistical number of rows
  num_rows?: integer
  // The data length in bytes
  data_length?: integer
  // The maximum data length in bytes
  max_data_length?: integer
  // The average row length in bytes
  avg_row_length?: integer
}
```

- 403 Access to the database is forbidden

- 404 Table, database or container could not be found

***

### [PUT]/api/v1/database/{databaseId}/table/{tableId}

- Summary  
Update table

- Description  
Updates a table in the database with id. Requires role `update-table` or `system`.

- Security  
bearerAuth  
basicAuth  

#### RequestBody

- application/json

```ts
{
  // The comment
  description?: string
  // The visibility; if true, The will be displayed publicly and is searchable
  is_public: boolean
  // The insights; if true, The schema will be displayed publicly and is searchable
  is_schema_public: boolean
}
```

#### Responses

- 202 Updated the table

`application/json`

```ts
{
  // The id
  id: string
  // The user-friendly table name
  name: string
  // The comment
  description?: string
  // The database id
  database_id: string
  // The machine-friendly table name
  internal_name: string
  // If true, The is using data versioning
  is_versioned: boolean
  // The visibility; if true, The will be displayed publicly and is searchable
  is_public: boolean
  // The insights; if true, The schema will be displayed publicly and is searchable
  is_schema_public: boolean
  // The owner username
  owned_by: string
}
```

- 400 Update table visibility payload is malformed

- 403 Update table visibility not permitted

- 404 Table could not be found

- 502 Connection to search service failed

- 503 Failed to save in search service

***

### [DELETE]/api/v1/database/{databaseId}/table/{tableId}

- Summary  
Delete table

- Description  
Deletes a table with id. Only the owner of a table can perform this action (requires role `delete-table`) or anyone can delete a table (requires role `delete-foreign-table`).

- Security  
bearerAuth  
basicAuth  

#### Responses

- 202 Delete table successfully

- 400 Delete table query resulted in an invalid query statement

- 403 Access to the database is forbidden

- 404 Table, database or container could not be found

- 502 Connection to search service failed

- 503 Failed to save in search service

***

### [PUT]/api/v1/database/{databaseId}/table/{tableId}/statistic

- Summary  
Update statistics

- Description  
Updates basic statistical properties (min, max, mean, median, std.dev) for numerical columns in a table with id. This action can only be performed by the table owner. Requires role `update-table-statistic`.

- Security  
bearerAuth  
basicAuth  

#### Responses

- 202 Updated table statistics successfully

- 400 Failed to map column statistic to known columns

- 403 Not the owner

- 404 Failed to find database/table in metadata database

- 502 Connection to search service failed

- 503 Failed to save in search service

***

### [PUT]/api/v1/database/{databaseId}/table/{tableId}/column/{columnId}

- Summary  
Update semantics

- Description  
Updates column semantics of a table column with id. Only the table owner with at least *READ* access to the associated database can update the column semantics (requires role `modify-table-column-semantics`) or foreign table columns if role `modify-foreign-table-column-semantics`.

- Security  
bearerAuth  
basicAuth  

#### RequestBody

- application/json

```ts
{
  // The description
  description?: string
  // The URI of the concept
  concept_uri?: string
  // The URI of the unit
  unit_uri?: string
}
```

#### Responses

- 202 Updated column semantics successfully

`application/json`

```ts
{
  // The id
  id: string
  // The user-friendly column name
  name: string
  // The data source alias name
  alias?: string
  // The column size, determines the number of digits before the comma as x=size-d where size >= d
  size?: integer
  // The column d
  d?: integer
  // The statistically average numerical value
  mean?: number
  // The statistically most middle numerical value
  median?: number
  description?: string
  enums: {
    // The enum id
    id: string
    // The enum value
    value: string
  }[]
  sets: {
    // The set id
    id: string
    // The set value
    value: string
  }[]
  // The database id
  database_id: string
  // The table id
  table_id: string
  // The ordinal position of the colum to order it
  ord: integer
  // The machine-friendly column name
  internal_name: string
  // The length of the index
  index_length?: integer
  // The length of the total data in the table (index + data)
  length?: integer
  // The column type name
  type: enum[char, varchar, binary, varbinary, tinyblob, tinytext, text, blob, mediumtext, mediumblob, longtext, longblob, enum, set, serial, bit, tinyint, bool, smallint, mediumint, int, bigint, float, double, decimal, date, datetime, timestamp, time, year]
  // The data length
  data_length?: integer
  // The maximum data length
  max_data_length?: integer
  // The number of rows
  num_rows?: integer
  // The statistically highest numerical value
  val_min?: number
  // The statistically lowest numerical value
  val_max?: number
  // The statistically determined standard deviation
  std_dev?: number
  // The concept URI
  concept_uri?: string
  // The unit URI
  unit_uri?: string
  is_null_allowed: boolean
}
```

- 400 Update semantic concept query is malformed or update unit of measurement query is malformed

- 403 Access to the database is forbidden

- 404 Failed to find user/table/database/ontology in metadata database

- 502 Connection to search service failed

- 503 Failed to save in search service

***

### [PUT]/api/v1/database/{databaseId}/owner

- Summary  
Update database owner

- Description  
Updates the database with id on the owner. Only the database owner can perform this operation. Requires role `modify-database-owner`.

- Security  
bearerAuth  
basicAuth  

#### RequestBody

- application/json

```ts
{
  // The username of the new owner
  username: string
}
```

#### Responses

- 202 Transfer of ownership was successful

`application/json`

```ts
{
  id: string
  // The name
  name: string
  // The comment
  description?: string
  identifiers: {
    id: string
    type: enum[database, subset, table, view]
    creators: {
      id: string
      affiliation?: string
      creator_name: string
      name_type?: enum[Personal, Organizational]
      name_identifier?: string
      name_identifier_scheme?: enum[ORCID, ROR, ISNI, GRID]
      affiliation_identifier?: string
      affiliation_identifier_scheme?: enum[ROR, GRID, ISNI]
    }[]
    titles: {
      id: string
      title?: string
      language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
      type?: enum[AlternativeTitle, Subtitle, TranslatedTitle, Other]
    }[]
    descriptions: {
      id: string
      description?: string
      language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
      type?: enum[Abstract, Methods, SeriesInformation, TableOfContents, TechnicalInfo, Other]
    }[]
    doi?: string
    publisher: string
    status: enum[draft, published]
    created?: string
    database_id: string
    query_id?: string
    table_id?: string
    view_id?: string
    publication_year: integer
    // The owner username
    owned_by: string
  }[]
  // The machine-friendly database name
  internal_name: string
  // The visibility; if true, The will be displayed publicly and is searchable
  is_public: boolean
  // The insights; if true, The schema will be displayed publicly and is searchable
  is_schema_public: boolean
  contact: {
    id?: string
    // Only contains lowercase characters
    username: string
    name?: string
    orcid?: string
    qualified_name?: string
    given_name?: string
    family_name?: string
  }
  // The owner username
  owned_by: string
  // The preview image
  preview_image?: string
}
```

- 400 Owner payload is malformed

- 403 Transfer of ownership is not permitted

- 404 Database could not be found

- 502 Connection to search service failed

- 503 Failed to save in search service

***

### [PUT]/api/v1/database/{databaseId}/metadata/view

- Summary  
Update database view schemas

- Description  
Updates the database with id with generated metadata from view that are not yet known to the database. Only the database owner can perform this operation. Requires role `find-database`.

- Security  
bearerAuth  
basicAuth  

#### Responses

- 200 Refreshed database views metadata

`application/json`

```ts
{
  id: string
  // The name
  name: string
  // The comment
  description?: string
  identifiers: {
    id: string
    type: enum[database, subset, table, view]
    creators: {
      id: string
      affiliation?: string
      creator_name: string
      name_type?: enum[Personal, Organizational]
      name_identifier?: string
      name_identifier_scheme?: enum[ORCID, ROR, ISNI, GRID]
      affiliation_identifier?: string
      affiliation_identifier_scheme?: enum[ROR, GRID, ISNI]
    }[]
    titles: {
      id: string
      title?: string
      language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
      type?: enum[AlternativeTitle, Subtitle, TranslatedTitle, Other]
    }[]
    descriptions: {
      id: string
      description?: string
      language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
      type?: enum[Abstract, Methods, SeriesInformation, TableOfContents, TechnicalInfo, Other]
    }[]
    doi?: string
    publisher: string
    status: enum[draft, published]
    created?: string
    database_id: string
    query_id?: string
    table_id?: string
    view_id?: string
    publication_year: integer
    // The owner username
    owned_by: string
  }[]
  // The machine-friendly database name
  internal_name: string
  // The visibility; if true, The will be displayed publicly and is searchable
  is_public: boolean
  // The insights; if true, The schema will be displayed publicly and is searchable
  is_schema_public: boolean
  contact: {
    id?: string
    // Only contains lowercase characters
    username: string
    name?: string
    orcid?: string
    qualified_name?: string
    given_name?: string
    family_name?: string
  }
  // The owner username
  owned_by: string
  // The preview image
  preview_image?: string
}
```

- 403 Refresh view metadata is not permitted

- 404 Failed to find database in metadata database

- 502 Connection to search service failed

- 503 Failed to save in search service

***

### [PUT]/api/v1/database/{databaseId}/metadata/table

- Summary  
Update database table schemas

- Description  
Updates the database with id with generated metadata from tables that are not yet known to the database. Only the database owner can perform this operation. Requires role `find-database`.

- Security  
bearerAuth  
basicAuth  

#### Responses

- 200 Refreshed database tables metadata

`application/json`

```ts
{
  id: string
  // The name
  name: string
  // The comment
  description?: string
  identifiers: {
    id: string
    type: enum[database, subset, table, view]
    creators: {
      id: string
      affiliation?: string
      creator_name: string
      name_type?: enum[Personal, Organizational]
      name_identifier?: string
      name_identifier_scheme?: enum[ORCID, ROR, ISNI, GRID]
      affiliation_identifier?: string
      affiliation_identifier_scheme?: enum[ROR, GRID, ISNI]
    }[]
    titles: {
      id: string
      title?: string
      language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
      type?: enum[AlternativeTitle, Subtitle, TranslatedTitle, Other]
    }[]
    descriptions: {
      id: string
      description?: string
      language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
      type?: enum[Abstract, Methods, SeriesInformation, TableOfContents, TechnicalInfo, Other]
    }[]
    doi?: string
    publisher: string
    status: enum[draft, published]
    created?: string
    database_id: string
    query_id?: string
    table_id?: string
    view_id?: string
    publication_year: integer
    // The owner username
    owned_by: string
  }[]
  // The machine-friendly database name
  internal_name: string
  // The visibility; if true, The will be displayed publicly and is searchable
  is_public: boolean
  // The insights; if true, The schema will be displayed publicly and is searchable
  is_schema_public: boolean
  contact: {
    id?: string
    // Only contains lowercase characters
    username: string
    name?: string
    orcid?: string
    qualified_name?: string
    given_name?: string
    family_name?: string
  }
  // The owner username
  owned_by: string
  // The preview image
  preview_image?: string
}
```

- 400 Failed to parse payload at search service

- 403 Not allowed to refresh table metadata

- 404 Failed to find database in metadata database

- 502 Connection to search service failed

- 503 Failed to save in search service

***

### [GET]/api/v1/database/{databaseId}/image

- Summary  
Get database preview image

- Description  
Gets the database with id on the preview image.

- Security  
bearerAuth  
basicAuth  

#### Responses

- 200 View of image was successful

`*/*`

```ts
{
  "type": "string",
  "format": "byte"
}
```

- 404 Database or user could not be found

***

### [PUT]/api/v1/database/{databaseId}/image

- Summary  
Update database preview image

- Description  
Updates the database with id on the preview image. Only the database owner can perform this operation. Requires role `modify-database-image`.

- Security  
bearerAuth  
basicAuth  

#### RequestBody

- application/json

```ts
{
  // The key of the S3 binary object in the storage service
  key?: string
}
```

#### Responses

- 202 Modify of image was successful

`application/json`

```ts
{
  id: string
  // The name
  name: string
  // The comment
  description?: string
  identifiers: {
    id: string
    type: enum[database, subset, table, view]
    creators: {
      id: string
      affiliation?: string
      creator_name: string
      name_type?: enum[Personal, Organizational]
      name_identifier?: string
      name_identifier_scheme?: enum[ORCID, ROR, ISNI, GRID]
      affiliation_identifier?: string
      affiliation_identifier_scheme?: enum[ROR, GRID, ISNI]
    }[]
    titles: {
      id: string
      title?: string
      language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
      type?: enum[AlternativeTitle, Subtitle, TranslatedTitle, Other]
    }[]
    descriptions: {
      id: string
      description?: string
      language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
      type?: enum[Abstract, Methods, SeriesInformation, TableOfContents, TechnicalInfo, Other]
    }[]
    doi?: string
    publisher: string
    status: enum[draft, published]
    created?: string
    database_id: string
    query_id?: string
    table_id?: string
    view_id?: string
    publication_year: integer
    // The owner username
    owned_by: string
  }[]
  // The machine-friendly database name
  internal_name: string
  // The visibility; if true, The will be displayed publicly and is searchable
  is_public: boolean
  // The insights; if true, The schema will be displayed publicly and is searchable
  is_schema_public: boolean
  contact: {
    id?: string
    // Only contains lowercase characters
    username: string
    name?: string
    orcid?: string
    qualified_name?: string
    given_name?: string
    family_name?: string
  }
  // The owner username
  owned_by: string
  // The preview image
  preview_image?: string
}
```

- 403 Modify of image is not permitted

- 404 Database could not be found

- 410 File was not found in the Storage Service

- 502 Connection to search service failed

- 503 Failed to save in search service

***

### [GET]/api/v1/message

- Summary  
List messages

- Description  
Lists messages known to the metadata database. Messages can be filtered be filtered with the optional `active` parameter. If set to *true*, only active messages (that is, messages whose end time has not been reached) will be returned. Otherwise only inactive messages are returned. If not set, active and inactive messages are returned.

#### Parameters(Query)

```ts
active?: boolean
```

#### Responses

- 200 List messages

`application/json`

```ts
{
  id: string
  type: enum[error, warning, info]
  message: string
  link?: string
  link_text?: string
  display_start?: string
  display_end?: string
}[]
```

***

### [POST]/api/v1/message

- Summary  
Create message

- Description  
Creates a message in the metadata database. Requires role `create-maintenance-message`.

- Security  
bearerAuth  
basicAuth  

#### RequestBody

- application/json

```ts
{
  type: enum[error, warning, info]
  message: string
  link?: string
  link_text?: string
  display_start?: string
  display_end?: string
}
```

#### Responses

- 201 Created message

`application/json`

```ts
{
  type: enum[error, warning, info]
  message: string
  link?: string
  link_text?: string
}
```

***

### [GET]/api/v1/image

- Summary  
List images

- Description  
Lists all container images known to the metadata database.

#### Responses

- 200 List images

`application/json`

```ts
{
  // The image id
  id: string
  // The image name
  name: string
  // The image tag
  version: string
  // Marks the image as default
  default: boolean
}[]
```

***

### [POST]/api/v1/image

- Summary  
Create image

- Description  
Creates a container image in the metadata database. Requires role `create-image`.

- Security  
bearerAuth  
basicAuth  

#### RequestBody

- application/json

```ts
{
  // The URL of the registry without protocol
  registry: string
  // The image name
  name: string
  version: string
  // The SQL dialect class name
  dialect: string
  // The default image port to access the database
  default_port: integer
  // The driver class name
  driver_class: string
  // The method used by JDBC
  jdbc_method: string
}
```

#### Responses

- 201 Created image

`application/json`

```ts
{
  // The image id
  id: string
  // The image name
  name: string
  version: string
  operators: {
    // The operator id
    id: string
    // The machine-friendly name of the operator
    value: string
    // The documentation link
    documentation: string
    // The user-friendly name of the operator
    display_name: string
  }[]
  default: boolean
  data_types: {
    // The id of the data type
    id: string
    // The machine-friendly value of the data type
    value: string
    // The documentation link
    documentation: string
    // The human-friendly name of the data type
    display_name: string
    // The minimum size
    size_min?: integer
    // The maximum size
    size_max?: integer
    // The default size
    size_default?: integer
    // If true, the size parameter cannot be empty
    size_required?: boolean
    // The step increment
    size_step?: integer
    // The minimum d
    d_min?: integer
    // The maximum d
    d_max?: integer
    // The default d
    d_default?: integer
    // If true, the d parameter cannot be empty
    d_required?: boolean
    // The d increment
    d_step?: integer
    // The user-friendly description of the data format
    data_hint?: string
    // The user-friendly description of the data type
    type_hint?: string
    // frontend needs to quote this data type
    is_quoted: boolean
    // frontend can build this data type
    is_buildable: boolean
  }[]
}
```

- 400 Image specification is invalid

- 409 Image already exists

***

### [GET]/api/v1/identifier

- Summary  
List identifiers

- Description  
Lists all identifiers known to the metadata database

#### Parameters(Query)

```ts
type?: enum[database, subset, table, view]
```

```ts
status?: enum[draft, published]
```

```ts
dbid?: string
```

```ts
qid?: string
```

```ts
vid?: string
```

```ts
tid?: string
```

#### Headers

```ts
Accept: string
```

#### Responses

- 200 Found identifiers successfully

`application/json`

```ts
{
  id: string
  type: enum[database, subset, table, view]
  creators: {
    id: string
    affiliation?: string
    creator_name: string
    name_type?: enum[Personal, Organizational]
    name_identifier?: string
    name_identifier_scheme?: enum[ORCID, ROR, ISNI, GRID]
    affiliation_identifier?: string
    affiliation_identifier_scheme?: enum[ROR, GRID, ISNI]
  }[]
  titles: {
    id: string
    title?: string
    language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
    type?: enum[AlternativeTitle, Subtitle, TranslatedTitle, Other]
  }[]
  descriptions: {
    id: string
    description?: string
    language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
    type?: enum[Abstract, Methods, SeriesInformation, TableOfContents, TechnicalInfo, Other]
  }[]
  doi?: string
  publisher: string
  status: enum[draft, published]
  created?: string
  database_id: string
  query_id?: string
  table_id?: string
  view_id?: string
  publication_year: integer
  // The owner username
  owned_by: string
}[][]
```

`application/ld+json`

```ts
{
  // The name
  name: string
  // The description
  description: string
  // The URL
  url: string
  identifier?: string[]
  license?: string
  creator: {
    // The name
    name: string
    // The unambiguous reference to a person's identifier
    sameAs?: string
    // The firstname
    givenName?: string
    // The lastname
    familyName?: string
    // The type
    @type: string
  }[]
  // The ciration URL
  citation: string
  hasPart:#/components/schemas/LdDatasetDto[]
  // The temporal coverage
  temporalCoverage: string
  // The version
  version: string
  // The context schema URI
  @context: string
  // The type
  @type: string
}[]
```

***

### [POST]/api/v1/identifier

- Summary  
Create identifier

- Description  
Create an identifier with id to create a draft identifier. Identifiers can only be created for objects the user has at least *READ* access in the associated database (requires role `create-identifier`) or for any object in any database (requires role `create-foreign-identifier`).

- Security  
bearerAuth  
basicAuth  

#### RequestBody

- application/json

```ts
{
  // The identifier type
  type: enum[database, subset, table, view]
  // The doi persistent identifier with optional https://doi.org/ prefix
  doi?: string
  titles: {
    // The title
    title: string
    // The language
    language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
    // The type
    type?: enum[AlternativeTitle, Subtitle, TranslatedTitle, Other]
  }[]
  descriptions: {
    // The description value
    description: string
    // The language
    language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
    // The type
    type?: enum[Abstract, Methods, SeriesInformation, TableOfContents, TechnicalInfo, Other]
  }[]
  funders: {
    // The funder name
    funder_name: string
    // The identifier that identifies the funder unambiguously
    funder_identifier?: string
    // The funder type, when the `funder_identifier` is a DOI, the `funder_identifier_type` field must be `Crossref Funder ID`
    funder_identifier_type?: enum[Crossref Funder ID, ROR, GND, ISNI, Other]
    // The scheme URI of the `funder_identifier`
    scheme_uri?: string
    // The award number
    award_number?: string
    // The award title
    award_title?: string
  }[]
  licenses: {
    // The id
    identifier: string
    // The link to the license such as an SPDX identifier
    uri: string
    // A user-friendly short abstract of key details of the license
    description?: string
  }[]
  // The publisher
  publisher: string
  // The language
  language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
  creators: {
    // The given name
    firstname?: string
    // The family name
    lastname?: string
    // The affiliation
    affiliation?: string
    // The full name
    creator_name: string
    // The name type
    name_type?: enum[Personal, Organizational]
    // The persistent identifier that identifies the creator unambiguously
    name_identifier?: string
    // The persistent identifier that identifies the affiliation unambiguously
    affiliation_identifier?: string
  }[]
  // The id
  database_id: string
  // The subset id, is only set when type=`subset`
  query_id?: string
  // The view id, is only set when type=`view`
  view_id?: string
  // The table id, is only set when type=`table`
  table_id?: string
  // The day of publication
  publication_day?: integer
  // The month of publication
  publication_month?: integer
  // The year of publication
  publication_year: integer
  related_identifiers: {
    value: string
    type: enum[DOI, URL, URN, ARK, arXiv, bibcode, EAN13, EISSN, Handle, IGSN, ISBN, ISTC, LISSN, LSID, PMID, PURL, UPC, w3id]
    relation: enum[IsCitedBy, Cites, IsSupplementTo, IsSupplementedBy, IsContinuedBy, Continues, IsDescribedBy, Describes, HasMetadata, IsMetadataFor, HasVersion, IsVersionOf, IsNewVersionOf, IsPreviousVersionOf, IsPartOf, HasPart, IsPublishedIn, IsReferencedBy, References, IsDocumentedBy, Documents, IsCompiledBy, Compiles, IsVariantFormOf, IsOriginalFormOf, IsIdenticalTo, IsReviewedBy, Reviews, IsDerivedFrom, IsSourceOf, IsRequiredBy, Requires, IsObsoletedBy, Obsoletes]
  }[]
}
```

#### Responses

- 201 Drafted identifier

`application/json`

```ts
{
  id: string
  links: {
    self: string
    data?: string
    self_html: string
    dashboard_html?: string
  }
  type: enum[database, subset, table, view]
  titles: {
    id: string
    title?: string
    language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
    type?: enum[AlternativeTitle, Subtitle, TranslatedTitle, Other]
  }[]
  descriptions: {
    id: string
    description?: string
    language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
    type?: enum[Abstract, Methods, SeriesInformation, TableOfContents, TechnicalInfo, Other]
  }[]
  funders: {
    id: string
    funder_name: string
    funder_identifier?: string
    funder_identifier_type?: enum[Crossref Funder ID, ROR, GND, ISNI, Other]
    scheme_uri?: string
    award_number?: string
    award_title?: string
  }[]
  query: string
  execution?: string
  doi?: string
  publisher: string
  owner: {
    id?: string
    // Only contains lowercase characters
    username: string
    name?: string
    orcid?: string
    qualified_name?: string
    given_name?: string
    family_name?: string
  }
  language: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
  licenses: {
    // The id
    identifier: string
    // The link to the license such as an SPDX identifier
    uri: string
    // A user-friendly short abstract of key details of the license
    description?: string
  }[]
  creators: {
    id: string
    firstname?: string
    lastname?: string
    affiliation?: string
    creator_name: string
    name_type?: enum[Personal, Organizational]
    name_identifier?: string
    name_identifier_scheme?: enum[ORCID, ROR, ISNI, GRID]
    name_identifier_scheme_uri?: string
    affiliation_identifier?: string
    affiliation_identifier_scheme?: enum[ROR, GRID, ISNI]
    affiliation_identifier_scheme_uri?: string
  }[]
  status: enum[draft, published]
  created?: string
  database_id: string
  query_id?: string
  table_id?: string
  view_id?: string
  query_normalized: string
  related_identifiers: {
    id: string
    value: string
    type: enum[DOI, URL, URN, ARK, arXiv, bibcode, EAN13, EISSN, Handle, IGSN, ISBN, ISTC, LISSN, LSID, PMID, PURL, UPC, w3id]
    relation: enum[IsCitedBy, Cites, IsSupplementTo, IsSupplementedBy, IsContinuedBy, Continues, IsDescribedBy, Describes, HasMetadata, IsMetadataFor, HasVersion, IsVersionOf, IsNewVersionOf, IsPreviousVersionOf, IsPartOf, HasPart, IsPublishedIn, IsReferencedBy, References, IsDocumentedBy, Documents, IsCompiledBy, Compiles, IsVariantFormOf, IsOriginalFormOf, IsIdenticalTo, IsReviewedBy, Reviews, IsDerivedFrom, IsSourceOf, IsRequiredBy, Requires, IsObsoletedBy, Obsoletes]
  }[]
  // query hash in sha512
  query_hash: string
  result_hash?: string
  result_number?: integer
  publication_day?: integer
  publication_month?: integer
  publication_year: integer
}
```

- 400 Identifier form contains invalid request data

- 403 Insufficient access rights or authorities

- 404 Failed to find database, table or view

- 502 Connection to search service failed

- 503 Failed to save in search service

***

### [GET]/api/v1/database/{databaseId}/view

- Summary  
List views

- Description  
Lists views known to the metadata database.

- Security  
bearerAuth  
basicAuth  

#### Responses

- 200 Find views successfully

`application/json`

```ts
{
  // The id
  id: string
  // The user-friendly name
  name: string
  // The SQL statement used to create the view
  query: string
  // The database id
  database_id: string
  // The machine-friendly name
  internal_name: string
  // The visibility; if true, The will be displayed publicly and is searchable
  is_public: boolean
  // The insights; if true, The schema will be displayed publicly and is searchable
  is_schema_public: boolean
  // If true, the view is default for the database
  initial_view?: boolean
  // The sha256-hash of the query
  query_hash: string
  // The owner username
  owned_by?: string
}[]
```

- 404 Database or user could not be found

***

### [POST]/api/v1/database/{databaseId}/view

- Summary  
Create view

- Description  
Creates a view. This can only be performed by the database owner. Requires role `create-database-view`.

- Security  
bearerAuth  
basicAuth  

#### RequestBody

- application/json

```ts
{
  // The name
  name: string
  query: {
    columns: {
      // The id of the column
      id: string
      // The column alias
      alias?: string
    }[]
    joins: {
      // The type of join
      type: enum[inner, left, right, cross]
      conditionals: {
        // The id of the column
        column_id: string
        // The id of the foreign column
        foreign_column_id: string
      }[]
      // The id of the data source
      datasource_id: string
    }[]
    filters: {
      // The filter type
      type: enum[where, or, and]
      // The filter value
      value?: string
      // The column id
      column_id: string
      // The operator id
      operator_id: string
    }[]
    orders: {
      // The sort direction
      direction?: enum[asc, desc]
      // The column id
      column_id: string
    }[]
    datasource_ids?: string[]
  }
  // The visibility; if true, The will be displayed publicly and is searchable
  is_public: boolean
  // The insights; if true, The schema will be displayed publicly and is searchable
  is_schema_public: boolean
}
```

#### Responses

- 201 Create view successfully

`application/json`

```ts
{
  // The id
  id: string
  // The user-friendly name
  name: string
  // The SQL statement used to create the view
  query: string
  // The database id
  database_id: string
  // The machine-friendly name
  internal_name: string
  // The visibility; if true, The will be displayed publicly and is searchable
  is_public: boolean
  // The insights; if true, The schema will be displayed publicly and is searchable
  is_schema_public: boolean
  // If true, the view is default for the database
  initial_view?: boolean
  // The sha256-hash of the query
  query_hash: string
  // The owner username
  owned_by?: string
}
```

- 400 Create view query is malformed

- 403 Credentials missing

- 404 Failed to find database/user in metadata database.

- 409 View exists with name

- 423 Create view resulted in an invalid query statement

- 502 Connection to search service failed

- 503 Failed to save in search service

***

### [GET]/api/v1/database/{databaseId}/table

- Summary  
List tables

- Description  
Lists all tables known to the metadata database. When a database has a hidden schema (i.e. when `is_schema_public` is `false`), then the user needs to have at least read access and the role `list-tables`.

- Security  
bearerAuth  
basicAuth  

#### Responses

- 200 List tables

`application/json`

```ts
{
  // The id
  id: string
  // The user-friendly table name
  name: string
  // The comment
  description?: string
  // The database id
  database_id: string
  // The machine-friendly table name
  internal_name: string
  // If true, The is using data versioning
  is_versioned: boolean
  // The visibility; if true, The will be displayed publicly and is searchable
  is_public: boolean
  // The insights; if true, The schema will be displayed publicly and is searchable
  is_schema_public: boolean
  // The owner username
  owned_by: string
}[]
```

- 403 List tables not permitted

- 404 Database could not be found

***

### [POST]/api/v1/database/{databaseId}/table

- Summary  
Create table

- Description  
Creates a table in the database with id. Requires role `create-table`.

- Security  
bearerAuth  
basicAuth  

#### RequestBody

- application/json

```ts
{
  // The table name
  name: string
  // The table comment
  description?: string
  columns: {
    // The column name
    name: string
    // The data type
    type: enum[char, varchar, binary, varbinary, tinyblob, tinytext, text, blob, mediumtext, mediumblob, longtext, longblob, enum, set, serial, bit, tinyint, bool, smallint, mediumint, int, bigint, float, double, decimal, date, datetime, timestamp, time, year]
    // The size determines the number of digits before the comma: x=size-d where size >= d
    size?: integer
    // The digits behind the comma
    d?: integer
    // The column comment
    description?: string
    enums?: string[]
    sets?: string[]
    // The index length
    index_length?: integer
    // If set to true, the column value can be null
    null_allowed: boolean
    // The column concept
    concept_uri?: string
    // The column unit
    unit_uri?: string
  }[]
  constraints: {
    uniques?: string[][]
    checks?: string[]
    foreign_keys: {
      columns?: string[]
      // The name of the foreign table
      referenced_table: string
      referenced_columns?: string[]
      // The integrity action when updating tuples
      on_update?: enum[restrict, cascade, set_null, no_action, set_default]
      // The integrity action when deleting tuples
      on_delete?: enum[restrict, cascade, set_null, no_action, set_default]
    }[]
    primary_key?: string[]
  }
  // The table visibility; if true, the table will be displayed publicly and is searchable
  is_public: boolean
  // The table insights; if true, the table schema will be displayed publicly and is searchable
  is_schema_public: boolean
}
```

#### Responses

- 201 Created a new table

`application/json`

```ts
{
  // The id
  id: string
  // The user-friendly table name
  name: string
  // The comment
  description?: string
  // The database id
  database_id: string
  // The machine-friendly table name
  internal_name: string
  // If true, The is using data versioning
  is_versioned: boolean
  // The visibility; if true, The will be displayed publicly and is searchable
  is_public: boolean
  // The insights; if true, The schema will be displayed publicly and is searchable
  is_schema_public: boolean
  // The owner username
  owned_by: string
}
```

- 400 Create table query is malformed

- 403 Create table not permitted

- 404 Database, container or user could not be found

- 409 Create table conflicts with existing table name

- 502 Connection to search service failed

- 503 Failed to save in search service

***

### [GET]/api/v1/container

- Summary  
List containers

- Description  
List all containers in the metadata database.

#### Parameters(Query)

```ts
limit?: integer
```

#### Responses

- 200 List containers

`application/json`

```ts
{
  // The container id
  id: string
  // The container hash
  hash: string
  // The user-friendly container name
  name: string
  image: {
    // The image id
    id: string
    // The image name
    name: string
    // The image tag
    version: string
    // Marks the image as default
    default: boolean
  }
  // The number of databases the container is capable to hold simultaneously, if null the container has no limit
  quota: integer
  // The number of databases currently in the container
  count: integer
  // The machine-friendly container name
  internal_name: string
}[]
```

***

### [POST]/api/v1/container

- Summary  
Create container

- Description  
Creates a container in the metadata database. Requires role `create-container`.

- Security  
bearerAuth  
basicAuth  

#### RequestBody

- application/json

```ts
{
  // The user-friendly container name
  name: string
  // The container hostname
  host: string
  // The container port
  port: integer
  quota: integer
  // The image id used for the container database engine
  image_id: string
  // The username of the privileged user
  privileged_username: string
  // The password of the privileged user
  privileged_password: string
}
```

#### Responses

- 201 Created a new container

`application/json`

```ts
{
  // The container id
  id: string
  // The user-friendly container name
  name: string
  image: {
    // The image id
    id: string
    // The image name
    name: string
    version: string
    operators: {
      // The operator id
      id: string
      // The machine-friendly name of the operator
      value: string
      // The documentation link
      documentation: string
      // The user-friendly name of the operator
      display_name: string
    }[]
    default: boolean
    data_types: {
      // The id of the data type
      id: string
      // The machine-friendly value of the data type
      value: string
      // The documentation link
      documentation: string
      // The human-friendly name of the data type
      display_name: string
      // The minimum size
      size_min?: integer
      // The maximum size
      size_max?: integer
      // The default size
      size_default?: integer
      // If true, the size parameter cannot be empty
      size_required?: boolean
      // The step increment
      size_step?: integer
      // The minimum d
      d_min?: integer
      // The maximum d
      d_max?: integer
      // The default d
      d_default?: integer
      // If true, the d parameter cannot be empty
      d_required?: boolean
      // The d increment
      d_step?: integer
      // The user-friendly description of the data format
      data_hint?: string
      // The user-friendly description of the data type
      type_hint?: string
      // frontend needs to quote this data type
      is_quoted: boolean
      // frontend can build this data type
      is_buildable: boolean
    }[]
  }
  // The number of databases the container is capable to hold simultaneously, if null the container has no limit
  quota: integer
  // The number of databases currently in the container
  count: integer
  // The machine-friendly container name
  internal_name: string
}
```

- 400 Container payload malformed

- 403 Create container not permitted, need authority `create-container`

- 404 Container image or user could not be found

- 409 Container name already exists

***

### [GET]/api/v1/user

- Summary  
List users

- Description  
Lists users known to the metadata database. Internal users are omitted from the result list. If the optional query parameter `username` is present, the result list can be filtered by matching this exact username.

#### Parameters(Query)

```ts
username?: string
```

#### Responses

- 200 List users

`application/json`

```ts
{
  id?: string
  // Only contains lowercase characters
  username: string
  name?: string
  orcid?: string
  qualified_name?: string
  given_name?: string
  family_name?: string
}[]
```

- 403 Listing not allowed

`application/json`

```ts
{
  status: enum[100 CONTINUE, 101 SWITCHING_PROTOCOLS, 102 PROCESSING, 103 EARLY_HINTS, 103 CHECKPOINT, 200 OK, 201 CREATED, 202 ACCEPTED, 203 NON_AUTHORITATIVE_INFORMATION, 204 NO_CONTENT, 205 RESET_CONTENT, 206 PARTIAL_CONTENT, 207 MULTI_STATUS, 208 ALREADY_REPORTED, 226 IM_USED, 300 MULTIPLE_CHOICES, 301 MOVED_PERMANENTLY, 302 FOUND, 302 MOVED_TEMPORARILY, 303 SEE_OTHER, 304 NOT_MODIFIED, 305 USE_PROXY, 307 TEMPORARY_REDIRECT, 308 PERMANENT_REDIRECT, 400 BAD_REQUEST, 401 UNAUTHORIZED, 402 PAYMENT_REQUIRED, 403 FORBIDDEN, 404 NOT_FOUND, 405 METHOD_NOT_ALLOWED, 406 NOT_ACCEPTABLE, 407 PROXY_AUTHENTICATION_REQUIRED, 408 REQUEST_TIMEOUT, 409 CONFLICT, 410 GONE, 411 LENGTH_REQUIRED, 412 PRECONDITION_FAILED, 413 PAYLOAD_TOO_LARGE, 413 REQUEST_ENTITY_TOO_LARGE, 414 URI_TOO_LONG, 414 REQUEST_URI_TOO_LONG, 415 UNSUPPORTED_MEDIA_TYPE, 416 REQUESTED_RANGE_NOT_SATISFIABLE, 417 EXPECTATION_FAILED, 418 I_AM_A_TEAPOT, 419 INSUFFICIENT_SPACE_ON_RESOURCE, 420 METHOD_FAILURE, 421 DESTINATION_LOCKED, 422 UNPROCESSABLE_ENTITY, 423 LOCKED, 424 FAILED_DEPENDENCY, 425 TOO_EARLY, 426 UPGRADE_REQUIRED, 428 PRECONDITION_REQUIRED, 429 TOO_MANY_REQUESTS, 431 REQUEST_HEADER_FIELDS_TOO_LARGE, 451 UNAVAILABLE_FOR_LEGAL_REASONS, 500 INTERNAL_SERVER_ERROR, 501 NOT_IMPLEMENTED, 502 BAD_GATEWAY, 503 SERVICE_UNAVAILABLE, 504 GATEWAY_TIMEOUT, 505 HTTP_VERSION_NOT_SUPPORTED, 506 VARIANT_ALSO_NEGOTIATES, 507 INSUFFICIENT_STORAGE, 508 LOOP_DETECTED, 509 BANDWIDTH_LIMIT_EXCEEDED, 510 NOT_EXTENDED, 511 NETWORK_AUTHENTICATION_REQUIRED]
  // The error message in English
  message: string
  // The error code used for internationalization of the error messages
  code: string
}
```

***

### [GET]/api/v1/oai

- Summary  
Get record

#### Parameters(Query)

```ts
{
  "name": "verb",
  "in": "query"
}
```

```ts
parameters: {
  metadataPrefix?: string
  from?: string
  until?: string
  set?: string
  resumptionToken?: string
  fromDate?: string
  untilDate?: string
  parametersString?: string
}
```

#### Responses

- 200 List containers

`text/xml`

```ts
{
  "type": "string"
}
```

***

### [GET]/api/v1/message/message/{messageId}

- Summary  
Find message

- Description  
Finds a message with id in the metadata database.

#### Responses

- 200 Get messages

`application/json`

```ts
{
  id: string
  type: enum[error, warning, info]
  message: string
  link?: string
  link_text?: string
  display_start?: string
  display_end?: string
}
```

- 404 Could not find message

***

### [GET]/api/v1/license

- Summary  
List licenses

- Description  
Lists licenses known to the metadata database.

#### Responses

- 200 List of licenses

`application/json`

```ts
{
  // The id
  identifier: string
  // The link to the license such as an SPDX identifier
  uri: string
  // A user-friendly short abstract of key details of the license
  description?: string
}[]
```

***

### [GET]/api/v1/identifier/retrieve

- Summary  
Retrieve PID metadata

- Description  
Retrieves Persistent Identifier (PID) metadata from external endpoints. Requires authentication. Supported PIDs are: ORCID, ROR, DOI.

#### Parameters(Query)

```ts
url: string
```

#### Responses

- 200 Retrieved metadata from identifier

`application/json`

```ts
{
  id: string
  links: {
    self: string
    data?: string
    self_html: string
    dashboard_html?: string
  }
  type: enum[database, subset, table, view]
  titles: {
    id: string
    title?: string
    language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
    type?: enum[AlternativeTitle, Subtitle, TranslatedTitle, Other]
  }[]
  descriptions: {
    id: string
    description?: string
    language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
    type?: enum[Abstract, Methods, SeriesInformation, TableOfContents, TechnicalInfo, Other]
  }[]
  funders: {
    id: string
    funder_name: string
    funder_identifier?: string
    funder_identifier_type?: enum[Crossref Funder ID, ROR, GND, ISNI, Other]
    scheme_uri?: string
    award_number?: string
    award_title?: string
  }[]
  query: string
  execution?: string
  doi?: string
  publisher: string
  owner: {
    id?: string
    // Only contains lowercase characters
    username: string
    name?: string
    orcid?: string
    qualified_name?: string
    given_name?: string
    family_name?: string
  }
  language: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
  licenses: {
    // The id
    identifier: string
    // The link to the license such as an SPDX identifier
    uri: string
    // A user-friendly short abstract of key details of the license
    description?: string
  }[]
  creators: {
    id: string
    firstname?: string
    lastname?: string
    affiliation?: string
    creator_name: string
    name_type?: enum[Personal, Organizational]
    name_identifier?: string
    name_identifier_scheme?: enum[ORCID, ROR, ISNI, GRID]
    name_identifier_scheme_uri?: string
    affiliation_identifier?: string
    affiliation_identifier_scheme?: enum[ROR, GRID, ISNI]
    affiliation_identifier_scheme_uri?: string
  }[]
  status: enum[draft, published]
  created?: string
  database_id: string
  query_id?: string
  table_id?: string
  view_id?: string
  query_normalized: string
  related_identifiers: {
    id: string
    value: string
    type: enum[DOI, URL, URN, ARK, arXiv, bibcode, EAN13, EISSN, Handle, IGSN, ISBN, ISTC, LISSN, LSID, PMID, PURL, UPC, w3id]
    relation: enum[IsCitedBy, Cites, IsSupplementTo, IsSupplementedBy, IsContinuedBy, Continues, IsDescribedBy, Describes, HasMetadata, IsMetadataFor, HasVersion, IsVersionOf, IsNewVersionOf, IsPreviousVersionOf, IsPartOf, HasPart, IsPublishedIn, IsReferencedBy, References, IsDocumentedBy, Documents, IsCompiledBy, Compiles, IsVariantFormOf, IsOriginalFormOf, IsIdenticalTo, IsReviewedBy, Reviews, IsDerivedFrom, IsSourceOf, IsRequiredBy, Requires, IsObsoletedBy, Obsoletes]
  }[]
  // query hash in sha512
  query_hash: string
  result_hash?: string
  result_number?: integer
  publication_day?: integer
  publication_month?: integer
  publication_year: integer
}
```

- 404 Failed to find metadata for identifier

***

### [GET]/api/v1/database/{databaseId}

- Summary  
Find database

- Description  
Finds a database with id.

- Security  
bearerAuth  
basicAuth  

#### Responses

- 200 Database found successfully

`application/json`

```ts
{
  id: string
  // The name
  name: string
  // The comment
  description?: string
  identifiers: {
    id: string
    type: enum[database, subset, table, view]
    creators: {
      id: string
      affiliation?: string
      creator_name: string
      name_type?: enum[Personal, Organizational]
      name_identifier?: string
      name_identifier_scheme?: enum[ORCID, ROR, ISNI, GRID]
      affiliation_identifier?: string
      affiliation_identifier_scheme?: enum[ROR, GRID, ISNI]
    }[]
    titles: {
      id: string
      title?: string
      language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
      type?: enum[AlternativeTitle, Subtitle, TranslatedTitle, Other]
    }[]
    descriptions: {
      id: string
      description?: string
      language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
      type?: enum[Abstract, Methods, SeriesInformation, TableOfContents, TechnicalInfo, Other]
    }[]
    doi?: string
    publisher: string
    status: enum[draft, published]
    created?: string
    database_id: string
    query_id?: string
    table_id?: string
    view_id?: string
    publication_year: integer
    // The owner username
    owned_by: string
  }[]
  // The machine-friendly database name
  internal_name: string
  // The visibility; if true, The will be displayed publicly and is searchable
  is_public: boolean
  // The insights; if true, The schema will be displayed publicly and is searchable
  is_schema_public: boolean
  contact: {
    id?: string
    // Only contains lowercase characters
    username: string
    name?: string
    orcid?: string
    qualified_name?: string
    given_name?: string
    family_name?: string
  }
  // The owner username
  owned_by: string
  // The preview image
  preview_image?: string
}
```

- 403 Not allowed to view database

- 404 Database could not be found

***

### [GET]/api/v1/container/{containerId}

- Summary  
Find container

- Description  
Finds a container in the metadata database.

#### Responses

- 200 Found container

`application/json`

```ts
{
  // The container id
  id: string
  // The user-friendly container name
  name: string
  image: {
    // The image id
    id: string
    // The image name
    name: string
    version: string
    operators: {
      // The operator id
      id: string
      // The machine-friendly name of the operator
      value: string
      // The documentation link
      documentation: string
      // The user-friendly name of the operator
      display_name: string
    }[]
    default: boolean
    data_types: {
      // The id of the data type
      id: string
      // The machine-friendly value of the data type
      value: string
      // The documentation link
      documentation: string
      // The human-friendly name of the data type
      display_name: string
      // The minimum size
      size_min?: integer
      // The maximum size
      size_max?: integer
      // The default size
      size_default?: integer
      // If true, the size parameter cannot be empty
      size_required?: boolean
      // The step increment
      size_step?: integer
      // The minimum d
      d_min?: integer
      // The maximum d
      d_max?: integer
      // The default d
      d_default?: integer
      // If true, the d parameter cannot be empty
      d_required?: boolean
      // The d increment
      d_step?: integer
      // The user-friendly description of the data format
      data_hint?: string
      // The user-friendly description of the data type
      type_hint?: string
      // frontend needs to quote this data type
      is_quoted: boolean
      // frontend can build this data type
      is_buildable: boolean
    }[]
  }
  // The number of databases the container is capable to hold simultaneously, if null the container has no limit
  quota: integer
  // The number of databases currently in the container
  count: integer
  // The machine-friendly container name
  internal_name: string
}
```

- 404 Container image could not be found

***

### [DELETE]/api/v1/container/{containerId}

- Summary  
Delete container

- Description  
Deletes a container in the metadata database. Requires role `delete-container`.

- Security  
bearerAuth  
basicAuth  

#### Responses

- 202 Deleted container

- 403 Create container not permitted, need authority `delete-container`

- 404 Container not found

***

### [GET]/api/v1/search

- Summary  
Performs a fuzzy search

- Description  
Performs a fuzzy search

#### Parameters(Query)

```ts
q: string
```

#### Responses

- 200 OK, contains the elements formatted as an array of JSON arrays

`application/json`

```ts
[]
```

- 415 Wrong accept type

***

### [POST]/api/v1/search/{field_type}

- Summary  
Performs a general search

- Description  
Performs a general search

#### Parameters(Query)

```ts
t1?: integer
```

```ts
t2?: integer
```

#### Parameters(Body)

```ts
body: {
  field_value_pairs: {
  }
  search_term: string
}
```

#### Responses

- 200 OK, contains the elements formatted as an array of JSON arrays

`application/json`

```ts
{
  results: {
  }[]
  // Same as the requested type
  type?: enum[database, table, view, column, user, identifier, concept, unit]
}
```

***

### [GET]/api/v1/search/{field_type}/fields

- Summary  
Get searchable fields

#### Responses

- 200 List of fields

`application/json`

```ts
{
  results: {
    attr_friendly_name: string
    attr_name: string
    // OpenSearch data types.
    type: string
  }[]
}
```

- 404 Invalid type.

***

### [GET]/api/v1/search/{index}

- Summary  
Gets the index

- Description  
Gets the index

#### Parameters(Body)

```ts
body: {
  field_value_pairs: {
  }
  search_term?: string
  t1?: integer
  t2?: integer
}
```

#### Responses

- 200 OK, contains the elements formatted as an array of JSON arrays

`application/json`

```ts
{
  "properties": {
    "results": {
      "items": {
        "type": "object"
      },
      "type": "array"
    },
    "type": {
      "description": "Same as the requested type",
      "enum": [
        "database",
        "table",
        "view",
        "column",
        "user",
        "identifier",
        "concept",
        "unit"
      ],
      "type": "string"
    }
  },
  "required": [
    "results",
    "type"
  ]
}
```

## References

### #/components/securitySchemes/basicAuth

```ts
{
  "in": "header",
  "scheme": "basic",
  "type": "http"
}
```

### #/components/securitySchemes/bearerAuth

```ts
{
  "bearerFormat": "JWT",
  "in": "header",
  "scheme": "bearer",
  "type": "http"
}
```

### #/components/schemas/DatabaseAccessDto

```ts
{
  // The user name
  username: string
  user: {
    id?: string
    // Only contains lowercase characters
    username: string
    name?: string
    orcid?: string
    qualified_name?: string
    given_name?: string
    family_name?: string
  }
  type: enum[read, write_own, write_all]
}
```

### #/components/schemas/UserBriefDto

```ts
{
  id?: string
  // Only contains lowercase characters
  username: string
  name?: string
  orcid?: string
  qualified_name?: string
  given_name?: string
  family_name?: string
}
```

### #/components/schemas/TupleUpdateDto

```ts
{
  // The key-value data map
  data: {
  }
  // The map of conditions
  keys: {
  }
}
```

### #/components/schemas/QueryPersistDto

```ts
{
  // If false, the query is marked for deletion at a later point in time
  persist: boolean
}
```

### #/components/schemas/CreatorBriefDto

```ts
{
  id: string
  affiliation?: string
  creator_name: string
  name_type?: enum[Personal, Organizational]
  name_identifier?: string
  name_identifier_scheme?: enum[ORCID, ROR, ISNI, GRID]
  affiliation_identifier?: string
  affiliation_identifier_scheme?: enum[ROR, GRID, ISNI]
}
```

### #/components/schemas/IdentifierBriefDto

```ts
{
  id: string
  type: enum[database, subset, table, view]
  creators: {
    id: string
    affiliation?: string
    creator_name: string
    name_type?: enum[Personal, Organizational]
    name_identifier?: string
    name_identifier_scheme?: enum[ORCID, ROR, ISNI, GRID]
    affiliation_identifier?: string
    affiliation_identifier_scheme?: enum[ROR, GRID, ISNI]
  }[]
  titles: {
    id: string
    title?: string
    language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
    type?: enum[AlternativeTitle, Subtitle, TranslatedTitle, Other]
  }[]
  descriptions: {
    id: string
    description?: string
    language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
    type?: enum[Abstract, Methods, SeriesInformation, TableOfContents, TechnicalInfo, Other]
  }[]
  doi?: string
  publisher: string
  status: enum[draft, published]
  created?: string
  database_id: string
  query_id?: string
  table_id?: string
  view_id?: string
  publication_year: integer
  // The owner username
  owned_by: string
}
```

### #/components/schemas/IdentifierDescriptionDto

```ts
{
  id: string
  description?: string
  language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
  type?: enum[Abstract, Methods, SeriesInformation, TableOfContents, TechnicalInfo, Other]
}
```

### #/components/schemas/IdentifierTitleDto

```ts
{
  id: string
  title?: string
  language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
  type?: enum[AlternativeTitle, Subtitle, TranslatedTitle, Other]
}
```

### #/components/schemas/QueryDto

```ts
{
  // The query id
  id: string
  owner: {
    id?: string
    // Only contains lowercase characters
    username: string
    name?: string
    orcid?: string
    qualified_name?: string
    given_name?: string
    family_name?: string
  }
  // The timestamp when the query was executed
  execution: string
  // The mapped SQL query
  query: string
  // The query type
  type?: enum[query, view]
  identifiers: {
    id: string
    type: enum[database, subset, table, view]
    creators: {
      id: string
      affiliation?: string
      creator_name: string
      name_type?: enum[Personal, Organizational]
      name_identifier?: string
      name_identifier_scheme?: enum[ORCID, ROR, ISNI, GRID]
      affiliation_identifier?: string
      affiliation_identifier_scheme?: enum[ROR, GRID, ISNI]
    }[]
    titles: {
      id: string
      title?: string
      language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
      type?: enum[AlternativeTitle, Subtitle, TranslatedTitle, Other]
    }[]
    descriptions: {
      id: string
      description?: string
      language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
      type?: enum[Abstract, Methods, SeriesInformation, TableOfContents, TechnicalInfo, Other]
    }[]
    doi?: string
    publisher: string
    status: enum[draft, published]
    created?: string
    database_id: string
    query_id?: string
    table_id?: string
    view_id?: string
    publication_year: integer
    // The owner username
    owned_by: string
  }[]
  // The database id
  database_id: string
  // The normalized SQL query as executed
  query_normalized: string
  // The sha256-hash of the mapped query
  query_hash: string
  // The sha256-hash of the result
  result_hash?: string
  // The row count of the result
  result_number?: integer
  // If false, the query is marked for deletion at a later point in time
  is_persisted: boolean
}
```

### #/components/schemas/TupleDto

```ts
{
  // The key-value data map
  data: {
  }
}
```

### #/components/schemas/ImportDto

```ts
{
  // The key of the S3 binary object in the storage service
  location: string
  // If true, the first line contains the column names, otherwise it contains only data
  header: boolean
  // The column delimiter of the dataset
  separator: string
  // The quote symbol around values in the dataset
  quote?: string
  // The newline symbol
  line_termination?: string
}
```

### #/components/schemas/ConditionalDto

```ts
{
  // The id of the column
  column_id: string
  // The id of the foreign column
  foreign_column_id: string
}
```

### #/components/schemas/FilterDto

```ts
{
  // The filter type
  type: enum[where, or, and]
  // The filter value
  value?: string
  // The column id
  column_id: string
  // The operator id
  operator_id: string
}
```

### #/components/schemas/JoinDto

```ts
{
  // The type of join
  type: enum[inner, left, right, cross]
  conditionals: {
    // The id of the column
    column_id: string
    // The id of the foreign column
    foreign_column_id: string
  }[]
  // The id of the data source
  datasource_id: string
}
```

### #/components/schemas/OrderDto

```ts
{
  // The sort direction
  direction?: enum[asc, desc]
  // The column id
  column_id: string
}
```

### #/components/schemas/SubsetColumnDto

```ts
{
  // The id of the column
  id: string
  // The column alias
  alias?: string
}
```

### #/components/schemas/SubsetDto

```ts
{
  columns: {
    // The id of the column
    id: string
    // The column alias
    alias?: string
  }[]
  joins: {
    // The type of join
    type: enum[inner, left, right, cross]
    conditionals: {
      // The id of the column
      column_id: string
      // The id of the foreign column
      foreign_column_id: string
    }[]
    // The id of the data source
    datasource_id: string
  }[]
  filters: {
    // The filter type
    type: enum[where, or, and]
    // The filter value
    value?: string
    // The column id
    column_id: string
    // The operator id
    operator_id: string
  }[]
  orders: {
    // The sort direction
    direction?: enum[asc, desc]
    // The column id
    column_id: string
  }[]
  datasource_ids?: string[]
}
```

### #/components/schemas/ColumnAnalysisResultDto

```ts
{
  // The column name
  name: string
  // The column data type
  datatype: enum[char, varchar, binary, varbinary, tinyblob, tinytext, text, blob, mediumtext, mediumblob, longtext, longblob, enum, set, serial, bit, tinyint, bool, smallint, mediumint, int, bigint, float, double, decimal, date, datetime, timestamp, time, year]
  // The size determines the number of digits before the comma: x=size-d where size >= d
  size?: integer
  // The digits behind the comma
  d?: integer
  enums?: string[]
  sets?: string[]
  // If set to true, the column value can be null
  null_allowed?: boolean
  // The column is a candidate to be part of a composite primary key
  primary_key?: boolean
}
```

### #/components/schemas/SchemaAnalysisResultDto

```ts
{
  // The column delimiter of the dataset
  delimiter: string
  // The quote symbol around values in the dataset
  quote: string
  // The escape symbol
  escape: string
  // The comment symbol
  comment: string
  columns: {
    // The column name
    name: string
    // The column data type
    datatype: enum[char, varchar, binary, varbinary, tinyblob, tinytext, text, blob, mediumtext, mediumblob, longtext, longblob, enum, set, serial, bit, tinyint, bool, smallint, mediumint, int, bigint, float, double, decimal, date, datetime, timestamp, time, year]
    // The size determines the number of digits before the comma: x=size-d where size >= d
    size?: integer
    // The digits behind the comma
    d?: integer
    enums?: string[]
    sets?: string[]
    // If set to true, the column value can be null
    null_allowed?: boolean
    // The column is a candidate to be part of a composite primary key
    primary_key?: boolean
  }[]
  // The newline symbol
  newline_delimiter: string
  // The number of rows to skip
  skip_rows: integer
  // Detected the first line as header
  has_header: boolean
  // The format of the date
  date_format?: string
  // The format of the timestamp
  timestamp_format?: string
}
```

### #/components/schemas/TableHistoryDto

```ts
{
  // The timestamp
  timestamp: string
  // The event type
  event: enum[insert, delete]
  // The total number of rows affected by the event
  total: integer
}
```

### #/components/schemas/TupleDeleteDto

```ts
{
  // The map of conditions
  keys: {
  }
}
```

### #/components/schemas/UserAttributesDto

```ts
{
  theme: string
  orcid?: string
  affiliation?: string
  language: string
}
```

### #/components/schemas/UserDto

```ts
{
  id?: string
  name?: string
  username: string
  password?: string
  attributes: {
    theme: string
    orcid?: string
    affiliation?: string
    language: string
  }
  qualified_name?: string
  given_name?: string
  family_name?: string
}
```

### #/components/schemas/DatabaseBriefDto

```ts
{
  id: string
  // The name
  name: string
  // The comment
  description?: string
  identifiers: {
    id: string
    type: enum[database, subset, table, view]
    creators: {
      id: string
      affiliation?: string
      creator_name: string
      name_type?: enum[Personal, Organizational]
      name_identifier?: string
      name_identifier_scheme?: enum[ORCID, ROR, ISNI, GRID]
      affiliation_identifier?: string
      affiliation_identifier_scheme?: enum[ROR, GRID, ISNI]
    }[]
    titles: {
      id: string
      title?: string
      language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
      type?: enum[AlternativeTitle, Subtitle, TranslatedTitle, Other]
    }[]
    descriptions: {
      id: string
      description?: string
      language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
      type?: enum[Abstract, Methods, SeriesInformation, TableOfContents, TechnicalInfo, Other]
    }[]
    doi?: string
    publisher: string
    status: enum[draft, published]
    created?: string
    database_id: string
    query_id?: string
    table_id?: string
    view_id?: string
    publication_year: integer
    // The owner username
    owned_by: string
  }[]
  // The machine-friendly database name
  internal_name: string
  // The visibility; if true, The will be displayed publicly and is searchable
  is_public: boolean
  // The insights; if true, The schema will be displayed publicly and is searchable
  is_schema_public: boolean
  contact: {
    id?: string
    // Only contains lowercase characters
    username: string
    name?: string
    orcid?: string
    qualified_name?: string
    given_name?: string
    family_name?: string
  }
  // The owner username
  owned_by: string
  // The preview image
  preview_image?: string
}
```

### #/components/schemas/UserUpdateDto

```ts
{
  firstname?: string
  lastname?: string
  affiliation?: string
  orcid?: string
  theme: string
  language: string
}
```

### #/components/schemas/BannerMessageUpdateDto

```ts
{
  type: enum[error, warning, info]
  message: string
  link?: string
  link_text?: string
  display_start?: string
  display_end?: string
}
```

### #/components/schemas/BannerMessageBriefDto

```ts
{
  type: enum[error, warning, info]
  message: string
  link?: string
  link_text?: string
}
```

### #/components/schemas/ImageChangeDto

```ts
{
  // The URL of the registry without protocol
  registry: string
  // The SQL dialect class name
  dialect: string
  // The default image port to access the database
  default_port: integer
  // The driver class name
  driver_class: string
  // The method used by JDBC
  jdbc_method: string
}
```

### #/components/schemas/DataTypeDto

```ts
{
  // The id of the data type
  id: string
  // The machine-friendly value of the data type
  value: string
  // The documentation link
  documentation: string
  // The human-friendly name of the data type
  display_name: string
  // The minimum size
  size_min?: integer
  // The maximum size
  size_max?: integer
  // The default size
  size_default?: integer
  // If true, the size parameter cannot be empty
  size_required?: boolean
  // The step increment
  size_step?: integer
  // The minimum d
  d_min?: integer
  // The maximum d
  d_max?: integer
  // The default d
  d_default?: integer
  // If true, the d parameter cannot be empty
  d_required?: boolean
  // The d increment
  d_step?: integer
  // The user-friendly description of the data format
  data_hint?: string
  // The user-friendly description of the data type
  type_hint?: string
  // frontend needs to quote this data type
  is_quoted: boolean
  // frontend can build this data type
  is_buildable: boolean
}
```

### #/components/schemas/ImageDto

```ts
{
  // The image id
  id: string
  // The image name
  name: string
  version: string
  operators: {
    // The operator id
    id: string
    // The machine-friendly name of the operator
    value: string
    // The documentation link
    documentation: string
    // The user-friendly name of the operator
    display_name: string
  }[]
  default: boolean
  data_types: {
    // The id of the data type
    id: string
    // The machine-friendly value of the data type
    value: string
    // The documentation link
    documentation: string
    // The human-friendly name of the data type
    display_name: string
    // The minimum size
    size_min?: integer
    // The maximum size
    size_max?: integer
    // The default size
    size_default?: integer
    // If true, the size parameter cannot be empty
    size_required?: boolean
    // The step increment
    size_step?: integer
    // The minimum d
    d_min?: integer
    // The maximum d
    d_max?: integer
    // The default d
    d_default?: integer
    // If true, the d parameter cannot be empty
    d_required?: boolean
    // The d increment
    d_step?: integer
    // The user-friendly description of the data format
    data_hint?: string
    // The user-friendly description of the data type
    type_hint?: string
    // frontend needs to quote this data type
    is_quoted: boolean
    // frontend can build this data type
    is_buildable: boolean
  }[]
}
```

### #/components/schemas/OperatorDto

```ts
{
  // The operator id
  id: string
  // The machine-friendly name of the operator
  value: string
  // The documentation link
  documentation: string
  // The user-friendly name of the operator
  display_name: string
}
```

### #/components/schemas/IdentifierSaveDto

```ts
{
  id: string
  type: enum[database, subset, table, view]
  doi?: string
  titles: {
    id: string
    title: string
    language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
    type?: enum[AlternativeTitle, Subtitle, TranslatedTitle, Other]
  }[]
  descriptions: {
    id: string
    description: string
    language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
    type?: enum[Abstract, Methods, SeriesInformation, TableOfContents, TechnicalInfo, Other]
  }[]
  funders: {
    id: string
    funder_name: string
    funder_identifier?: string
    funder_identifier_type?: enum[Crossref Funder ID, ROR, GND, ISNI, Other]
    scheme_uri?: string
    award_number?: string
    award_title?: string
  }[]
  licenses: {
    // The id
    identifier: string
    // The link to the license such as an SPDX identifier
    uri: string
    // A user-friendly short abstract of key details of the license
    description?: string
  }[]
  publisher: string
  language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
  creators: {
    id: string
    firstname?: string
    lastname?: string
    affiliation?: string
    creator_name: string
    name_type?: enum[Personal, Organizational]
    name_identifier?: string
    affiliation_identifier?: string
  }[]
  database_id: string
  query_id?: string
  view_id?: string
  table_id?: string
  publication_day?: integer
  publication_month?: integer
  publication_year: integer
  related_identifiers: {
    id: string
    value: string
    type: enum[DOI, URL, URN, ARK, arXiv, bibcode, EAN13, EISSN, Handle, IGSN, ISBN, ISTC, LISSN, LSID, PMID, PURL, UPC, w3id]
    relation: enum[IsCitedBy, Cites, IsSupplementTo, IsSupplementedBy, IsContinuedBy, Continues, IsDescribedBy, Describes, HasMetadata, IsMetadataFor, HasVersion, IsVersionOf, IsNewVersionOf, IsPreviousVersionOf, IsPartOf, HasPart, IsPublishedIn, IsReferencedBy, References, IsDocumentedBy, Documents, IsCompiledBy, Compiles, IsVariantFormOf, IsOriginalFormOf, IsIdenticalTo, IsReviewedBy, Reviews, IsDerivedFrom, IsSourceOf, IsRequiredBy, Requires, IsObsoletedBy, Obsoletes]
  }[]
}
```

### #/components/schemas/LicenseDto

```ts
{
  // The id
  identifier: string
  // The link to the license such as an SPDX identifier
  uri: string
  // A user-friendly short abstract of key details of the license
  description?: string
}
```

### #/components/schemas/SaveIdentifierCreatorDto

```ts
{
  id: string
  firstname?: string
  lastname?: string
  affiliation?: string
  creator_name: string
  name_type?: enum[Personal, Organizational]
  name_identifier?: string
  affiliation_identifier?: string
}
```

### #/components/schemas/SaveIdentifierDescriptionDto

```ts
{
  id: string
  description: string
  language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
  type?: enum[Abstract, Methods, SeriesInformation, TableOfContents, TechnicalInfo, Other]
}
```

### #/components/schemas/SaveIdentifierFunderDto

```ts
{
  id: string
  funder_name: string
  funder_identifier?: string
  funder_identifier_type?: enum[Crossref Funder ID, ROR, GND, ISNI, Other]
  scheme_uri?: string
  award_number?: string
  award_title?: string
}
```

### #/components/schemas/SaveIdentifierTitleDto

```ts
{
  id: string
  title: string
  language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
  type?: enum[AlternativeTitle, Subtitle, TranslatedTitle, Other]
}
```

### #/components/schemas/SaveRelatedIdentifierDto

```ts
{
  id: string
  value: string
  type: enum[DOI, URL, URN, ARK, arXiv, bibcode, EAN13, EISSN, Handle, IGSN, ISBN, ISTC, LISSN, LSID, PMID, PURL, UPC, w3id]
  relation: enum[IsCitedBy, Cites, IsSupplementTo, IsSupplementedBy, IsContinuedBy, Continues, IsDescribedBy, Describes, HasMetadata, IsMetadataFor, HasVersion, IsVersionOf, IsNewVersionOf, IsPreviousVersionOf, IsPartOf, HasPart, IsPublishedIn, IsReferencedBy, References, IsDocumentedBy, Documents, IsCompiledBy, Compiles, IsVariantFormOf, IsOriginalFormOf, IsIdenticalTo, IsReviewedBy, Reviews, IsDerivedFrom, IsSourceOf, IsRequiredBy, Requires, IsObsoletedBy, Obsoletes]
}
```

### #/components/schemas/CreatorDto

```ts
{
  id: string
  firstname?: string
  lastname?: string
  affiliation?: string
  creator_name: string
  name_type?: enum[Personal, Organizational]
  name_identifier?: string
  name_identifier_scheme?: enum[ORCID, ROR, ISNI, GRID]
  name_identifier_scheme_uri?: string
  affiliation_identifier?: string
  affiliation_identifier_scheme?: enum[ROR, GRID, ISNI]
  affiliation_identifier_scheme_uri?: string
}
```

### #/components/schemas/IdentifierDto

```ts
{
  id: string
  links: {
    self: string
    data?: string
    self_html: string
    dashboard_html?: string
  }
  type: enum[database, subset, table, view]
  titles: {
    id: string
    title?: string
    language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
    type?: enum[AlternativeTitle, Subtitle, TranslatedTitle, Other]
  }[]
  descriptions: {
    id: string
    description?: string
    language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
    type?: enum[Abstract, Methods, SeriesInformation, TableOfContents, TechnicalInfo, Other]
  }[]
  funders: {
    id: string
    funder_name: string
    funder_identifier?: string
    funder_identifier_type?: enum[Crossref Funder ID, ROR, GND, ISNI, Other]
    scheme_uri?: string
    award_number?: string
    award_title?: string
  }[]
  query: string
  execution?: string
  doi?: string
  publisher: string
  owner: {
    id?: string
    // Only contains lowercase characters
    username: string
    name?: string
    orcid?: string
    qualified_name?: string
    given_name?: string
    family_name?: string
  }
  language: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
  licenses: {
    // The id
    identifier: string
    // The link to the license such as an SPDX identifier
    uri: string
    // A user-friendly short abstract of key details of the license
    description?: string
  }[]
  creators: {
    id: string
    firstname?: string
    lastname?: string
    affiliation?: string
    creator_name: string
    name_type?: enum[Personal, Organizational]
    name_identifier?: string
    name_identifier_scheme?: enum[ORCID, ROR, ISNI, GRID]
    name_identifier_scheme_uri?: string
    affiliation_identifier?: string
    affiliation_identifier_scheme?: enum[ROR, GRID, ISNI]
    affiliation_identifier_scheme_uri?: string
  }[]
  status: enum[draft, published]
  created?: string
  database_id: string
  query_id?: string
  table_id?: string
  view_id?: string
  query_normalized: string
  related_identifiers: {
    id: string
    value: string
    type: enum[DOI, URL, URN, ARK, arXiv, bibcode, EAN13, EISSN, Handle, IGSN, ISBN, ISTC, LISSN, LSID, PMID, PURL, UPC, w3id]
    relation: enum[IsCitedBy, Cites, IsSupplementTo, IsSupplementedBy, IsContinuedBy, Continues, IsDescribedBy, Describes, HasMetadata, IsMetadataFor, HasVersion, IsVersionOf, IsNewVersionOf, IsPreviousVersionOf, IsPartOf, HasPart, IsPublishedIn, IsReferencedBy, References, IsDocumentedBy, Documents, IsCompiledBy, Compiles, IsVariantFormOf, IsOriginalFormOf, IsIdenticalTo, IsReviewedBy, Reviews, IsDerivedFrom, IsSourceOf, IsRequiredBy, Requires, IsObsoletedBy, Obsoletes]
  }[]
  // query hash in sha512
  query_hash: string
  result_hash?: string
  result_number?: integer
  publication_day?: integer
  publication_month?: integer
  publication_year: integer
}
```

### #/components/schemas/IdentifierFunderDto

```ts
{
  id: string
  funder_name: string
  funder_identifier?: string
  funder_identifier_type?: enum[Crossref Funder ID, ROR, GND, ISNI, Other]
  scheme_uri?: string
  award_number?: string
  award_title?: string
}
```

### #/components/schemas/LinksDto

```ts
{
  self: string
  data?: string
  self_html: string
  dashboard_html?: string
}
```

### #/components/schemas/RelatedIdentifierDto

```ts
{
  id: string
  value: string
  type: enum[DOI, URL, URN, ARK, arXiv, bibcode, EAN13, EISSN, Handle, IGSN, ISBN, ISTC, LISSN, LSID, PMID, PURL, UPC, w3id]
  relation: enum[IsCitedBy, Cites, IsSupplementTo, IsSupplementedBy, IsContinuedBy, Continues, IsDescribedBy, Describes, HasMetadata, IsMetadataFor, HasVersion, IsVersionOf, IsNewVersionOf, IsPreviousVersionOf, IsPartOf, HasPart, IsPublishedIn, IsReferencedBy, References, IsDocumentedBy, Documents, IsCompiledBy, Compiles, IsVariantFormOf, IsOriginalFormOf, IsIdenticalTo, IsReviewedBy, Reviews, IsDerivedFrom, IsSourceOf, IsRequiredBy, Requires, IsObsoletedBy, Obsoletes]
}
```

### #/components/schemas/DatabaseModifyVisibilityDto

```ts
{
  // The visibility; if true, The will be displayed publicly and is searchable
  is_public: boolean
  // The insights; if true, The schema will be displayed publicly and is searchable
  is_schema_public: boolean
  // If true, the dashboard will be managed
  is_dashboard_enabled: boolean
}
```

### #/components/schemas/ViewUpdateDto

```ts
{
  // The visibility; if true, The will be displayed publicly and is searchable
  is_public: boolean
  // The insights; if true, The schema will be displayed publicly and is searchable
  is_schema_public: boolean
}
```

### #/components/schemas/ViewBriefDto

```ts
{
  // The id
  id: string
  // The user-friendly name
  name: string
  // The SQL statement used to create the view
  query: string
  // The database id
  database_id: string
  // The machine-friendly name
  internal_name: string
  // The visibility; if true, The will be displayed publicly and is searchable
  is_public: boolean
  // The insights; if true, The schema will be displayed publicly and is searchable
  is_schema_public: boolean
  // If true, the view is default for the database
  initial_view?: boolean
  // The sha256-hash of the query
  query_hash: string
  // The owner username
  owned_by?: string
}
```

### #/components/schemas/TableUpdateDto

```ts
{
  // The comment
  description?: string
  // The visibility; if true, The will be displayed publicly and is searchable
  is_public: boolean
  // The insights; if true, The schema will be displayed publicly and is searchable
  is_schema_public: boolean
}
```

### #/components/schemas/TableBriefDto

```ts
{
  // The id
  id: string
  // The user-friendly table name
  name: string
  // The comment
  description?: string
  // The database id
  database_id: string
  // The machine-friendly table name
  internal_name: string
  // If true, The is using data versioning
  is_versioned: boolean
  // The visibility; if true, The will be displayed publicly and is searchable
  is_public: boolean
  // The insights; if true, The schema will be displayed publicly and is searchable
  is_schema_public: boolean
  // The owner username
  owned_by: string
}
```

### #/components/schemas/ColumnSemanticsUpdateDto

```ts
{
  // The description
  description?: string
  // The URI of the concept
  concept_uri?: string
  // The URI of the unit
  unit_uri?: string
}
```

### #/components/schemas/ColumnDto

```ts
{
  // The id
  id: string
  // The user-friendly column name
  name: string
  // The data source alias name
  alias?: string
  // The column size, determines the number of digits before the comma as x=size-d where size >= d
  size?: integer
  // The column d
  d?: integer
  // The statistically average numerical value
  mean?: number
  // The statistically most middle numerical value
  median?: number
  description?: string
  enums: {
    // The enum id
    id: string
    // The enum value
    value: string
  }[]
  sets: {
    // The set id
    id: string
    // The set value
    value: string
  }[]
  // The database id
  database_id: string
  // The table id
  table_id: string
  // The ordinal position of the colum to order it
  ord: integer
  // The machine-friendly column name
  internal_name: string
  // The length of the index
  index_length?: integer
  // The length of the total data in the table (index + data)
  length?: integer
  // The column type name
  type: enum[char, varchar, binary, varbinary, tinyblob, tinytext, text, blob, mediumtext, mediumblob, longtext, longblob, enum, set, serial, bit, tinyint, bool, smallint, mediumint, int, bigint, float, double, decimal, date, datetime, timestamp, time, year]
  // The data length
  data_length?: integer
  // The maximum data length
  max_data_length?: integer
  // The number of rows
  num_rows?: integer
  // The statistically highest numerical value
  val_min?: number
  // The statistically lowest numerical value
  val_max?: number
  // The statistically determined standard deviation
  std_dev?: number
  // The concept URI
  concept_uri?: string
  // The unit URI
  unit_uri?: string
  is_null_allowed: boolean
}
```

### #/components/schemas/EnumDto

```ts
{
  // The enum id
  id: string
  // The enum value
  value: string
}
```

### #/components/schemas/SetDto

```ts
{
  // The set id
  id: string
  // The set value
  value: string
}
```

### #/components/schemas/DatabaseTransferDto

```ts
{
  // The username of the new owner
  username: string
}
```

### #/components/schemas/DatabaseModifyImageDto

```ts
{
  // The key of the S3 binary object in the storage service
  key?: string
}
```

### #/components/schemas/CreateAccessDto

```ts
{
  // The access type
  type: enum[read, write_own, write_all]
}
```

### #/components/schemas/BannerMessageCreateDto

```ts
{
  type: enum[error, warning, info]
  message: string
  link?: string
  link_text?: string
  display_start?: string
  display_end?: string
}
```

### #/components/schemas/ImageCreateDto

```ts
{
  // The URL of the registry without protocol
  registry: string
  // The image name
  name: string
  version: string
  // The SQL dialect class name
  dialect: string
  // The default image port to access the database
  default_port: integer
  // The driver class name
  driver_class: string
  // The method used by JDBC
  jdbc_method: string
}
```

### #/components/schemas/CreateIdentifierCreatorDto

```ts
{
  // The given name
  firstname?: string
  // The family name
  lastname?: string
  // The affiliation
  affiliation?: string
  // The full name
  creator_name: string
  // The name type
  name_type?: enum[Personal, Organizational]
  // The persistent identifier that identifies the creator unambiguously
  name_identifier?: string
  // The persistent identifier that identifies the affiliation unambiguously
  affiliation_identifier?: string
}
```

### #/components/schemas/CreateIdentifierDescriptionDto

```ts
{
  // The description value
  description: string
  // The language
  language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
  // The type
  type?: enum[Abstract, Methods, SeriesInformation, TableOfContents, TechnicalInfo, Other]
}
```

### #/components/schemas/CreateIdentifierDto

```ts
{
  // The identifier type
  type: enum[database, subset, table, view]
  // The doi persistent identifier with optional https://doi.org/ prefix
  doi?: string
  titles: {
    // The title
    title: string
    // The language
    language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
    // The type
    type?: enum[AlternativeTitle, Subtitle, TranslatedTitle, Other]
  }[]
  descriptions: {
    // The description value
    description: string
    // The language
    language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
    // The type
    type?: enum[Abstract, Methods, SeriesInformation, TableOfContents, TechnicalInfo, Other]
  }[]
  funders: {
    // The funder name
    funder_name: string
    // The identifier that identifies the funder unambiguously
    funder_identifier?: string
    // The funder type, when the `funder_identifier` is a DOI, the `funder_identifier_type` field must be `Crossref Funder ID`
    funder_identifier_type?: enum[Crossref Funder ID, ROR, GND, ISNI, Other]
    // The scheme URI of the `funder_identifier`
    scheme_uri?: string
    // The award number
    award_number?: string
    // The award title
    award_title?: string
  }[]
  licenses: {
    // The id
    identifier: string
    // The link to the license such as an SPDX identifier
    uri: string
    // A user-friendly short abstract of key details of the license
    description?: string
  }[]
  // The publisher
  publisher: string
  // The language
  language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
  creators: {
    // The given name
    firstname?: string
    // The family name
    lastname?: string
    // The affiliation
    affiliation?: string
    // The full name
    creator_name: string
    // The name type
    name_type?: enum[Personal, Organizational]
    // The persistent identifier that identifies the creator unambiguously
    name_identifier?: string
    // The persistent identifier that identifies the affiliation unambiguously
    affiliation_identifier?: string
  }[]
  // The id
  database_id: string
  // The subset id, is only set when type=`subset`
  query_id?: string
  // The view id, is only set when type=`view`
  view_id?: string
  // The table id, is only set when type=`table`
  table_id?: string
  // The day of publication
  publication_day?: integer
  // The month of publication
  publication_month?: integer
  // The year of publication
  publication_year: integer
  related_identifiers: {
    value: string
    type: enum[DOI, URL, URN, ARK, arXiv, bibcode, EAN13, EISSN, Handle, IGSN, ISBN, ISTC, LISSN, LSID, PMID, PURL, UPC, w3id]
    relation: enum[IsCitedBy, Cites, IsSupplementTo, IsSupplementedBy, IsContinuedBy, Continues, IsDescribedBy, Describes, HasMetadata, IsMetadataFor, HasVersion, IsVersionOf, IsNewVersionOf, IsPreviousVersionOf, IsPartOf, HasPart, IsPublishedIn, IsReferencedBy, References, IsDocumentedBy, Documents, IsCompiledBy, Compiles, IsVariantFormOf, IsOriginalFormOf, IsIdenticalTo, IsReviewedBy, Reviews, IsDerivedFrom, IsSourceOf, IsRequiredBy, Requires, IsObsoletedBy, Obsoletes]
  }[]
}
```

### #/components/schemas/CreateIdentifierFunderDto

```ts
{
  // The funder name
  funder_name: string
  // The identifier that identifies the funder unambiguously
  funder_identifier?: string
  // The funder type, when the `funder_identifier` is a DOI, the `funder_identifier_type` field must be `Crossref Funder ID`
  funder_identifier_type?: enum[Crossref Funder ID, ROR, GND, ISNI, Other]
  // The scheme URI of the `funder_identifier`
  scheme_uri?: string
  // The award number
  award_number?: string
  // The award title
  award_title?: string
}
```

### #/components/schemas/CreateIdentifierTitleDto

```ts
{
  // The title
  title: string
  // The language
  language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
  // The type
  type?: enum[AlternativeTitle, Subtitle, TranslatedTitle, Other]
}
```

### #/components/schemas/CreateRelatedIdentifierDto

```ts
{
  value: string
  type: enum[DOI, URL, URN, ARK, arXiv, bibcode, EAN13, EISSN, Handle, IGSN, ISBN, ISTC, LISSN, LSID, PMID, PURL, UPC, w3id]
  relation: enum[IsCitedBy, Cites, IsSupplementTo, IsSupplementedBy, IsContinuedBy, Continues, IsDescribedBy, Describes, HasMetadata, IsMetadataFor, HasVersion, IsVersionOf, IsNewVersionOf, IsPreviousVersionOf, IsPartOf, HasPart, IsPublishedIn, IsReferencedBy, References, IsDocumentedBy, Documents, IsCompiledBy, Compiles, IsVariantFormOf, IsOriginalFormOf, IsIdenticalTo, IsReviewedBy, Reviews, IsDerivedFrom, IsSourceOf, IsRequiredBy, Requires, IsObsoletedBy, Obsoletes]
}
```

### #/components/schemas/CreateDatabaseDto

```ts
{
  // The name
  name: string
  // The container id
  container_id: string
  // The visibility; if true, The will be displayed publicly and is searchable
  is_public: boolean
  // The insights; if true, The schema will be displayed publicly and is searchable
  is_schema_public: boolean
}
```

### #/components/schemas/CreateViewDto

```ts
{
  // The name
  name: string
  query: {
    columns: {
      // The id of the column
      id: string
      // The column alias
      alias?: string
    }[]
    joins: {
      // The type of join
      type: enum[inner, left, right, cross]
      conditionals: {
        // The id of the column
        column_id: string
        // The id of the foreign column
        foreign_column_id: string
      }[]
      // The id of the data source
      datasource_id: string
    }[]
    filters: {
      // The filter type
      type: enum[where, or, and]
      // The filter value
      value?: string
      // The column id
      column_id: string
      // The operator id
      operator_id: string
    }[]
    orders: {
      // The sort direction
      direction?: enum[asc, desc]
      // The column id
      column_id: string
    }[]
    datasource_ids?: string[]
  }
  // The visibility; if true, The will be displayed publicly and is searchable
  is_public: boolean
  // The insights; if true, The schema will be displayed publicly and is searchable
  is_schema_public: boolean
}
```

### #/components/schemas/CreateForeignKeyDto

```ts
{
  columns?: string[]
  // The name of the foreign table
  referenced_table: string
  referenced_columns?: string[]
  // The integrity action when updating tuples
  on_update?: enum[restrict, cascade, set_null, no_action, set_default]
  // The integrity action when deleting tuples
  on_delete?: enum[restrict, cascade, set_null, no_action, set_default]
}
```

### #/components/schemas/CreateTableColumnDto

```ts
{
  // The column name
  name: string
  // The data type
  type: enum[char, varchar, binary, varbinary, tinyblob, tinytext, text, blob, mediumtext, mediumblob, longtext, longblob, enum, set, serial, bit, tinyint, bool, smallint, mediumint, int, bigint, float, double, decimal, date, datetime, timestamp, time, year]
  // The size determines the number of digits before the comma: x=size-d where size >= d
  size?: integer
  // The digits behind the comma
  d?: integer
  // The column comment
  description?: string
  enums?: string[]
  sets?: string[]
  // The index length
  index_length?: integer
  // If set to true, the column value can be null
  null_allowed: boolean
  // The column concept
  concept_uri?: string
  // The column unit
  unit_uri?: string
}
```

### #/components/schemas/CreateTableConstraintsDto

```ts
{
  uniques?: string[][]
  checks?: string[]
  foreign_keys: {
    columns?: string[]
    // The name of the foreign table
    referenced_table: string
    referenced_columns?: string[]
    // The integrity action when updating tuples
    on_update?: enum[restrict, cascade, set_null, no_action, set_default]
    // The integrity action when deleting tuples
    on_delete?: enum[restrict, cascade, set_null, no_action, set_default]
  }[]
  primary_key?: string[]
}
```

### #/components/schemas/CreateTableDto

```ts
{
  // The table name
  name: string
  // The table comment
  description?: string
  columns: {
    // The column name
    name: string
    // The data type
    type: enum[char, varchar, binary, varbinary, tinyblob, tinytext, text, blob, mediumtext, mediumblob, longtext, longblob, enum, set, serial, bit, tinyint, bool, smallint, mediumint, int, bigint, float, double, decimal, date, datetime, timestamp, time, year]
    // The size determines the number of digits before the comma: x=size-d where size >= d
    size?: integer
    // The digits behind the comma
    d?: integer
    // The column comment
    description?: string
    enums?: string[]
    sets?: string[]
    // The index length
    index_length?: integer
    // If set to true, the column value can be null
    null_allowed: boolean
    // The column concept
    concept_uri?: string
    // The column unit
    unit_uri?: string
  }[]
  constraints: {
    uniques?: string[][]
    checks?: string[]
    foreign_keys: {
      columns?: string[]
      // The name of the foreign table
      referenced_table: string
      referenced_columns?: string[]
      // The integrity action when updating tuples
      on_update?: enum[restrict, cascade, set_null, no_action, set_default]
      // The integrity action when deleting tuples
      on_delete?: enum[restrict, cascade, set_null, no_action, set_default]
    }[]
    primary_key?: string[]
  }
  // The table visibility; if true, the table will be displayed publicly and is searchable
  is_public: boolean
  // The table insights; if true, the table schema will be displayed publicly and is searchable
  is_schema_public: boolean
}
```

### #/components/schemas/CreateContainerDto

```ts
{
  // The user-friendly container name
  name: string
  // The container hostname
  host: string
  // The container port
  port: integer
  quota: integer
  // The image id used for the container database engine
  image_id: string
  // The username of the privileged user
  privileged_username: string
  // The password of the privileged user
  privileged_password: string
}
```

### #/components/schemas/ContainerDto

```ts
{
  // The container id
  id: string
  // The user-friendly container name
  name: string
  image: {
    // The image id
    id: string
    // The image name
    name: string
    version: string
    operators: {
      // The operator id
      id: string
      // The machine-friendly name of the operator
      value: string
      // The documentation link
      documentation: string
      // The user-friendly name of the operator
      display_name: string
    }[]
    default: boolean
    data_types: {
      // The id of the data type
      id: string
      // The machine-friendly value of the data type
      value: string
      // The documentation link
      documentation: string
      // The human-friendly name of the data type
      display_name: string
      // The minimum size
      size_min?: integer
      // The maximum size
      size_max?: integer
      // The default size
      size_default?: integer
      // If true, the size parameter cannot be empty
      size_required?: boolean
      // The step increment
      size_step?: integer
      // The minimum d
      d_min?: integer
      // The maximum d
      d_max?: integer
      // The default d
      d_default?: integer
      // If true, the d parameter cannot be empty
      d_required?: boolean
      // The d increment
      d_step?: integer
      // The user-friendly description of the data format
      data_hint?: string
      // The user-friendly description of the data type
      type_hint?: string
      // frontend needs to quote this data type
      is_quoted: boolean
      // frontend can build this data type
      is_buildable: boolean
    }[]
  }
  // The number of databases the container is capable to hold simultaneously, if null the container has no limit
  quota: integer
  // The number of databases currently in the container
  count: integer
  // The machine-friendly container name
  internal_name: string
}
```

### #/components/schemas/ApiErrorDto

```ts
{
  status: enum[100 CONTINUE, 101 SWITCHING_PROTOCOLS, 102 PROCESSING, 103 EARLY_HINTS, 103 CHECKPOINT, 200 OK, 201 CREATED, 202 ACCEPTED, 203 NON_AUTHORITATIVE_INFORMATION, 204 NO_CONTENT, 205 RESET_CONTENT, 206 PARTIAL_CONTENT, 207 MULTI_STATUS, 208 ALREADY_REPORTED, 226 IM_USED, 300 MULTIPLE_CHOICES, 301 MOVED_PERMANENTLY, 302 FOUND, 302 MOVED_TEMPORARILY, 303 SEE_OTHER, 304 NOT_MODIFIED, 305 USE_PROXY, 307 TEMPORARY_REDIRECT, 308 PERMANENT_REDIRECT, 400 BAD_REQUEST, 401 UNAUTHORIZED, 402 PAYMENT_REQUIRED, 403 FORBIDDEN, 404 NOT_FOUND, 405 METHOD_NOT_ALLOWED, 406 NOT_ACCEPTABLE, 407 PROXY_AUTHENTICATION_REQUIRED, 408 REQUEST_TIMEOUT, 409 CONFLICT, 410 GONE, 411 LENGTH_REQUIRED, 412 PRECONDITION_FAILED, 413 PAYLOAD_TOO_LARGE, 413 REQUEST_ENTITY_TOO_LARGE, 414 URI_TOO_LONG, 414 REQUEST_URI_TOO_LONG, 415 UNSUPPORTED_MEDIA_TYPE, 416 REQUESTED_RANGE_NOT_SATISFIABLE, 417 EXPECTATION_FAILED, 418 I_AM_A_TEAPOT, 419 INSUFFICIENT_SPACE_ON_RESOURCE, 420 METHOD_FAILURE, 421 DESTINATION_LOCKED, 422 UNPROCESSABLE_ENTITY, 423 LOCKED, 424 FAILED_DEPENDENCY, 425 TOO_EARLY, 426 UPGRADE_REQUIRED, 428 PRECONDITION_REQUIRED, 429 TOO_MANY_REQUESTS, 431 REQUEST_HEADER_FIELDS_TOO_LARGE, 451 UNAVAILABLE_FOR_LEGAL_REASONS, 500 INTERNAL_SERVER_ERROR, 501 NOT_IMPLEMENTED, 502 BAD_GATEWAY, 503 SERVICE_UNAVAILABLE, 504 GATEWAY_TIMEOUT, 505 HTTP_VERSION_NOT_SUPPORTED, 506 VARIANT_ALSO_NEGOTIATES, 507 INSUFFICIENT_STORAGE, 508 LOOP_DETECTED, 509 BANDWIDTH_LIMIT_EXCEEDED, 510 NOT_EXTENDED, 511 NETWORK_AUTHENTICATION_REQUIRED]
  // The error message in English
  message: string
  // The error code used for internationalization of the error messages
  code: string
}
```

### #/components/schemas/OaiListRecordsParameters

```ts
{
  metadataPrefix?: string
  from?: string
  until?: string
  set?: string
  resumptionToken?: string
  fromDate?: string
  untilDate?: string
  parametersString?: string
}
```

### #/components/schemas/BannerMessageDto

```ts
{
  id: string
  type: enum[error, warning, info]
  message: string
  link?: string
  link_text?: string
  display_start?: string
  display_end?: string
}
```

### #/components/schemas/ImageBriefDto

```ts
{
  // The image id
  id: string
  // The image name
  name: string
  // The image tag
  version: string
  // Marks the image as default
  default: boolean
}
```

### #/components/schemas/LdCreatorDto

```ts
{
  // The name
  name: string
  // The unambiguous reference to a person's identifier
  sameAs?: string
  // The firstname
  givenName?: string
  // The lastname
  familyName?: string
  // The type
  @type: string
}
```

### #/components/schemas/LdDatasetDto

```ts
{
  // The name
  name: string
  // The description
  description: string
  // The URL
  url: string
  identifier?: string[]
  license?: string
  creator: {
    // The name
    name: string
    // The unambiguous reference to a person's identifier
    sameAs?: string
    // The firstname
    givenName?: string
    // The lastname
    familyName?: string
    // The type
    @type: string
  }[]
  // The ciration URL
  citation: string
  hasPart: {
    // The name
    name: string
    // The description
    description: string
    // The URL
    url: string
    identifier?: string[]
    license?: string
    creator:#/components/schemas/LdCreatorDto[]
    // The ciration URL
    citation: string
    hasPart:#/components/schemas/LdDatasetDto[]
    // The temporal coverage
    temporalCoverage: string
    // The version
    version: string
    // The context schema URI
    @context: string
    // The type
    @type: string
  }[]
  // The temporal coverage
  temporalCoverage: string
  // The version
  version: string
  // The context schema URI
  @context: string
  // The type
  @type: string
}
```

### #/components/schemas/ViewColumnDto

```ts
{
  // The id
  id: string
  // The user-friendly column name
  name: string
  // The column size, determines the number of digits before the comma as x=size-d where size >= d
  size?: integer
  // The column d
  d?: integer
  description?: string
  enums: {
    // The enum id
    id: string
    // The enum value
    value: string
  }[]
  sets: {
    // The set id
    id: string
    // The set value
    value: string
  }[]
  // The database id
  database_id: string
  // The ordinal position of the colum to order it
  ord: integer
  // The machine-friendly column name
  internal_name: string
  // The length of the index
  index_length?: integer
  // The length of the total data in the table (index + data)
  length?: integer
  // The column type name
  type: enum[char, varchar, binary, varbinary, tinyblob, tinytext, text, blob, mediumtext, mediumblob, longtext, longblob, enum, set, serial, bit, tinyint, bool, smallint, mediumint, int, bigint, float, double, decimal, date, datetime, timestamp, time, year]
  is_null_allowed: boolean
}
```

### #/components/schemas/ViewDto

```ts
{
  // The id
  id: string
  // The user-friendly name
  name: string
  identifiers: {
    id: string
    links: {
      self: string
      data?: string
      self_html: string
      dashboard_html?: string
    }
    type: enum[database, subset, table, view]
    titles: {
      id: string
      title?: string
      language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
      type?: enum[AlternativeTitle, Subtitle, TranslatedTitle, Other]
    }[]
    descriptions: {
      id: string
      description?: string
      language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
      type?: enum[Abstract, Methods, SeriesInformation, TableOfContents, TechnicalInfo, Other]
    }[]
    funders: {
      id: string
      funder_name: string
      funder_identifier?: string
      funder_identifier_type?: enum[Crossref Funder ID, ROR, GND, ISNI, Other]
      scheme_uri?: string
      award_number?: string
      award_title?: string
    }[]
    query: string
    execution?: string
    doi?: string
    publisher: string
    owner: {
      id?: string
      // Only contains lowercase characters
      username: string
      name?: string
      orcid?: string
      qualified_name?: string
      given_name?: string
      family_name?: string
    }
    language: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
    licenses: {
      // The id
      identifier: string
      // The link to the license such as an SPDX identifier
      uri: string
      // A user-friendly short abstract of key details of the license
      description?: string
    }[]
    creators: {
      id: string
      firstname?: string
      lastname?: string
      affiliation?: string
      creator_name: string
      name_type?: enum[Personal, Organizational]
      name_identifier?: string
      name_identifier_scheme?: enum[ORCID, ROR, ISNI, GRID]
      name_identifier_scheme_uri?: string
      affiliation_identifier?: string
      affiliation_identifier_scheme?: enum[ROR, GRID, ISNI]
      affiliation_identifier_scheme_uri?: string
    }[]
    status: enum[draft, published]
    created?: string
    database_id: string
    query_id?: string
    table_id?: string
    view_id?: string
    query_normalized: string
    related_identifiers: {
      id: string
      value: string
      type: enum[DOI, URL, URN, ARK, arXiv, bibcode, EAN13, EISSN, Handle, IGSN, ISBN, ISTC, LISSN, LSID, PMID, PURL, UPC, w3id]
      relation: enum[IsCitedBy, Cites, IsSupplementTo, IsSupplementedBy, IsContinuedBy, Continues, IsDescribedBy, Describes, HasMetadata, IsMetadataFor, HasVersion, IsVersionOf, IsNewVersionOf, IsPreviousVersionOf, IsPartOf, HasPart, IsPublishedIn, IsReferencedBy, References, IsDocumentedBy, Documents, IsCompiledBy, Compiles, IsVariantFormOf, IsOriginalFormOf, IsIdenticalTo, IsReviewedBy, Reviews, IsDerivedFrom, IsSourceOf, IsRequiredBy, Requires, IsObsoletedBy, Obsoletes]
    }[]
    // query hash in sha512
    query_hash: string
    result_hash?: string
    result_number?: integer
    publication_day?: integer
    publication_month?: integer
    publication_year: integer
  }[]
  // The SQL statement used to create the view
  query: string
  owner:#/components/schemas/UserBriefDto
  columns: {
    // The id
    id: string
    // The user-friendly column name
    name: string
    // The column size, determines the number of digits before the comma as x=size-d where size >= d
    size?: integer
    // The column d
    d?: integer
    description?: string
    enums: {
      // The enum id
      id: string
      // The enum value
      value: string
    }[]
    sets: {
      // The set id
      id: string
      // The set value
      value: string
    }[]
    // The database id
    database_id: string
    // The ordinal position of the colum to order it
    ord: integer
    // The machine-friendly column name
    internal_name: string
    // The length of the index
    index_length?: integer
    // The length of the total data in the table (index + data)
    length?: integer
    // The column type name
    type: enum[char, varchar, binary, varbinary, tinyblob, tinytext, text, blob, mediumtext, mediumblob, longtext, longblob, enum, set, serial, bit, tinyint, bool, smallint, mediumint, int, bigint, float, double, decimal, date, datetime, timestamp, time, year]
    is_null_allowed: boolean
  }[]
  // The created timestamp
  created: string
  // The database id
  database_id: string
  // The machine-friendly name
  internal_name: string
  // The visibility; if true, The will be displayed publicly and is searchable
  is_public: boolean
  // The insights; if true, The schema will be displayed publicly and is searchable
  is_schema_public: boolean
  // If true, the view is default for the database
  initial_view?: boolean
  // The sha256-hash of the query
  query_hash: string
}
```

### #/components/schemas/ColumnBriefDto

```ts
{
  // The column id
  id: string
  // The column name
  name: string
  // The data source alias name
  alias?: string
  // The database id
  database_id: string
  // The table id
  table_id: string
  // The machine-friendly internal name
  internal_name: string
  // The column type name
  type: enum[char, varchar, binary, varbinary, tinyblob, tinytext, text, blob, mediumtext, mediumblob, longtext, longblob, enum, set, serial, bit, tinyint, bool, smallint, mediumint, int, bigint, float, double, decimal, date, datetime, timestamp, time, year]
}
```

### #/components/schemas/ConstraintsDto

```ts
{
  uniques: {
    // The unique key id
    id: string
    // The unique key name
    name: string
    table: {
      // The id
      id: string
      // The user-friendly table name
      name: string
      // The comment
      description?: string
      // The database id
      database_id: string
      // The machine-friendly table name
      internal_name: string
      // If true, The is using data versioning
      is_versioned: boolean
      // The visibility; if true, The will be displayed publicly and is searchable
      is_public: boolean
      // The insights; if true, The schema will be displayed publicly and is searchable
      is_schema_public: boolean
      // The owner username
      owned_by: string
    }
    columns: {
      // The column id
      id: string
      // The column name
      name: string
      // The data source alias name
      alias?: string
      // The database id
      database_id: string
      // The table id
      table_id: string
      // The machine-friendly internal name
      internal_name: string
      // The column type name
      type: enum[char, varchar, binary, varbinary, tinyblob, tinytext, text, blob, mediumtext, mediumblob, longtext, longblob, enum, set, serial, bit, tinyint, bool, smallint, mediumint, int, bigint, float, double, decimal, date, datetime, timestamp, time, year]
    }[]
  }[]
  checks?: string[]
  foreign_keys: {
    // The foreign key id
    id?: string
    // The foreign key name
    name: string
    references: {
      // The foreign key reference id
      id?: string
      column:#/components/schemas/ColumnBriefDto
      foreign_key: {
        // The foreign key id
        id?: string
      }
      referenced_column:#/components/schemas/ColumnBriefDto
    }[]
    table:#/components/schemas/TableBriefDto
    referenced_table:#/components/schemas/TableBriefDto
    // The integrity action when updating tuples
    on_update?: enum[restrict, cascade, set_null, no_action, set_default]
    // The integrity action when deleting tuples
    on_delete?: enum[restrict, cascade, set_null, no_action, set_default]
  }[]
  primary_key: {
    // The primary key id
    id?: string
    table:#/components/schemas/TableBriefDto
    column:#/components/schemas/ColumnBriefDto
  }[]
}
```

### #/components/schemas/ForeignKeyBriefDto

```ts
{
  // The foreign key id
  id?: string
}
```

### #/components/schemas/ForeignKeyDto

```ts
{
  // The foreign key id
  id?: string
  // The foreign key name
  name: string
  references: {
    // The foreign key reference id
    id?: string
    column: {
      // The column id
      id: string
      // The column name
      name: string
      // The data source alias name
      alias?: string
      // The database id
      database_id: string
      // The table id
      table_id: string
      // The machine-friendly internal name
      internal_name: string
      // The column type name
      type: enum[char, varchar, binary, varbinary, tinyblob, tinytext, text, blob, mediumtext, mediumblob, longtext, longblob, enum, set, serial, bit, tinyint, bool, smallint, mediumint, int, bigint, float, double, decimal, date, datetime, timestamp, time, year]
    }
    foreign_key: {
      // The foreign key id
      id?: string
    }
    referenced_column:#/components/schemas/ColumnBriefDto
  }[]
  table: {
    // The id
    id: string
    // The user-friendly table name
    name: string
    // The comment
    description?: string
    // The database id
    database_id: string
    // The machine-friendly table name
    internal_name: string
    // If true, The is using data versioning
    is_versioned: boolean
    // The visibility; if true, The will be displayed publicly and is searchable
    is_public: boolean
    // The insights; if true, The schema will be displayed publicly and is searchable
    is_schema_public: boolean
    // The owner username
    owned_by: string
  }
  referenced_table:#/components/schemas/TableBriefDto
  // The integrity action when updating tuples
  on_update?: enum[restrict, cascade, set_null, no_action, set_default]
  // The integrity action when deleting tuples
  on_delete?: enum[restrict, cascade, set_null, no_action, set_default]
}
```

### #/components/schemas/ForeignKeyReferenceDto

```ts
{
  // The foreign key reference id
  id?: string
  column: {
    // The column id
    id: string
    // The column name
    name: string
    // The data source alias name
    alias?: string
    // The database id
    database_id: string
    // The table id
    table_id: string
    // The machine-friendly internal name
    internal_name: string
    // The column type name
    type: enum[char, varchar, binary, varbinary, tinyblob, tinytext, text, blob, mediumtext, mediumblob, longtext, longblob, enum, set, serial, bit, tinyint, bool, smallint, mediumint, int, bigint, float, double, decimal, date, datetime, timestamp, time, year]
  }
  foreign_key: {
    // The foreign key id
    id?: string
  }
  referenced_column:#/components/schemas/ColumnBriefDto
}
```

### #/components/schemas/PrimaryKeyDto

```ts
{
  // The primary key id
  id?: string
  table: {
    // The id
    id: string
    // The user-friendly table name
    name: string
    // The comment
    description?: string
    // The database id
    database_id: string
    // The machine-friendly table name
    internal_name: string
    // If true, The is using data versioning
    is_versioned: boolean
    // The visibility; if true, The will be displayed publicly and is searchable
    is_public: boolean
    // The insights; if true, The schema will be displayed publicly and is searchable
    is_schema_public: boolean
    // The owner username
    owned_by: string
  }
  column: {
    // The column id
    id: string
    // The column name
    name: string
    // The data source alias name
    alias?: string
    // The database id
    database_id: string
    // The table id
    table_id: string
    // The machine-friendly internal name
    internal_name: string
    // The column type name
    type: enum[char, varchar, binary, varbinary, tinyblob, tinytext, text, blob, mediumtext, mediumblob, longtext, longblob, enum, set, serial, bit, tinyint, bool, smallint, mediumint, int, bigint, float, double, decimal, date, datetime, timestamp, time, year]
  }
}
```

### #/components/schemas/TableDto

```ts
{
  // The id
  id: string
  // The user-friendly name
  name: string
  // The comment
  description?: string
  // The alias
  alias?: string
  identifiers: {
    id: string
    links: {
      self: string
      data?: string
      self_html: string
      dashboard_html?: string
    }
    type: enum[database, subset, table, view]
    titles: {
      id: string
      title?: string
      language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
      type?: enum[AlternativeTitle, Subtitle, TranslatedTitle, Other]
    }[]
    descriptions: {
      id: string
      description?: string
      language?: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
      type?: enum[Abstract, Methods, SeriesInformation, TableOfContents, TechnicalInfo, Other]
    }[]
    funders: {
      id: string
      funder_name: string
      funder_identifier?: string
      funder_identifier_type?: enum[Crossref Funder ID, ROR, GND, ISNI, Other]
      scheme_uri?: string
      award_number?: string
      award_title?: string
    }[]
    query: string
    execution?: string
    doi?: string
    publisher: string
    owner: {
      id?: string
      // Only contains lowercase characters
      username: string
      name?: string
      orcid?: string
      qualified_name?: string
      given_name?: string
      family_name?: string
    }
    language: enum[ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, km, ch, ce, ny, zh, cu, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gd, gl, lg, ka, de, ki, el, kl, gn, gu, ht, ha, he, hz, hi, ho, hu, is, io, ig, id, ia, ie, iu, ik, ga, it, ja, jv, kn, kr, ks, kk, rw, kv, kg, ko, kj, ku, ky, lo, la, lv, lb, li, ln, lt, lu, mk, mg, ms, ml, mt, gv, mi, mr, mh, ro, mn, na, nv, nd, ng, ne, se, no, nb, nn, ii, oc, oj, or, om, os, pi, pa, ps, fa, pl, pt, qu, rm, rn, ru, sm, sg, sa, sc, sr, sn, sd, si, sk, sl, so, st, nr, es, su, sw, ss, sv, tl, ty, tg, ta, tt, te, th, bo, ti, to, ts, tn, tr, tk, tw, ug, uk, ur, uz, ve, vi, vo, wa, cy, fy, wo, xh, yi, yo, za, zu]
    licenses: {
      // The id
      identifier: string
      // The link to the license such as an SPDX identifier
      uri: string
      // A user-friendly short abstract of key details of the license
      description?: string
    }[]
    creators: {
      id: string
      firstname?: string
      lastname?: string
      affiliation?: string
      creator_name: string
      name_type?: enum[Personal, Organizational]
      name_identifier?: string
      name_identifier_scheme?: enum[ORCID, ROR, ISNI, GRID]
      name_identifier_scheme_uri?: string
      affiliation_identifier?: string
      affiliation_identifier_scheme?: enum[ROR, GRID, ISNI]
      affiliation_identifier_scheme_uri?: string
    }[]
    status: enum[draft, published]
    created?: string
    database_id: string
    query_id?: string
    table_id?: string
    view_id?: string
    query_normalized: string
    related_identifiers: {
      id: string
      value: string
      type: enum[DOI, URL, URN, ARK, arXiv, bibcode, EAN13, EISSN, Handle, IGSN, ISBN, ISTC, LISSN, LSID, PMID, PURL, UPC, w3id]
      relation: enum[IsCitedBy, Cites, IsSupplementTo, IsSupplementedBy, IsContinuedBy, Continues, IsDescribedBy, Describes, HasMetadata, IsMetadataFor, HasVersion, IsVersionOf, IsNewVersionOf, IsPreviousVersionOf, IsPartOf, HasPart, IsPublishedIn, IsReferencedBy, References, IsDocumentedBy, Documents, IsCompiledBy, Compiles, IsVariantFormOf, IsOriginalFormOf, IsIdenticalTo, IsReviewedBy, Reviews, IsDerivedFrom, IsSourceOf, IsRequiredBy, Requires, IsObsoletedBy, Obsoletes]
    }[]
    // query hash in sha512
    query_hash: string
    result_hash?: string
    result_number?: integer
    publication_day?: integer
    publication_month?: integer
    publication_year: integer
  }[]
  owner:#/components/schemas/UserBriefDto
  columns: {
    // The id
    id: string
    // The user-friendly column name
    name: string
    // The data source alias name
    alias?: string
    // The column size, determines the number of digits before the comma as x=size-d where size >= d
    size?: integer
    // The column d
    d?: integer
    // The statistically average numerical value
    mean?: number
    // The statistically most middle numerical value
    median?: number
    description?: string
    enums: {
      // The enum id
      id: string
      // The enum value
      value: string
    }[]
    sets: {
      // The set id
      id: string
      // The set value
      value: string
    }[]
    // The database id
    database_id: string
    // The table id
    table_id: string
    // The ordinal position of the colum to order it
    ord: integer
    // The machine-friendly column name
    internal_name: string
    // The length of the index
    index_length?: integer
    // The length of the total data in the table (index + data)
    length?: integer
    // The column type name
    type: enum[char, varchar, binary, varbinary, tinyblob, tinytext, text, blob, mediumtext, mediumblob, longtext, longblob, enum, set, serial, bit, tinyint, bool, smallint, mediumint, int, bigint, float, double, decimal, date, datetime, timestamp, time, year]
    // The data length
    data_length?: integer
    // The maximum data length
    max_data_length?: integer
    // The number of rows
    num_rows?: integer
    // The statistically highest numerical value
    val_min?: number
    // The statistically lowest numerical value
    val_max?: number
    // The statistically determined standard deviation
    std_dev?: number
    // The concept URI
    concept_uri?: string
    // The unit URI
    unit_uri?: string
    is_null_allowed: boolean
  }[]
  constraints: {
    uniques: {
      // The unique key id
      id: string
      // The unique key name
      name: string
      table: {
        // The id
        id: string
        // The user-friendly table name
        name: string
        // The comment
        description?: string
        // The database id
        database_id: string
        // The machine-friendly table name
        internal_name: string
        // If true, The is using data versioning
        is_versioned: boolean
        // The visibility; if true, The will be displayed publicly and is searchable
        is_public: boolean
        // The insights; if true, The schema will be displayed publicly and is searchable
        is_schema_public: boolean
        // The owner username
        owned_by: string
      }
      columns: {
        // The column id
        id: string
        // The column name
        name: string
        // The data source alias name
        alias?: string
        // The database id
        database_id: string
        // The table id
        table_id: string
        // The machine-friendly internal name
        internal_name: string
        // The column type name
        type: enum[char, varchar, binary, varbinary, tinyblob, tinytext, text, blob, mediumtext, mediumblob, longtext, longblob, enum, set, serial, bit, tinyint, bool, smallint, mediumint, int, bigint, float, double, decimal, date, datetime, timestamp, time, year]
      }[]
    }[]
    checks?: string[]
    foreign_keys: {
      // The foreign key id
      id?: string
      // The foreign key name
      name: string
      references: {
        // The foreign key reference id
        id?: string
        column:#/components/schemas/ColumnBriefDto
        foreign_key: {
          // The foreign key id
          id?: string
        }
        referenced_column:#/components/schemas/ColumnBriefDto
      }[]
      table:#/components/schemas/TableBriefDto
      referenced_table:#/components/schemas/TableBriefDto
      // The integrity action when updating tuples
      on_update?: enum[restrict, cascade, set_null, no_action, set_default]
      // The integrity action when deleting tuples
      on_delete?: enum[restrict, cascade, set_null, no_action, set_default]
    }[]
    primary_key: {
      // The primary key id
      id?: string
      table:#/components/schemas/TableBriefDto
      column:#/components/schemas/ColumnBriefDto
    }[]
  }
  // The created timestamp
  created: string
  // The database id
  database_id: string
  // The machine-friendly name
  internal_name: string
  // If true, The is using data versioning
  is_versioned: boolean
  // The queue name
  queue_name: string
  // The queue type
  queue_type?: string
  // The routing key
  routing_key: string
  // The visibility; if true, The will be displayed publicly and is searchable
  is_public: boolean
  // The insights; if true, The schema will be displayed publicly and is searchable
  is_schema_public: boolean
  // The statistical number of rows
  num_rows?: integer
  // The data length in bytes
  data_length?: integer
  // The maximum data length in bytes
  max_data_length?: integer
  // The average row length in bytes
  avg_row_length?: integer
}
```

### #/components/schemas/UniqueDto

```ts
{
  // The unique key id
  id: string
  // The unique key name
  name: string
  table: {
    // The id
    id: string
    // The user-friendly table name
    name: string
    // The comment
    description?: string
    // The database id
    database_id: string
    // The machine-friendly table name
    internal_name: string
    // If true, The is using data versioning
    is_versioned: boolean
    // The visibility; if true, The will be displayed publicly and is searchable
    is_public: boolean
    // The insights; if true, The schema will be displayed publicly and is searchable
    is_schema_public: boolean
    // The owner username
    owned_by: string
  }
  columns: {
    // The column id
    id: string
    // The column name
    name: string
    // The data source alias name
    alias?: string
    // The database id
    database_id: string
    // The table id
    table_id: string
    // The machine-friendly internal name
    internal_name: string
    // The column type name
    type: enum[char, varchar, binary, varbinary, tinyblob, tinytext, text, blob, mediumtext, mediumblob, longtext, longblob, enum, set, serial, bit, tinyint, bool, smallint, mediumint, int, bigint, float, double, decimal, date, datetime, timestamp, time, year]
  }[]
}
```

### #/components/schemas/ContainerBriefDto

```ts
{
  // The container id
  id: string
  // The container hash
  hash: string
  // The user-friendly container name
  name: string
  image: {
    // The image id
    id: string
    // The image name
    name: string
    // The image tag
    version: string
    // Marks the image as default
    default: boolean
  }
  // The number of databases the container is capable to hold simultaneously, if null the container has no limit
  quota: integer
  // The number of databases currently in the container
  count: integer
  // The machine-friendly container name
  internal_name: string
}
```

### #/components/schemas/ApiError

```ts
{
  code?: string
  message?: string
  status?: string
}
```

### #/components/schemas/IndexDto

```ts
{
  "properties": {
    "results": {
      "items": {
        "type": "object"
      },
      "type": "array"
    },
    "type": {
      "description": "Same as the requested type",
      "enum": [
        "database",
        "table",
        "view",
        "column",
        "user",
        "identifier",
        "concept",
        "unit"
      ],
      "type": "string"
    }
  },
  "required": [
    "results",
    "type"
  ]
}
```

### #/components/schemas/IndexFieldDto

```ts
{
  attr_friendly_name: string
  attr_name: string
  // OpenSearch data types.
  type: string
}
```

### #/components/schemas/IndexFieldsDto

```ts
{
  results: {
    attr_friendly_name: string
    attr_name: string
    // OpenSearch data types.
    type: string
  }[]
}
```

### #/components/schemas/SearchRequestDto

```ts
{
  field_value_pairs: {
  }
  search_term: string
}
```
