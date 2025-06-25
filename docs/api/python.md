---
author: Martin Weise
---

The REST API manages all of DBRepo. This documentation is also available as [Sphinx UI](/infrastructures/dbrepo/python/).

Ensure that you use the same Python library version as the target instance. For example: if you see `1.9.2` in the
bottom left, you need to use the `1.9.2` Python library.

# Python API

### *class* dbrepo.RestClient.RestClient(endpoint: str = 'http://localhost', username: str = None, password: str = None, secure: bool = True)

The RestClient class for communicating with the DBRepo REST API. All parameters can be set also via environment     variables, e.g. set endpoint with REST_API_ENDPOINT, username with REST_API_USERNAME, etc. You can override     the constructor parameters with the environment variables.

* **Parameters:**
  * **endpoint** – The REST API endpoint. Optional. Default: http://gateway-service
  * **username** – The REST API username. Optional.
  * **password** – The REST API password. Optional.
  * **secure** – When set to false, the requests library will not verify the authenticity of your TLS/SSL
    certificates (i.e. when using self-signed certificates). Default: True.

#### analyse_datatypes(dataframe: DataFrame, enum: bool = None, enum_tol: int = None) → [DatatypeAnalysis](../source/dbrepo.api.md#dbrepo.api.dto.DatatypeAnalysis)

Import a csv dataset from a file and analyse it for the possible enums, line encoding and column data types.

* **Parameters:**
  * **dataframe** – The dataframe.
  * **enum** – If set to True, enumerations should be guessed, otherwise no guessing. Optional.
  * **enum_tol** – The tolerance for guessing enumerations (ignored if enum=False). Optional.
* **Returns:**
  The determined data types, if successful.
* **Raises:**
  * [**MalformedError**](../source/dbrepo.api.md#dbrepo.api.exceptions.MalformedError) – If the payload is rejected by the service.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the file was not found by the Analyse Service.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the analysis.

#### analyse_keys(dataframe: DataFrame) → [KeyAnalysis](../source/dbrepo.api.md#dbrepo.api.dto.KeyAnalysis)

Import a csv dataset from a file and analyse it for the possible primary key.

* **Parameters:**
  **dataframe** – The dataframe.
* **Returns:**
  The determined ranking of the primary key candidates, if successful.
* **Raises:**
  * [**MalformedError**](../source/dbrepo.api.md#dbrepo.api.exceptions.MalformedError) – If the payload is rejected by the service.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the file was not found by the Analyse Service.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the analysis.

#### analyse_table_statistics(database_id: str, table_id: str) → [TableStatistics](../source/dbrepo.api.md#dbrepo.api.dto.TableStatistics)

Analyses the numerical contents of a table in a database with given database id and table id.

* **Parameters:**
  * **database_id** – The database id.
  * **table_id** – The table id.
* **Returns:**
  The table statistics, if successful.
* **Raises:**
  * [**MalformedError**](../source/dbrepo.api.md#dbrepo.api.exceptions.MalformedError) – If the payload is rejected by the service.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the file was not found by the Analyse Service.
  * [**ServiceConnectionError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceConnectionError) – If something went wrong with connection to the metadata service.
  * [**ServiceError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceError) – If something went wrong with obtaining the information in the search service.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the analysis.

#### create_container(name: str, host: str, image_id: str, privileged_username: str, privileged_password: str, port: int = None, ui_host: str = None, ui_port: int = None) → [Container](../source/dbrepo.api.md#dbrepo.api.dto.Container)

Register a container instance executing a given container image. Note that this does not create a container,
but only saves it in the metadata database to be used within DBRepo. The container still needs to be created
through e.g. docker run image:tag -d.

* **Parameters:**
  * **name** – The container name.
  * **host** – The container hostname.
  * **image_id** – The container image id.
  * **privileged_username** – The container privileged user username.
  * **privileged_password** – The container privileged user password.
  * **port** – The container port bound to the host. Optional.
  * **ui_host** – The container hostname displayed in the user interface. Optional. Default: value of host.
  * **ui_port** – The container port displayed in the user interface. Optional. Default: default_port of image.
* **Returns:**
  The container, if successful.
* **Raises:**
  * [**MalformedError**](../source/dbrepo.api.md#dbrepo.api.exceptions.MalformedError) – If the payload was rejected by the service.
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the container does not exist.
  * [**NameExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NameExistsError) – If a container with this name already exists.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the retrieval.

#### create_database(name: str, container_id: str, is_public: bool = True, is_schema_public: bool = True) → [Database](../source/dbrepo.api.md#dbrepo.api.dto.Database)

Create a databases in a container with given container id.

* **Parameters:**
  * **name** – The name of the database.
  * **container_id** – The container id.
  * **is_public** – The visibility of the data. If set to True the data will be publicly visible. Optional. Default: True.
  * **is_schema_public** – The visibility of the schema metadata. If set to True the schema metadata will be publicly visible. Optional. Default: True.
* **Returns:**
  The database, if successful.
* **Raises:**
  * [**MalformedError**](../source/dbrepo.api.md#dbrepo.api.exceptions.MalformedError) – If the payload was rejected by the service.
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the container does not exist.
  * [**QueryStoreError**](../source/dbrepo.api.md#dbrepo.api.exceptions.QueryStoreError) – If something went wrong with the query store.
  * [**ServiceConnectionError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceConnectionError) – If something went wrong with connection to the search service.
  * [**ServiceError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceError) – If something went wrong with obtaining the information in the search service.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the retrieval.

#### create_database_access(database_id: str, user_id: str, type: [AccessType](../source/dbrepo.api.md#dbrepo.api.dto.AccessType)) → [AccessType](../source/dbrepo.api.md#dbrepo.api.dto.AccessType)

Create access to a database with given database id and user id.

* **Parameters:**
  * **database_id** – The database id.
  * **user_id** – The user id.
  * **type** – The access type.
* **Returns:**
  The access type, if successful.
* **Raises:**
  * [**MalformedError**](../source/dbrepo.api.md#dbrepo.api.exceptions.MalformedError) – If the payload is rejected by the service.
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the database or user does not exist.
  * [**ServiceConnectionError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceConnectionError) – If something went wrong with connection to the data service.
  * [**ServiceError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceError) – If something went wrong with obtaining the information in the data service.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the retrieval.

#### create_identifier(database_id: str, type: [IdentifierType](../source/dbrepo.api.md#dbrepo.api.dto.IdentifierType), titles: List[[CreateIdentifierTitle](../source/dbrepo.api.md#dbrepo.api.dto.CreateIdentifierTitle)], publisher: str, creators: List[[CreateIdentifierCreator](../source/dbrepo.api.md#dbrepo.api.dto.CreateIdentifierCreator)], publication_year: int, descriptions: List[[CreateIdentifierDescription](../source/dbrepo.api.md#dbrepo.api.dto.CreateIdentifierDescription)] = None, funders: List[[CreateIdentifierFunder](../source/dbrepo.api.md#dbrepo.api.dto.CreateIdentifierFunder)] = None, licenses: List[[License](../source/dbrepo.api.md#dbrepo.api.dto.License)] = None, language: [Language](../source/dbrepo.api.md#dbrepo.api.dto.Language) = None, subset_id: str = None, view_id: str = None, table_id: str = None, publication_day: int = None, publication_month: int = None, related_identifiers: List[[CreateRelatedIdentifier](../source/dbrepo.api.md#dbrepo.api.dto.CreateRelatedIdentifier)] = None) → [Identifier](../source/dbrepo.api.md#dbrepo.api.dto.Identifier)

Create an identifier draft.

* **Parameters:**
  * **database_id** – The database id of the created identifier.
  * **type** – The type of the created identifier.
  * **titles** – The titles of the created identifier.
  * **publisher** – The publisher of the created identifier.
  * **creators** – The creator(s) of the created identifier.
  * **publication_year** – The publication year of the created identifier.
  * **descriptions** – The description(s) of the created identifier. Optional.
  * **funders** – The funders(s) of the created identifier. Optional.
  * **licenses** – The license(s) of the created identifier. Optional.
  * **language** – The language of the created identifier. Optional.
  * **subset_id** – The subset id of the created identifier. Required when type=SUBSET, otherwise invalid. Optional.
  * **view_id** – The view id of the created identifier. Required when type=VIEW, otherwise invalid. Optional.
  * **table_id** – The table id of the created identifier. Required when type=TABLE, otherwise invalid. Optional.
  * **publication_day** – The publication day of the created identifier. Optional.
  * **publication_month** – The publication month of the created identifier. Optional.
  * **related_identifiers** – The related identifier(s) of the created identifier. Optional.
* **Returns:**
  The identifier, if successful.
* **Raises:**
  * [**MalformedError**](../source/dbrepo.api.md#dbrepo.api.exceptions.MalformedError) – If the payload is rejected by the service.
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the database, table/view/subset or user does not exist.
  * [**ServiceConnectionError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceConnectionError) – If something went wrong with connection to the search service.
  * [**ServiceError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceError) – If something went wrong with obtaining the information in the search service.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the creation of the identifier.

#### create_subset(database_id: str, query: [QueryDefinition](../source/dbrepo.api.md#dbrepo.api.dto.QueryDefinition), page: int = 0, size: int = 10, timestamp: datetime = None) → DataFrame

Executes a SQL query in a database where the current user has at least read access with given database id. The
result set can be paginated with setting page and size (both). Historic data can be queried by setting
timestamp.

* **Parameters:**
  * **database_id** – The database id.
  * **query** – The query definition.
  * **page** – The result pagination number. Optional. Default: 0.
  * **size** – The result pagination size. Optional. Default: 10.
  * **timestamp** – The timestamp at which the data validity is set. Optional. Default: <current timestamp>.
* **Returns:**
  The result set, if successful.
* **Raises:**
  * [**MalformedError**](../source/dbrepo.api.md#dbrepo.api.exceptions.MalformedError) – If the payload is rejected by the service.
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the database, table or user does not exist.
  * [**QueryStoreError**](../source/dbrepo.api.md#dbrepo.api.exceptions.QueryStoreError) – The query store rejected the query.
  * [**FormatNotAvailable**](../source/dbrepo.api.md#dbrepo.api.exceptions.FormatNotAvailable) – The subset query contains non-supported keywords.
  * [**ServiceError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceError) – If something went wrong with obtaining the information in the data service.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the retrieval.

#### create_table(database_id: str, name: str, is_public: bool, is_schema_public: bool, dataframe: DataFrame, description: str = None, with_data: bool = True) → [TableBrief](../source/dbrepo.api.md#dbrepo.api.dto.TableBrief)

Updates the database owner of a database with given database id.

* **Parameters:**
  * **database_id** – The database id.
  * **name** – The name of the created table.
  * **is_public** – The visibility of the data. If set to True the data will be publicly visible.
  * **is_schema_public** – The visibility of the schema metadata. If set to True the schema metadata will be publicly visible.
  * **dataframe** – The pandas dataframe.
  * **description** – The description of the created table. Optional.
  * **with_data** – If set to True, the data will be included in the new table. Optional. Default: True.
* **Returns:**
  The table, if successful.
* **Raises:**
  * [**MalformedError**](../source/dbrepo.api.md#dbrepo.api.exceptions.MalformedError) – If the payload is rejected by the service.
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the database does not exist.
  * [**NameExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NameExistsError) – If a table with this name already exists.
  * [**ServiceConnectionError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceConnectionError) – If something went wrong with connection to the data service.
  * [**ServiceError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceError) – If something went wrong with obtaining the information in the data service.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the creation.

#### create_table_data(database_id: str, table_id: str, data: dict) → None

Insert data into a table in a database with given database id and table id.

* **Parameters:**
  * **database_id** – The database id.
  * **table_id** – The table id.
  * **data** – The data dictionary to be inserted into the table with the form column=value of the table.
* **Raises:**
  * [**MalformedError**](../source/dbrepo.api.md#dbrepo.api.exceptions.MalformedError) – If the payload is rejected by the service (e.g. LOB could not be imported).
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the table does not exist.
  * [**ServiceError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceError) – If something went wrong with obtaining the information in the metadata service.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the insert.

#### create_view(database_id: str, name: str, query: [QueryDefinition](../source/dbrepo.api.md#dbrepo.api.dto.QueryDefinition), is_public: bool, is_schema_public: bool) → [ViewBrief](../source/dbrepo.api.md#dbrepo.api.dto.ViewBrief)

Create a view in a database with given database id.

* **Parameters:**
  * **database_id** – The database id.
  * **name** – The name of the created view.
  * **query** – The query definition of the view.
  * **is_public** – The visibility of the data. If set to True the data will be publicly visible. Optional. Default: True.
  * **is_schema_public** – The visibility of the schema metadata. If set to True the schema metadata will be publicly visible. Optional. Default: True.
* **Returns:**
  The created view, if successful.
* **Raises:**
  * [**MalformedError**](../source/dbrepo.api.md#dbrepo.api.exceptions.MalformedError) – If the payload is rejected by the service.
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the database does not exist.
  * [**ExternalSystemError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ExternalSystemError) – If the mapped view creation query is erroneous.
  * [**ServiceConnectionError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceConnectionError) – If something went wrong with connection to the search service.
  * [**ServiceError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceError) – If something went wrong with obtaining the information in the search service.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the retrieval.

#### delete_container(container_id: str) → None

Deletes a container with given id. Note that this does not delete the container, but deletes the entry in the
metadata database. The container still needs to be removed, e.g. docker container stop hash and then
docker container rm hash.

* **Parameters:**
  **container_id** – The container id.
* **Raises:**
  * [**MalformedError**](../source/dbrepo.api.md#dbrepo.api.exceptions.MalformedError) – If the payload is rejected by the service.
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the container does not exist.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the deletion.

#### delete_database_access(database_id: str, user_id: str) → None

Deletes the access for a user to a database with given database id and user id.

* **Parameters:**
  * **database_id** – The database id.
  * **user_id** – The user id.
* **Raises:**
  * [**MalformedError**](../source/dbrepo.api.md#dbrepo.api.exceptions.MalformedError) – If the payload is rejected by the service.
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the database or user does not exist.
  * [**ServiceConnectionError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceConnectionError) – If something went wrong with connection to the data service.
  * [**ServiceError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceError) – If something went wrong with obtaining the information in the data service.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the retrieval.

#### delete_table(database_id: str, table_id: str) → None

Delete a table with given database id and table id.

* **Parameters:**
  * **database_id** – The database id.
  * **table_id** – The table id.
* **Raises:**
  * [**MalformedError**](../source/dbrepo.api.md#dbrepo.api.exceptions.MalformedError) – If the payload is rejected by the service.
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the container does not exist.
  * [**ServiceConnectionError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceConnectionError) – If something went wrong with connection to the data service.
  * [**ServiceError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceError) – If something went wrong with obtaining the information in the data service.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the deletion.

#### delete_table_data(database_id: str, table_id: str, keys: dict) → None

Delete data in a table in a database with given database id and table id.

* **Parameters:**
  * **database_id** – The database id.
  * **table_id** – The table id.
  * **keys** – The key dictionary matching the rows in the form column=value.
* **Raises:**
  * [**MalformedError**](../source/dbrepo.api.md#dbrepo.api.exceptions.MalformedError) – If the payload is rejected by the service.
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the table does not exist.
  * [**ServiceError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceError) – If something went wrong with obtaining the information in the metadata service.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the deletion.

#### delete_view(database_id: str, view_id: str) → None

Deletes a view in a database with given database id and view id.

* **Parameters:**
  * **database_id** – The database id.
  * **view_id** – The view id.
* **Raises:**
  * [**MalformedError**](../source/dbrepo.api.md#dbrepo.api.exceptions.MalformedError) – If the payload is rejected by the service.
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the container does not exist.
  * [**ExternalSystemError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ExternalSystemError) – If the mapped view deletion query is erroneous.
  * [**ServiceConnectionError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceConnectionError) – If something went wrong with connection to the search service.
  * [**ServiceError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceError) – If something went wrong with obtaining the information in the search service.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the deletion.

#### get_concepts() → List[[ConceptBrief](../source/dbrepo.api.md#dbrepo.api.dto.ConceptBrief)]

Get list of concepts known to the metadata database.

* **Returns:**
  List of concepts, if successful.

#### get_container(container_id: str) → [Container](../source/dbrepo.api.md#dbrepo.api.dto.Container)

Get a container with given id.

* **Returns:**
  List of containers, if successful.
* **Raises:**
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the container does not exist.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the retrieval.

#### get_containers() → List[[ContainerBrief](../source/dbrepo.api.md#dbrepo.api.dto.ContainerBrief)]

Get all containers.

* **Returns:**
  List of containers, if successful.
* **Raises:**
  [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the retrieval.

#### get_database(database_id: str) → [Database](../source/dbrepo.api.md#dbrepo.api.dto.Database)

Get a databases with given id.

* **Parameters:**
  **database_id** – The database id.
* **Returns:**
  The database, if successful.
* **Raises:**
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the container does not exist.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the retrieval.

#### get_database_access(database_id: str) → [AccessType](../source/dbrepo.api.md#dbrepo.api.dto.AccessType)

Get access of a view in a database with given database id and view id.

* **Parameters:**
  **database_id** – The database id.
* **Returns:**
  The access type, if successful.
* **Raises:**
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the container does not exist.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the retrieval.

#### get_databases() → List[[DatabaseBrief](../source/dbrepo.api.md#dbrepo.api.dto.DatabaseBrief)]

Get all databases.

* **Returns:**
  List of databases, if successful.
* **Raises:**
  [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the retrieval.

#### get_databases_count() → int

Count all databases.

* **Returns:**
  Count of databases if successful.
* **Raises:**
  [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the retrieval.

#### get_identifier(identifier_id: str) → [Identifier](../source/dbrepo.api.md#dbrepo.api.dto.Identifier)

Get the identifier by given id.

* **Parameters:**
  **identifier_id** – The identifier id.
* **Returns:**
  The identifier, if successful.
* **Raises:**
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the identifier does not exist.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the retrieval of the identifier.

#### get_identifier_data(identifier_id: str, page: int = 0, size: int = 10000) → DataFrame

Get the identifier data by given id.

* **Parameters:**
  * **identifier_id** – The identifier id.
  * **page** – The result pagination number. Optional. Default: 0.
  * **size** – The result pagination size. Optional. Default: 10000.
* **Returns:**
  The identifier, if successful.
* **Raises:**
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the identifier does not exist.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the retrieval of the identifier.

#### get_identifiers(database_id: str = None, subset_id: str = None, view_id: str = None, table_id: str = None, type: [IdentifierType](../source/dbrepo.api.md#dbrepo.api.dto.IdentifierType) = None, status: [IdentifierStatusType](../source/dbrepo.api.md#dbrepo.api.dto.IdentifierStatusType) = None) → List[[Identifier](../source/dbrepo.api.md#dbrepo.api.dto.Identifier)]

Get list of identifiers, filter by the remaining optional arguments.

* **Parameters:**
  * **database_id** – The database id. Optional.
  * **subset_id** – The subset id. Optional. Requires database_id to be set.
  * **view_id** – The view id. Optional. Requires database_id to be set.
  * **table_id** – The table id. Optional. Requires database_id to be set.
  * **type** – The identifier type. Optional.
  * **status** – The identifier status. Optional.
* **Returns:**
  List of identifiers, if successful.
* **Raises:**
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the accept header is neither application/json nor application/ld+json.
  * [**FormatNotAvailable**](../source/dbrepo.api.md#dbrepo.api.exceptions.FormatNotAvailable) – If the service could not represent the output.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the retrieval of the identifiers.

#### get_image(image_id: str) → [Image](../source/dbrepo.api.md#dbrepo.api.dto.Image)

Get container image.

* **Parameters:**
  **image_id** – The image id.
* **Returns:**
  The image, if successful.
* **Raises:**
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the image does not exist.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the retrieval of the image.

#### get_images() → List[[ImageBrief](../source/dbrepo.api.md#dbrepo.api.dto.ImageBrief)]

Get list of container images.

* **Returns:**
  List of images, if successful.

#### get_licenses() → List[[License](../source/dbrepo.api.md#dbrepo.api.dto.License)]

Get list of licenses allowed.

* **Returns:**
  List of licenses, if successful.

#### get_messages() → List[[Message](../source/dbrepo.api.md#dbrepo.api.dto.Message)]

Get list of messages.

* **Returns:**
  List of messages, if successful.

#### get_ontologies() → List[[OntologyBrief](../source/dbrepo.api.md#dbrepo.api.dto.OntologyBrief)]

Get list of ontologies.

* **Returns:**
  List of ontologies, if successful.

#### get_queries(database_id: str) → List[[Query](../source/dbrepo.api.md#dbrepo.api.dto.Query)]

Get queries from a database with given database id.

* **Parameters:**
  **database_id** – The database id.
* **Returns:**
  List of queries, if successful.
* **Raises:**
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the database or user does not exist.
  * [**ServiceError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceError) – If something went wrong with obtaining the information in the data service.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the retrieval.

#### get_subset(database_id: str, subset_id: str) → [Query](../source/dbrepo.api.md#dbrepo.api.dto.Query)

Get query from a database with given database id and query id.

* **Parameters:**
  * **database_id** – The database id.
  * **subset_id** – The subset id.
* **Returns:**
  The query, if successful.
* **Raises:**
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the database, query or user does not exist.
  * [**FormatNotAvailable**](../source/dbrepo.api.md#dbrepo.api.exceptions.FormatNotAvailable) – If the service could not represent the output.
  * [**ServiceError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceError) – If something went wrong with obtaining the information in the data service.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the retrieval.

#### get_subset_data(database_id: str, subset_id: str, page: int = 0, size: int = 10) → DataFrame

Re-executes a query in a database with given database id and query id.

* **Parameters:**
  * **database_id** – The database id.
  * **subset_id** – The subset id.
  * **page** – The result pagination number. Optional. Default: 0.
  * **size** – The result pagination size. Optional. Default: 10.
  * **size** – The result pagination size. Optional. Default: 10.
* **Returns:**
  The subset data, if successful.
* **Raises:**
  * [**MalformedError**](../source/dbrepo.api.md#dbrepo.api.exceptions.MalformedError) – If the payload is rejected by the service.
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the database, query or user does not exist.
  * [**ServiceError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceError) – If something went wrong with obtaining the information in the data service.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the retrieval.

#### get_subset_data_count(database_id: str, subset_id: str) → int

Re-executes a query in a database with given database id and query id and only counts the results.

* **Parameters:**
  * **database_id** – The database id.
  * **subset_id** – The subset id.
* **Returns:**
  The result set, if successful.
* **Raises:**
  * [**MalformedError**](../source/dbrepo.api.md#dbrepo.api.exceptions.MalformedError) – If the payload is rejected by the service.
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the database, query or user does not exist.
  * [**ServiceError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceError) – If something went wrong with obtaining the information in the data service.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the retrieval.

#### get_table(database_id: str, table_id: str) → [Table](../source/dbrepo.api.md#dbrepo.api.dto.Table)

Get a table with given database id and table id.

* **Parameters:**
  * **database_id** – The database id.
  * **table_id** – The table id.
* **Returns:**
  List of tables, if successful.
* **Raises:**
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the table does not exist.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the retrieval.

#### get_table_data(database_id: str, table_id: str, page: int = 0, size: int = 10, timestamp: datetime = None) → DataFrame

Get data of a table in a database with given database id and table id.

* **Parameters:**
  * **database_id** – The database id.
  * **table_id** – The table id.
  * **page** – The result pagination number. Optional. Default: 0.
  * **size** – The result pagination size. Optional. Default: 10.
  * **timestamp** – The query execution time. Optional.
* **Returns:**
  The table data, if successful.
* **Raises:**
  * [**MalformedError**](../source/dbrepo.api.md#dbrepo.api.exceptions.MalformedError) – If the payload is rejected by the service.
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the table does not exist.
  * [**ServiceError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceError) – If something went wrong with obtaining the information in the metadata service.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the retrieval.

#### get_table_data_count(database_id: str, table_id: str, timestamp: datetime = None) → int

Get data count of a table in a database with given database id and table id.

* **Parameters:**
  * **database_id** – The database id.
  * **table_id** – The table id.
  * **timestamp** – The query execution time. Optional.
* **Returns:**
  The result of the view query, if successful.
* **Raises:**
  * [**MalformedError**](../source/dbrepo.api.md#dbrepo.api.exceptions.MalformedError) – If the payload is rejected by the service.
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the table does not exist.
  * [**ExternalSystemError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ExternalSystemError) – If the mapped view selection query is erroneous.
  * [**ServiceError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceError) – If something went wrong with obtaining the information in the metadata service.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the retrieval.

#### get_table_history(database_id: str, table_id: str, size: int = 100) → [<class 'dbrepo.api.dto.History'>]

Get the table history of insert/delete operations.

* **Parameters:**
  * **database_id** – The database id.
  * **table_id** – The table id.
  * **size** – The number of operations. Optional. Default: 100.
* **Raises:**
  * [**MalformedError**](../source/dbrepo.api.md#dbrepo.api.exceptions.MalformedError) – If the payload is rejected by the service.
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the table does not exist.
  * [**ServiceError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceError) – If something went wrong with obtaining the information in the data service.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the retrieval.

#### get_tables(database_id: str) → List[[TableBrief](../source/dbrepo.api.md#dbrepo.api.dto.TableBrief)]

Get all tables.

* **Parameters:**
  **database_id** – The database id.
* **Returns:**
  List of tables, if successful.
* **Raises:**
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the database does not exist.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the retrieval.

#### get_units() → List[[UnitBrief](../source/dbrepo.api.md#dbrepo.api.dto.UnitBrief)]

Get all units known to the metadata database.

* **Returns:**
  List of units, if successful.
* **Raises:**
  [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the retrieval.

#### get_user(user_id: str) → [User](../source/dbrepo.api.md#dbrepo.api.dto.User)

Get a user with given user id.

* **Returns:**
  The user, if successful.
* **Raises:**
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the retrieval.
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the user does not exist.

#### get_users() → List[[UserBrief](../source/dbrepo.api.md#dbrepo.api.dto.UserBrief)]

Get all users.

* **Returns:**
  List of users, if successful.
* **Raises:**
  [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the retrieval.

#### get_view(database_id: str, view_id: str) → [View](../source/dbrepo.api.md#dbrepo.api.dto.View)

Get a view of a database with given database id and view id.

* **Parameters:**
  * **database_id** – The database id.
  * **view_id** – The view id.
* **Returns:**
  The view, if successful.
* **Raises:**
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the container does not exist.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the retrieval.

#### get_view_data(database_id: str, view_id: str, page: int = 0, size: int = 10) → DataFrame

Get data of a view in a database with given database id and view id.

* **Parameters:**
  * **database_id** – The database id.
  * **view_id** – The view id.
  * **page** – The result pagination number. Optional. Default: 0.
  * **size** – The result pagination size. Optional. Default: 10.
* **Returns:**
  The view data, if successful.
* **Raises:**
  * [**MalformedError**](../source/dbrepo.api.md#dbrepo.api.exceptions.MalformedError) – If the payload is rejected by the service.
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the view does not exist.
  * [**ExternalSystemError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ExternalSystemError) – If the mapped view selection query is erroneous.
  * [**ServiceError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceError) – If something went wrong with obtaining the information in the metadata service.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the retrieval.

#### get_view_data_count(database_id: str, view_id: str) → int

Get data count of a view in a database with given database id and view id.

* **Parameters:**
  * **database_id** – The database id.
  * **view_id** – The view id.
* **Returns:**
  The result count of the view query, if successful.
* **Raises:**
  * [**MalformedError**](../source/dbrepo.api.md#dbrepo.api.exceptions.MalformedError) – If the payload is rejected by the service.
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the view does not exist.
  * [**ExternalSystemError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ExternalSystemError) – If the mapped view selection query is erroneous.
  * [**ServiceError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceError) – If something went wrong with obtaining the information in the metadata service.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the retrieval.

#### get_views(database_id: str) → List[[ViewBrief](../source/dbrepo.api.md#dbrepo.api.dto.ViewBrief)]

Gets views of a database with given database id.

* **Parameters:**
  **database_id** – The database id.
* **Returns:**
  The list of views, if successful.
* **Raises:**
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the container does not exist.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the retrieval.

#### import_table_data(database_id: str, table_id: str, dataframe: DataFrame) → None

Import a csv dataset from a file into a table in a database with given database id and table id. ATTENTION:
the import is column-ordering sensitive! The csv dataset must have the same columns in the same order as the
target table.

* **Parameters:**
  * **database_id** – The database id.
  * **table_id** – The table id.
  * **dataframe** – The pandas dataframe.
* **Raises:**
  * [**MalformedError**](../source/dbrepo.api.md#dbrepo.api.exceptions.MalformedError) – If the payload is rejected by the service (e.g. LOB could not be imported).
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the table does not exist.
  * [**ServiceError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceError) – If something went wrong with obtaining the information in the metadata service.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the insert.

#### publish_identifier(identifier_id: str) → [Identifier](../source/dbrepo.api.md#dbrepo.api.dto.Identifier)

Publish an identifier with given id.

* **Parameters:**
  **identifier_id** – The identifier id.
* **Returns:**
  The identifier, if successful.
* **Raises:**
  * [**MalformedError**](../source/dbrepo.api.md#dbrepo.api.exceptions.MalformedError) – If the payload is rejected by the service.
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the database, table/view/subset or user does not exist.
  * [**ServiceConnectionError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceConnectionError) – If something went wrong with connection to the search service.
  * [**ServiceError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceError) – If something went wrong with obtaining the information in the search service.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the creation of the identifier.

#### update_database_access(database_id: str, user_id: str, type: [AccessType](../source/dbrepo.api.md#dbrepo.api.dto.AccessType)) → [AccessType](../source/dbrepo.api.md#dbrepo.api.dto.AccessType)

Updates the access for a user to a database with given database id and user id.

* **Parameters:**
  * **database_id** – The database id.
  * **user_id** – The user id.
  * **type** – The access type.
* **Returns:**
  The access type, if successful.
* **Raises:**
  * [**MalformedError**](../source/dbrepo.api.md#dbrepo.api.exceptions.MalformedError) – If the payload is rejected by the service.
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the database or user does not exist.
  * [**ServiceConnectionError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceConnectionError) – If something went wrong with connection to the data service.
  * [**ServiceError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceError) – If something went wrong with obtaining the information in the data service.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the retrieval.

#### update_database_owner(database_id: str, user_id: str) → [Database](../source/dbrepo.api.md#dbrepo.api.dto.Database)

Updates the database owner of a database with given database id.

* **Parameters:**
  * **database_id** – The database id.
  * **user_id** – The user id of the new owner.
* **Returns:**
  The database, if successful.
* **Raises:**
  * [**MalformedError**](../source/dbrepo.api.md#dbrepo.api.exceptions.MalformedError) – If the payload was rejected by the service.
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the database does not exist.
  * [**ServiceConnectionError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceConnectionError) – If something went wrong with connection to the search service.
  * [**ServiceError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceError) – If something went wrong with obtaining the information in the search service.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the update.

#### update_database_schema(database_id: str) → [DatabaseBrief](../source/dbrepo.api.md#dbrepo.api.dto.DatabaseBrief)

Updates the database table and view metadata of a database with given database id.

* **Parameters:**
  **database_id** – The database id.
* **Returns:**
  The updated database, if successful.
* **Raises:**
  * [**MalformedError**](../source/dbrepo.api.md#dbrepo.api.exceptions.MalformedError) – If the payload was rejected by the service.
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the database does not exist.
  * [**ServiceConnectionError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceConnectionError) – If something went wrong with connection to the data service.
  * [**ServiceError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceError) – If something went wrong with obtaining the information in the data service.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the update.

#### update_database_visibility(database_id: str, is_public: bool, is_schema_public: bool, is_dashboard_enabled: bool) → [Database](../source/dbrepo.api.md#dbrepo.api.dto.Database)

Updates the database visibility of a database with given database id.

* **Parameters:**
  * **database_id** – The database id.
  * **is_public** – The visibility of the data. If set to True the data will be publicly visible.
  * **is_schema_public** – The visibility of the schema metadata. If set to True the schema metadata will be publicly visible.
  * **is_dashboard_enabled** – If set to True, the provisioned dashboard for this database is enabled.
* **Returns:**
  The database, if successful.
* **Raises:**
  * [**MalformedError**](../source/dbrepo.api.md#dbrepo.api.exceptions.MalformedError) – If the payload was rejected by the service.
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the database does not exist.
  * [**ServiceConnectionError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceConnectionError) – If something went wrong with connection to the search service.
  * [**ServiceError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceError) – If something went wrong with obtaining the information in the search service.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the update.

#### update_identifier(identifier_id: str, database_id: str, type: [IdentifierType](../source/dbrepo.api.md#dbrepo.api.dto.IdentifierType), titles: List[[SaveIdentifierTitle](../source/dbrepo.api.md#dbrepo.api.dto.SaveIdentifierTitle)], publisher: str, creators: List[[SaveIdentifierCreator](../source/dbrepo.api.md#dbrepo.api.dto.SaveIdentifierCreator)], publication_year: int, descriptions: List[[SaveIdentifierDescription](../source/dbrepo.api.md#dbrepo.api.dto.SaveIdentifierDescription)] = None, funders: List[[SaveIdentifierFunder](../source/dbrepo.api.md#dbrepo.api.dto.SaveIdentifierFunder)] = None, licenses: List[[License](../source/dbrepo.api.md#dbrepo.api.dto.License)] = None, language: [Language](../source/dbrepo.api.md#dbrepo.api.dto.Language) = None, subset_id: str = None, view_id: str = None, table_id: str = None, publication_day: int = None, publication_month: int = None, related_identifiers: List[[SaveRelatedIdentifier](../source/dbrepo.api.md#dbrepo.api.dto.SaveRelatedIdentifier)] = None) → [Identifier](../source/dbrepo.api.md#dbrepo.api.dto.Identifier)

Update an existing identifier and update the metadata attached to it.

* **Parameters:**
  * **identifier_id** – The identifier id.
  * **database_id** – The database id of the created identifier.
  * **type** – The type of the created identifier.
  * **titles** – The titles of the created identifier.
  * **publisher** – The publisher of the created identifier.
  * **creators** – The creator(s) of the created identifier.
  * **publication_year** – The publication year of the created identifier.
  * **descriptions** – The description(s) of the created identifier. Optional.
  * **funders** – The funders(s) of the created identifier. Optional.
  * **licenses** – The license(s) of the created identifier. Optional.
  * **language** – The language of the created identifier. Optional.
  * **subset_id** – The subset id of the created identifier. Required when type=SUBSET, otherwise invalid. Optional.
  * **view_id** – The view id of the created identifier. Required when type=VIEW, otherwise invalid. Optional.
  * **table_id** – The table id of the created identifier. Required when type=TABLE, otherwise invalid. Optional.
  * **publication_day** – The publication day of the created identifier. Optional.
  * **publication_month** – The publication month of the created identifier. Optional.
  * **related_identifiers** – The related identifier(s) of the created identifier. Optional.
* **Returns:**
  The identifier, if successful.
* **Raises:**
  * [**MalformedError**](../source/dbrepo.api.md#dbrepo.api.exceptions.MalformedError) – If the payload is rejected by the service.
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the database, table/view/subset or user does not exist.
  * [**ServiceConnectionError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceConnectionError) – If something went wrong with connection to the search service.
  * [**ServiceError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceError) – If something went wrong with obtaining the information in the search service.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the creation of the identifier.

#### update_subset(database_id: str, subset_id: str, persist: bool) → [Query](../source/dbrepo.api.md#dbrepo.api.dto.Query)

Save query or mark it for deletion (at a later time) in a database with given database id and query id.

* **Parameters:**
  * **database_id** – The database id.
  * **subset_id** – The subset id.
  * **persist** – If set to True, the query will be saved and visible in the user interface, otherwise the query                 is marked for deletion in the future and not visible in the user interface.
* **Returns:**
  The query, if successful.
* **Raises:**
  * [**MalformedError**](../source/dbrepo.api.md#dbrepo.api.exceptions.MalformedError) – If the payload is rejected by the service.
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the database or user does not exist.
  * [**QueryStoreError**](../source/dbrepo.api.md#dbrepo.api.exceptions.QueryStoreError) – The query store rejected the update.
  * [**ServiceError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceError) – If something went wrong with obtaining the information in the data service.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the retrieval.

#### update_table_column(database_id: str, table_id: str, column_id: str, concept_uri: str = None, unit_uri: str = None) → [Column](../source/dbrepo.api.md#dbrepo.api.dto.Column)

Update semantic information of a table column by given database id and table id and column id.

* **Parameters:**
  * **database_id** – The database id.
  * **table_id** – The table id.
  * **column_id** – The column id.
  * **concept_uri** – The concept URI. Optional.
  * **unit_uri** – The unit URI. Optional.
* **Returns:**
  The column, if successful.
* **Raises:**
  * [**MalformedError**](../source/dbrepo.api.md#dbrepo.api.exceptions.MalformedError) – If the payload is rejected by the service.
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the accept header is neither application/json nor application/ld+json.
  * [**ServiceConnectionError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceConnectionError) – If something went wrong with connection to the search service.
  * [**ServiceError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceError) – If something went wrong with obtaining the information in the search service.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the retrieval of the identifiers.

#### update_table_data(database_id: str, table_id: str, data: dict, keys: dict) → None

Update data in a table in a database with given database id and table id.

* **Parameters:**
  * **database_id** – The database id.
  * **table_id** – The table id.
  * **data** – The data dictionary to be updated into the table with the form column=value of the table.
  * **keys** – The key dictionary matching the rows in the form column=value.
* **Raises:**
  * [**MalformedError**](../source/dbrepo.api.md#dbrepo.api.exceptions.MalformedError) – If the payload is rejected by the service (e.g. LOB data could not be imported).
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the table does not exist.
  * [**ServiceError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ServiceError) – If something went wrong with obtaining the information in the metadata service.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the update.

#### update_user(user_id: str, theme: str, language: str, firstname: str = None, lastname: str = None, affiliation: str = None, orcid: str = None) → [UserBrief](../source/dbrepo.api.md#dbrepo.api.dto.UserBrief)

Updates a user with given user id.

* **Parameters:**
  * **user_id** – The user id of the user that should be updated.
  * **theme** – The user theme. One of “light”, “dark”, “light-contrast”, “dark-contrast”.
  * **language** – The user language localization. One of “en”, “de”.
  * **firstname** – The updated given name. Optional.
  * **lastname** – The updated family name. Optional.
  * **affiliation** – The updated affiliation identifier. Optional.
  * **orcid** – The updated ORCID identifier. Optional.
* **Returns:**
  The user, if successful.
* **Raises:**
  * [**MalformedError**](../source/dbrepo.api.md#dbrepo.api.exceptions.MalformedError) – If the payload was rejected by the service.
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the user does not exist.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the update.

#### update_view(database_id: str, view_id: str, is_public: bool, is_schema_public: bool) → [ViewBrief](../source/dbrepo.api.md#dbrepo.api.dto.ViewBrief)

Get a view of a database with given database id and view id.

* **Parameters:**
  * **database_id** – The database id.
  * **view_id** – The view id.
  * **is_public** – If set to True, the view data is publicly visible.
  * **is_schema_public** – If set to True, the view schema is publicly visible.
* **Returns:**
  The view, if successful.
* **Raises:**
  * [**ForbiddenError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ForbiddenError) – If something went wrong with the authorization.
  * [**NotExistsError**](../source/dbrepo.api.md#dbrepo.api.exceptions.NotExistsError) – If the container does not exist.
  * [**ResponseCodeError**](../source/dbrepo.api.md#dbrepo.api.exceptions.ResponseCodeError) – If something went wrong with the retrieval.

#### whoami() → str | None

Print the username.

* **Returns:**
  The username, if set.
# Python AMQP API

### *class* dbrepo.AmqpClient.AmqpClient(broker_host: str = 'localhost', broker_port: int = 5672, broker_virtual_host: str = 'dbrepo', username: str = None, password: str = None)

The AmqpClient class for communicating with the DBRepo AMQP API to import data. All parameters can be set also     via environment variables, e.g. set endpoint with DBREPO_ENDPOINT. You can override the constructor parameters     with the environment variables.

* **Parameters:**
  * **broker_host** – The AMQP API host. Optional. Default: “localhost”.
  * **broker_port** – The AMQP API port. Optional. Default: 5672,
  * **broker_virtual_host** – The AMQP API virtual host. Optional. Default: “dbrepo”.
  * **username** – The AMQP API username. Optional.
  * **password** – The AMQP API password. Optional.

#### publish(routing_key: str, data=<class 'dict'>, exchange: str = 'dbrepo') → None

Publishes data to a given exchange with the given routing key with a blocking connection.

* **Parameters:**
  * **routing_key** – The routing key.
  * **data** – The data.
  * **exchange** – The exchange name. Default: “dbrepo”.
