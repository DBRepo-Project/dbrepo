---
author: Martin Weise
---

# Overview

We developed a Python Library for communicating with DBRepo from e.g. Jupyter Notebooks. See
the [Python Library](./open-api) page for more details.

We give usage examples of the most important use-cases we identified.

|                                                           |         UI         |      Terminal      |        JDBC        |       Python       |
|-----------------------------------------------------------|:------------------:|:------------------:|:------------------:|:------------------:|
| [Create User Account](#create-user-account)               | :white_check_mark: | :white_check_mark: |        :x:         | :white_check_mark: | 
| [Create Database](#create-database)                       | :white_check_mark: | :white_check_mark: |        :x:         | :white_check_mark: | 
| [Import Dataset](#import-dataset)                         | :white_check_mark: | :white_check_mark: |        :x:         | :white_check_mark: | 
| [Import Database Dump](#import-database-dump)             | :white_check_mark: | :white_check_mark: |        :x:         |        :x:         | 
| [Import Live Data](#import-live-data)                     |        :x:         | :white_check_mark: | :white_check_mark: | :white_check_mark: | 
| [Export Subset](#export-subset)                           | :white_check_mark: | :white_check_mark: | :white_check_mark: | :white_check_mark: | 
| [Assign Database PID](#assign-database-pid)               | :white_check_mark: | :white_check_mark: | :white_check_mark: | :white_check_mark: | 
| [Private Database &amp; Access](#private-database-access) | :white_check_mark: | :white_check_mark: | :white_check_mark: | :white_check_mark: | 

## Create User Account

A user wants to create an account in DBRepo.

=== "UI"

    To create a new user account in DBRepo, navigate to the signup page by clicking the ":material-account-plus: SIGNUP"
    button and provide a valid work e-mail address :material-numeric-1-circle-outline: and a username (in lowercase
    alphanumeric characters) :material-numeric-2-circle-outline:. Choose a secure password in field 
    :material-numeric-3-circle-outline: and repeat it in field :material-numeric-4-circle-outline:. Click "SUBMIT" and
    the system creates a user account in Figure 1 with the [default roles](./system-services-authentication/#roles)
    that your administrator has assigned.

    <figure markdown>
    ![Create user account](../images/screenshots/create-account-step-1.png){ .img-border }
    <figcaption>Figure 1: Create user account.</figcaption>
    </figure>

    To login in DBRepo, navigate to the login page by clicking the ":material-login: LOGIN" button and provide the
    username :material-numeric-1-circle-outline: and the password from the previous step
    :material-numeric-2-circle-outline:. After clicking "LOGIN", you will acquire the roles assigned to you and are now
    authenticated with DBRepo in Figure 2.

    <figure markdown>
    ![Create user account](../images/screenshots/create-account-step-2.png){ .img-border }
    <figcaption>Figure 2: Login to the user account.</figcaption>
    </figure>

    You can view your user information upon clicking on your username :material-numeric-1-circle-outline: on the top. To
    change your password in all components of DBRepo, click the "AUTHENTICATION" tab :material-numeric-2-circle-outline:
    and change your password by typing a new one into fields :material-numeric-3-circle-outline: and repeat it 
    identically in field :material-numeric-4-circle-outline:. 

    <figure markdown>
    ![Change user account password](../images/screenshots/create-account-step-3.png){ .img-border }
    <figcaption>Figure 3: Change user account password.</figcaption>
    </figure>

=== "Terminal"

    To create a new user account in DBRepo with the Terminal, provide your details in the following REST HTTP-API call.

    ```bash
    curl -sSL \
      -X POST \
      -H "Content-Type: application/json" \
      -d '{"username": "foo","email": "foo.bar@example.com", "password":"bar"}' \
      http://<hostname>/api/user | jq .id
    ```

    You can view your user information by sending a request to the user endpoint with your access token.

    ```bash
    curl -sSL \
      -H "Content-Type: application/json" \
      -u "foo:bar" \
      http://<hostname>/api/user/14824cc5-186b-423d-a7b7-a252aed42b59 | jq
    ```

    To change your password in all components of DBRepo:

    ```bash
    curl -sSL \
      -X PUT \
      -H "Content-Type: application/json" \
      -u "foo:bar" \
      -d '{"password": "s3cr3tp4ss0rd"}' \
      http://<hostname>/api/user/USER_ID/password | jq
    ```

=== "Python"

    To create a new user account in DBRepo with the Terminal, provide your details in the following REST HTTP-API call.

    ```python
    from dbrepo.RestClient import RestClient

    client = RestClient(endpoint="http://<hostname>")
    user = client.create_user(username="foo", password="bar",
                              email="foo@example.com")
    print(f"user id: {user.id}")
    ```

    You can view your user information by sending a request to the user endpoint with your access token.

    ```python
    from dbrepo.RestClient import RestClient

    client = RestClient(endpoint="http://<hostname>", username="foo",
                        password="bar")
    user = client.get_user(user_id="57ea75c6-2a1d-4516-89f4-296b8b62539b")
    print(f"user info: {user}")
    ```

    To change your password in all components of DBRepo:

    ```python
    from dbrepo.RestClient import RestClient

    client = RestClient(endpoint="http://<hostname>", username="foo",
                        password="bar")
    client.update_user_password(user_id="57ea75c6-2a1d-4516-89f4-296b8b62539b",
                                password="baz")
    ```

## Create Database

A user wants to create a database in DBRepo.

=== "UI"

    Login and press the ":material-plus: DATABASE" button on the top right :material-numeric-1-circle-outline: as seen in Figure 4.

    <figure markdown>
    ![Open the create database dialog](../images/screenshots/create-database-step-1.png){ .img-border }
    <figcaption>Figure 4: Open the create database dialog.</figcaption>
    </figure>

    Give the database a meaningful title :material-numeric-1-circle-outline: that describes the contained data in few 
    words and select a pre-configured container :material-numeric-2-circle-outline: from the list for this database. To
    finally create the database, press "Create" :material-numeric-3-circle-outline: as seen in Figure 5.

    <figure markdown>
    ![Create database form](../images/screenshots/create-database-step-2.png){ .img-border }
    <figcaption>Figure 5: Create database form.</figcaption>
    </figure>

    After a few seconds, you can see the created database in the "Recent Databases" list, as seen in Figure 6.

    <figure markdown>
    ![View the created database](../images/screenshots/create-database-step-3.png){ .img-border }
    <figcaption>Figure 6: View the created database.</figcaption>
    </figure>

=== "Terminal"

    Then list all available containers with their database engine descriptions and obtain a container id.

    ```bash
    curl -sSL \
      -H "Content-Type: application/json" \
      http://<hostname>/api/container | jq
    ```

    Create a public databse with the container id from the previous step. You can also create a private database in this
    step, others can still see the metadata.

    ```bash
    curl -sSL \
      -X POST \
      -H "Content-Type: application/json" \
      -u "foo:bar" \
      -d '{"name":"Danube Water Quality Measurements", "container_id":1, "is_public":true}' \
      http://<hostname>/api/database | jq .id
    ```

=== "Python"

    List all available containers with their database engine descriptions and obtain a container id.

    ```python
    from dbrepo.RestClient import RestClient

    client = RestClient(endpoint="http://<hostname>", username="foo",
                        password="bar")
    containers = client.get_containers()
    print(containers)
    ```

    Create a public databse with the container id from the previous step. You can also create a private database in this
    step, others can still see the metadata.

    ```python
    from dbrepo.RestClient import RestClient

    client = RestClient(endpoint="http://<hostname>", username="foo",
                        password="bar")
    database = client.create_database(name="Test", container_id=1, is_public=True)
    print(f"database id: {database.id}")
    ```

## Import Dataset

A user wants to import a static dataset (e.g. from a .csv file) into a database that they have at least `write-own`
access to. This is the default for self-created databases like above in [Create Databases](#create-database).

=== "UI"

    Login and select a database where you have at least `write-all` access (this is the case for e.g. self-created 
    databases). Click the ":material-cloud-upload: IMPORT CSV" button :material-numeric-1-circle-outline: as seen in 
    Figure 7.

    <figure markdown>
    ![Open the import CSV form](../images/screenshots/import-dataset-step-1.png){ .img-border }
    <figcaption>Figure 7: Open the import CSV form.</figcaption>
    </figure>

    Provide the table name :material-numeric-1-circle-outline: and optionally a table description :material-numeric-2-circle-outline:
    as seen in Figure 8.

    <figure markdown>
    ![Basic table information](../images/screenshots/import-dataset-step-2.png){ .img-border }
    <figcaption>Figure 8: Basic table information.</figcaption>
    </figure>

    Next, provide the dataset metadata that is necessary for import into the table by providing the dataset separator
    (e.g. `,` or `;` or `\t`) in :material-numeric-1-circle-outline:. If your dataset has a header line (the first line
    containing the names of the columns) set the number of lines to skip to 1 in field :material-numeric-2-circle-outline:.
    If your dataset contains more lines that should be ignored, set the number of lines accordingly. If your dataset
    contains quoted values, indicate this by setting the field :material-numeric-3-circle-outline: accordingly
    in Figure 9.

    If your dataset contains encodings for `NULL` (e.g. `NA`), provide this encoding information 
    in :material-numeric-4-circle-outline:. Similar, if it contains encodings for boolean `true` (e.g. `1` or `YES`),
    provide this encoding information in :material-numeric-5-circle-outline:. For boolean `false` (e.g. `0` or `NO`),
    provide this information in :material-numeric-6-circle-outline:.

    <figure markdown>
    ![Dataset metadata necessary for import](../images/screenshots/import-dataset-step-3.png){ .img-border }
    <figcaption>Figure 9: Dataset metadata necessary for import.</figcaption>
    </figure>

    Select the dataset file from your local computer by clicking :material-numeric-1-circle-outline: or dragging the
    dataset file onto the field in Figure 10.

    <figure markdown>
    ![Dataset import file](../images/screenshots/import-dataset-step-4.png){ .img-border }
    <figcaption>Figure 10: Dataset import file.</figcaption>
    </figure>

    The table schema is suggested based on heuristics between the upload and the suggested schema in Figure 8. If your
    dataset has no column names present, e.g. you didn't provide a *Number of lines to skip* (c.f. Figure 6), then you
    need to provide a column name in :material-numeric-1-circle-outline:. Provide a data type from the list of MySQL 8
    available data types :material-numeric-2-circle-outline:. Indicate if the column is (part of) a primary key 
    :material-numeric-4-circle-outline: or if `NULL` values are allowed in :material-numeric-5-circle-outline: or if a
    unique constraint is needed (no values in this column are then allowed to repeat) in :material-numeric-6-circle-outline:.

    Optionally, you can remove table column definitions by clicking the "REMOVE" button or add additional table column
    definitions by clicking the "ADD COLUMN" button in Figure 11.

    <figure markdown>
    ![Confirm the table schema and provide missing information](../images/screenshots/import-dataset-step-5.png){ .img-border }
    <figcaption>Figure 11: Confirm the table schema and provide missing information.</figcaption>
    </figure>

    If a table column data type is of `DATE` or `TIMESTAMP` (or similar), provide a date format 
    :material-numeric-3-circle-outline: from the list of available formats that are most similar to the one in the
    dataset as seen in Figure 12.

    <figure markdown>
    ![Confirm the table schema and provide missing information](../images/screenshots/import-dataset-step-6.png){ .img-border }
    <figcaption>Figure 12: Confirm the table schema and provide missing information.</figcaption>
    </figure>

    When you are finished with the table schema definition, the dataset is imported and a table is created. You are
    being redirected automatically to the table info page upon success, navigate to the "DATA" tab 
    :material-numeric-1-circle-outline:. You can still delete the table :material-numeric-2-circle-outline: as long as
    no identifier is associated with it :material-numeric-3-circle-outline:.

    Public databases allow anyone to download :material-numeric-4-circle-outline: the table data as dataset file. Also
    it allows anyone to view the recent history of inserted data :material-numeric-5-circle-outline: dialog in Figure
    13.

    <figure markdown>
    ![Table data](../images/screenshots/import-dataset-step-7.png){ .img-border }
    <figcaption>Figure 13: Table data.</figcaption>
    </figure>

=== "Terminal"

    Select a database where you have at least `write-all` access (this is the case for e.g. self-created databases).

    Upload the dataset via the [`tusc`](https://github.com/adhocore/tusc.sh) terminal application or use Python and
    copy the file key.

    ```bash
    tusc -H http://<hostname>/api/upload/files -f danube.csv -b /dbrepo-upload/
    ```

    Analyse the dataset and get the table column names and datatype suggestion.

    ```bash
    curl -sSL \
      -X POST \
      -H "Content-Type: application/json" \
      -u "foo:bar" \
      -d '{"filename":"FILEKEY","separator":","}' \
      http://<hostname>/api/analyse/datatypes | jq
    ```

    Provide the table name and optionally a table description along with the table columns.

    ```bash
    curl -sSL \
      -X POST \
      -H "Content-Type: application/json" \
      -u "foo:bar" \
      -d '{"name":"Danube water levels","description":"Measurements of the river danube water levels","columns":[{"name":"datetime","type":"timestamp","dfid":1,"primary_key":false,"null_allowed":true},{"name":"level","type":"bigint","size":255,"primary_key":false,"null_allowed":true}]}' \
      http://<hostname>/api/database/1/table | jq .id
    ```

    Next, provide the dataset metadata that is necessary for import into the table by providing the dataset separator
    (e.g. `,` or `;` or `\t`). If your dataset has a header line (the first line containing the names of the columns) 
    set the number of lines to skip to 1. If your dataset contains more lines that should be ignored, set the number of
    lines accordingly. If your dataset contains quoted values, indicate this by setting the field accordingly.

    If your dataset contains encodings for `NULL` (e.g. `NA`), provide this encoding information. Similar, if it 
    contains encodings for boolean `true` (e.g. `1` or `YES`), provide this encoding information. For boolean `false`
    (e.g. `0` or `NO`), provide this information.

    ```bash
    curl -sSL \
      -X POST \
      -H "Content-Type: application/json" \
      -u "foo:bar" \
      -d '{"location":"FILEKEY","separator":",","quote":"\"","skip_lines":1,"null_element":"NA"}' \
      http://<hostname>/api/database/1/table/1/data/import | jq
    ```

    When you are finished with the table schema definition, the dataset is imported and a table is created. View the
    table data:

    ```bash
    curl -sSL \
      -H "Content-Type: application/json" \
      -u "foo:bar" \
      http://<hostname>/api/database/1/table/1/data?page=0&size=10 | jq
    ```

=== "Python"

    Select a database where you have at least `write-all` access (this is the case for e.g. self-created databases).
    Upload the dataset to analyse the dataset and get the table column names and datatype suggestion.

    ```python
    from dbrepo.RestClient import RestClient

    client = RestClient(endpoint="http://<hostname>", username="foo",
                        password="bar")
    analysis = client.analyse_datatypes(file_path="/path/to/dataset.csv",
                                        separator=",")
    print(f"analysis: {analysis}")
    # separator=, columns={(id, bigint), ...}, line_termination=\n
    ```

    Provide the table name and optionally a table description along with the table columns.

    ```python
    from dbrepo.RestClient import RestClient

    client = RestClient(endpoint="http://<hostname>", username="foo",
                        password="bar")
    table = client.create_table(database_id=1,
                                name="Sensor Data",
                                constraints=CreateTableConstraints(),
                                columns=[CreateTableColumn(name="id",
                                                           type=ColumnType.BIGINT,
                                                           primary_key=True,
                                                           null_allowed=False)])
    print(f"table id: {table.id}")
    ```

    Next, provide the dataset metadata that is necessary for import into the table by providing the dataset separator
    (e.g. `,` or `;` or `\t`). If your dataset has a header line (the first line containing the names of the columns) 
    set the number of lines to skip to 1. If your dataset contains more lines that should be ignored, set the number of
    lines accordingly. If your dataset contains quoted values, indicate this by setting the field accordingly.

    If your dataset contains encodings for `NULL` (e.g. `NA`), provide this encoding information. Similar, if it 
    contains encodings for boolean `true` (e.g. `1` or `YES`), provide this encoding information. For boolean `false`
    (e.g. `0` or `NO`), provide this information.

    ```python
    from dbrepo.RestClient import RestClient

    client = RestClient(endpoint="http://<hostname>", username="foo",
                        password="bar")
    client.import_table_data(database_id=1, table_id=3,
                             file_path="/path/to/dataset.csv", separator=",",
                             skip_lines=1, line_encoding="\n")
    ```

    When you are finished with the table schema definition, the dataset is imported and a table is created. View the
    table data:

    ```python
    from dbrepo.RestClient import RestClient

    client = RestClient(endpoint="http://<hostname>", username="foo",
                        password="bar")
    client.get_table_data(database_id=1, table_id=3)
    ```

## Import Database Dump

A user wants to import a database dump in `.sql` (or in `.sql.gz`) format into DBRepo.

=== "UI"

    First, create a new database as descriped in the [Create Database](#create-database) use-case above. Then, import
    the database dump `dump.sql` via the [MySQL Workbench](https://www.mysql.com/products/workbench/) client which is 
    semi-compatible with MariaDB databases, i.e. core features work some status/performance features do not.

    Setup a new connection in the MySQL Workbench (c.f. Figure 14) by clicking the small 
    ":material-plus-circle-outline:" button :material-numeric-1-circle-outline: to open the dialog. In the opened dialog
    fill out the connection parameters (for local deployments the hostname is `127.0.0.1` and port `3307` for the
    [Data Database](./system-databases-data/) :material-numeric-2-circle-outline:.

    The default credentials are username `root` and password `dbrepo`, type the password in 
    :material-numeric-3-circle-outline: and click the "OK" button. Then finish the setup of the new connection by
    clicking the "OK" button :material-numeric-4-circle-outline:.

    <figure markdown>
    ![Setup New Connection in MySQL Workbench](../images/screenshots/import-database-dump-step-1.png)
    <figcaption>Figure 14: Setup New Connection in MySQL Workbench.</figcaption>
    </figure>

    Now you should be able to see some statistics for the Data Database (c.f. Figure 15), especially that it is running
    :material-numeric-1-circle-outline: and basic connection and version information :material-numeric-2-circle-outline:.

    <figure markdown>
    ![Server status of the Data Database in MySQL Workbench](../images/screenshots/import-database-dump-step-2.png)
    <figcaption>Figure 15: Server status of the Data Database in MySQL Workbench.</figcaption>
    </figure>

    Then proceed to import the database dump `dump.sql` by clicking "Data Import/Restore" 
    :material-numeric-1-circle-outline: and select "Import from Self-Contained File" in the Import Options. Then
    select the `dump.sql` file in the file path selection. Last, select the database you want to import this `dump.sql`
    into :material-numeric-4-circle-outline: (you can also create a new database for the import by clicking "New...").
    The import starts after clicking "Start Import" :material-numeric-5-circle-outline:.

    <figure markdown>
    ![Data Import/Restore in MySQL Workbench](../images/screenshots/import-database-dump-step-3.png)
    <figcaption>Figure 16: Data Import/Restore in MySQL Workbench.</figcaption>
    </figure>

=== "Terminal"

    First, create a new database as descriped in the [Create Database](#create-database) use-case above. Then, import
    the database dump `dump.sql` via the `mariadb` client.

    ```bash
    mariadb -H127.0.0.1 -p3307 -uUSERNAME -pYOURPASSWORD db_name < dump.sql
    ```

    Alternatively, if your database dump is compressed, import the `dump.sql.gz` by piping it through `gunzip`.

    ```bash
    gunzip < dump.sql.gz | mysql -H127.0.0.1 -p3307 -uUSERNAME -pYOURPASSWORD db_name
    ```

    The [Metadata Service](./system-services-metadata) periodically (by default configuration every 60 seconds) checks
    and adds missing tables and views to the [Metadata Database](./system-databases-metadata), the database dump
    will be visible afterwards. Currently, date formats for columns with time types (e.g. `DATE`, `TIMESTAMP`) are
    assumed to match the first date format found for the database image. This may need to be manually specified by the
    administrator.

    !!! example "Specifying a custom date format"

        In case the pre-defined date formats are not matching the found date format in the database dump, the system
        administrator needs to add it manually in the [Metadata Database](./system-databases-metadata).

        ```sql
        INSERT INTO `mdb_images_date` (`iid`, `database_format`, `unix_format`, `example`, `has_time`)
        VALUES (1, '%d.%c.%Y', 'dd.MM.yyyy', '15.01.2024', false),
               (1, '%Y-%c-%d %l:%i:%S %p', 'yyyy-MM-dd ll:mm:ss r', '2024-01-15 06:23:12 AM', true);
        ```

## Import Live Data

A user wants to import live data from e.g. sensor measurements fast and without delay into a table in DBRepo.

=== "Terminal"

    Add a data tuple to an already existing table where the user has at least `write-own` access.

    ```bash
    curl -sSL \
      -X POST \
      -H "Content-Type: application/json" \
      -u "foo:bar" \
      -d '{"data":{"column1": "value1", "column2": "value2"}}' \
      http://<hostname>/api/database/1/table/1/data
    ```

=== "JDBC API"

    Add a data tuple to an already existing table where the user has at least `write-own` access. Connect to the
    database via JDBC, you can obtain the connection string in the UI under the database info (c.f. Figure 14).

    <figure markdown>
    ![JDBC connection information](../images/screenshots/database-jdbc.png){ .img-border }
    <figcaption>Figure 14: JDBC connection information.</figcaption>
    </figure>
    

    ```sql
    INSERT INTO `danube_water_levels` (`datetime`, `level`)
    VALUES (NOW(), '10')
    ```

=== "Python"

    Add a data tuple to an already existing table where the user has at least `write-own` access. Connect to the
    database via AMQP, you can obtain the connection string in the UI under the table info (c.f. Figure 14).

    <figure markdown>
    ![AMQP connection information](../images/screenshots/table-amqp.png){ .img-border }
    <figcaption>Figure 14: AMQP connection information.</figcaption>
    </figure>

    ```python
    from dbrepo.AmqpClient import AmqpClient

    client = AmqpClient(broker_host="test.dbrepo.tuwien.ac.at", username="foo",
                        password="bar")
    client.publish(exchange="dbrepo", routing_key="dbrepo.database_feed.test",
                   data={'precipitation': 2.4})
    ```

## Export Subset

A user wants to create a subset and export it as csv file.

=== "UI"

    Login and select a database where you have at least `read` access (this is the case for e.g. self-created 
    databases). Click the ":material-wrench: CREATE SUBSET" button :material-numeric-1-circle-outline: as seen in 
    Figure 17.

    <figure markdown>
    ![Open the create subset form](../images/screenshots/export-subset-step-1.png){ .img-border }
    <figcaption>Figure 17: Open the create subset form.</figcaption>
    </figure>

    A subset can be created by using our query builder that is visible by default in the "SIMPLE" tab. First, a source 
    table :material-numeric-1-circle-outline: needs to be selected, then the columns that are part of the subset in
    :material-numeric-2-circle-outline:. Optionally the subset can be filtered. The subset query (=SQL) is displayed
    in :material-numeric-3-circle-outline: in Figure 18. 

    Once you are confident the query covers the desired result, click ":material-run: Create".

    <figure markdown>
    ![Subset query building](../images/screenshots/export-subset-step-2.png){ .img-border }
    <figcaption>Figure 18: Subset query building.</figcaption>
    </figure>

    Once the subset is created (may take some seconds), the user is presented with the result set in
    :material-numeric-1-circle-outline:, more information on the subset can be obtained by clicking ":material-run:
    View" on the top (c.f. Figure 19).

    <figure markdown>
    ![Subset result set](../images/screenshots/export-subset-step-3.png){ .img-border }
    <figcaption>Figure 19: Subset result set.</figcaption>
    </figure>

    The subset information page in Figure 20 shows the most important metadata like subset query hash and result hash
    (e.g. for reproducability) and subset result count. Note that although this subset is stored in the query store
    already, it is only temporarly stored there for 24 hours (default configuration). 

    After that the subset is deleted from the query store. If you wish to keep the subset (with metadata information), 
    click ":material-content-save-outline: SAVE" in :material-numeric-1-circle-outline:. The subset can be exported to
    a csv file by clicking the ":material-download: DATA .CSV" button :material-numeric-2-circle-outline:.

    <figure markdown>
    ![Subset information](../images/screenshots/export-subset-step-4.png){ .img-border }
    <figcaption>Figure 20: Subset information.</figcaption>
    </figure>

=== "Terminal"

    A subset can be created by passing a SQL query to a database where you have at least `read` access (this is the case
    for e.g. self-created databases).

    ```bash
    curl -sSL \
      -X POST \
      -H "Content-Type: application/json" \
      -u "foo:bar" \
      -d '{"statement":"SELECT `id`,`datetime`,`level` FROM `danube_water_levels`"}' \
      http://<hostname>/api/database/1/query?page=0&sort=10 | jq .id
    ```

    !!! note

        An optional field `"timestamp":"2024-01-16 23:00:00"` can be provided to execute a query with a system time of
        2024-01-16 23:00:00 (UTC). Make yourself familiar with the concept
        of [System Versioned Tables](https://mariadb.com/kb/en/system-versioned-tables/) for more information.

    ```bash
    curl -sSL \
      -H "Content-Type: application/json" \
      -u "foo:bar" \
      http://<hostname>/api/database/1/query/1 | jq
    ```

    The subset information shows the most important metadata like subset query hash and result hash (e.g. for 
    reproducability) and subset result count. Note that although this subset is stored in the query store already, it is
    only temporarly stored there for 24 hours (default configuration).

    After that the subset is deleted from the query store. If you wish to keep the subset (with metadata information),
    persist it.

    ```bash
    curl -sSL \
      -X PUT \
      -H "Content-Type: application/json" \
      -u "foo:bar" \
      -d '{"persist": true}' \
      http://<hostname>/api/database/1/query/1 | jq
    ```

    The subset can be exported to a `subset_export.csv` file (this also works for non-persisted subsets).

    ```bash
    curl -sSL \
      -u "foo:bar" \
      -H "Accept: text/csv" \
      -o subset_export.csv \
      http://<hostname>/api/database/1/query/1
    ```

=== "JDBC"

    A subset can be created by passing a SQL query to a database where you have at least `read` access (this is the case
    for e.g. self-created databases). Connect to the database via JDBC, you can obtain the connection string in the UI
    under the database info (c.f. Figure 20).

    <figure markdown>
    ![JDBC connection information](../images/screenshots/database-jdbc.png){ .img-border }
    <figcaption>Figure 20: JDBC connection information.</figcaption>
    </figure>

    ```sql
    CALL store_query('SELECT `id`,`datetime`,`level` FROM `danube_water_levels`',
        NOW(), @subsetId)
    ```

    Afterwards, you can see the subset in the UI with subset id `@subsetId` and persist it there. Only the administrator
    can persist the subset in the [Data Database](./system-databases-data) through JDBC by setting the `persisted`
    column to `true` in the `qs_queries` table.

=== "Python"

    A subset can be created by passing a SQL query to a database where you have at least `read` access (this is the case
    for e.g. self-created databases).

    ```python
    from dbrepo.RestClient import RestClient

    client = RestClient(endpoint="http://<hostname>", username="foo",
                        password="bar")
    subset = client.execute_query(database_id=1,
                                  query="SELECT `id`,`datetime` FROM `danube_water_levels`")
    print(f"subset id: {subset.id}")
    ```

    !!! note

        An optional field `timestamp="2024-01-16 23:00:00"` can be provided to execute a query with a system time of
        2024-01-16 23:00:00 (UTC). Make yourself familiar with the concept
        of [System Versioned Tables](https://mariadb.com/kb/en/system-versioned-tables/) for more information.

    The subset information can be shown:

    ```python
    from dbrepo.RestClient import RestClient

    client = RestClient(endpoint="http://<hostname>", username="foo",
                        password="bar")
    subset = client.get_query(database_id=1, query_id=6)
    print(subset)
    ```

    The subset information shows the most important metadata like subset query hash and result hash (e.g. for 
    reproducability) and subset result count. Note that although this subset is stored in the query store already, it is
    only temporarly stored there for 24 hours (default configuration).

    After that the subset is deleted from the query store. If you wish to keep the subset (with metadata information),
    persist it.

    ```python
    from dbrepo.RestClient import RestClient

    client = RestClient(endpoint="http://<hostname>", username="foo",
                        password="bar")
    client.update_query(database_id=1, query_id=6, persist=True)
    ```

    The subset can be exported to a `subset_export.csv` file (this also works for non-persisted subsets).

    ```python
    from dbrepo.RestClient import RestClient

    client = RestClient(endpoint="http://<hostname>", username="foo",
                        password="bar")
    client.get_query_data(database_id=1, query_id=6, file_path='subset_export.csv')
    ```

## Assign Database PID

A user wants to assign a persistent identifier to a database owned by them.

=== "UI"

    Login and select a database where you are the owner (this is the case for e.g. self-created databases). Click 
    the ":material-identifier: GET PID" button :material-numeric-1-circle-outline: as seen in Figure 21.

    <figure markdown>
    ![Open the get persisent identifier form](../images/screenshots/assign-database-pid-step-1.png){ .img-border }
    <figcaption>Figure 21: Open the get persisent identifier form.</figcaption>
    </figure>

    First, provide information on the dataset creator(s). Since the [Metadata Service](./system-services-metadata) 
    automatically resolves external PIDs, the easiest way is to provide the correct mandatory data is by filling the
    name identifier :material-numeric-1-circle-outline:. The creator type :material-numeric-2-circle-outline:
    denotes either a natural person or organization. Optionally fill out the given 
    name :material-numeric-3-circle-outline: and family name :material-numeric-4-circle-outline:, both form the
    mandatory name :material-numeric-5-circle-outline:. Optionally provide an external affilation
    identifier :material-numeric-6-circle-outline: which is automatically resolved. Optionally provide an affiliation
    name :material-numeric-7-circle-outline: in Figure 22. You can change the order of creators with the up/down buttons
    :material-numeric-8-circle-outline:.

    <figure markdown>
    ![Identifier creator fields](../images/screenshots/assign-database-pid-step-2.png){ .img-border }
    <figcaption>Figure 22: Identifier creator fields.</figcaption>
    </figure>

    The identifier needs at least one title in the title field :material-numeric-1-circle-outline: and optionally a
    title type :material-numeric-2-circle-outline: from the selection and a title 
    language :material-numeric-3-circle-outline: from the available list of languages. Additional titles can be removed
    again :material-numeric-4-circle-outline: if they are not needed in Figure 23.

    <figure markdown>
    ![JDBC connection information](../images/screenshots/assign-database-pid-step-3.png){ .img-border }
    <figcaption>Figure 23: Identifier title fields.</figcaption>
    </figure>

    The identifier needs at least one description in the description field :material-numeric-1-circle-outline: and 
    optionally a description type :material-numeric-2-circle-outline: from the selection and a description 
    language :material-numeric-3-circle-outline:. Fill the dataset publisher information 
    :material-numeric-4-circle-outline: and publication year :material-numeric-5-circle-outline: and optionally a
    publication month and publication day in Figure 24.

    <figure markdown>
    ![Identifier description fields and publishing information](../images/screenshots/assign-database-pid-step-4.png){ .img-border }
    <figcaption>Figure 24: Identifier description fields and publishing information.</figcaption>
    </figure>

    Optionally reference other PIDs :material-numeric-1-circle-outline:, if you added too much, removing a related
    identifier can be done by clicking the "REMOVE" button :material-numeric-2-circle-outline:. Add a license from the
    list :material-numeric-3-circle-outline: fitting to your interests and optionally provide the main language for the
    identifier :material-numeric-4-circle-outline: (c.f. Figure 25). This helps machines to understand the context of
    your data.

    <figure markdown>
    ![Related identifiers, license and language of the identifier](../images/screenshots/assign-database-pid-step-5.png){ .img-border }
    <figcaption>Figure 25: Related identifiers, license and language of the identifier.</figcaption>
    </figure>

    Optionally add funding information, again the [Metadata Service](./system-services-metadata) 
    automatically resolves external PIDs, the easiest way is to provide the correct mandatory data is by filling the
    funder identifier :material-numeric-1-circle-outline: that attempts to get the funder
    name :material-numeric-2-circle-outline:. If you provide an award number :material-numeric-3-circle-outline: and/or
    award title :material-numeric-4-circle-outline:, this information will be represented also in human-readable
    language on the identifier summary page (c.f. Figure 26).

    <figure markdown>
    ![Identifier funder information](../images/screenshots/assign-database-pid-step-6.png){ .img-border }
    <figcaption>Figure 26: Identifier funder information.</figcaption>
    </figure>

    Scroll to the top again and click the ":material-content-save-outline: CREATE PID" button to create the PID. The
    result is displayed in Figure 27.

    <figure markdown>
    ![Identifier summary page](../images/screenshots/assign-database-pid-step-7.png){ .img-border }
    <figcaption>Figure 27: Identifier summary page.</figcaption>
    </figure>

=== "Terminal"

    Create a persistent identifier for a database where you are the owner (this is the case for self-created databases)
    using the HTTP API:

    ```bash
    curl -sSL \
      -X POST \
      -H "Content-Type: application/json" \
      -u "foo:bar" \
      -d '{"type": "database", "titles": [{"title": "Danube water level measurements", "language": "en"},{"title": "Donau Wasserstandsmessungen", "language": "de", "type": "TranslatedTitle"}], "descriptions": [{"description": "This dataset contains hourly measurements of the water level in Vienna from 1983 to 2015", "language": "en", "type": "Abstract"}], "funders": [{ "funder_name": "Austrian Science Fund", "funder_identifier": "https://doi.org/10.13039/100000001", "funder_identifier_type": "Crossref Funder ID", "scheme_uri": "http://doi.org/"}], "licenses": [{"identifier": "CC-BY-4.0", "uri": "https://creativecommons.org/licenses/by/4.0/legalcode"}], "publisher": "Example University", "creators": [{"firstname": "Martin", "lastname": "Weise", "affiliation": "TU Wien", "creator_name": "Weise, Martin", "name_type": "Personal", "name_identifier": "0000-0003-4216-302X", "name_identifier_scheme": "ORCID", "affiliation_identifier": "https://ror.org/04d836q62", "affiliation_identifier_scheme": "ROR"}], "database_id": 1, "publication_day": 16, "publication_month": 1, "publication_year": 2024, "related_identifiers": [{"value": "10.5334/dsj-2022-004", "type": "DOI", "relation": "Cites"}]}' \
      http://<hostname>/api/identifier | jq .id
    ```

=== "JDBC"

    !!! warning

        Creating a PID directly in the [Metadata Database](./system-databases-metadata) is not recommended! It bypasses
        validation and creation of external PIDs (e.g. DOI) and may lead to inconstistent data locally compared to
        external systems (e.g. DataCite Fabrica).

    Create a local PID directly in the [Metadata Database](./system-databases-metadata) by filling the tables in this
    order (they have foreign key dependencies).

    1. `mdb_identifiers` ... identifier core information
        * `mdb_identifier_creators` ... identifier creator list
        * `mdb_identifier_titles` ... identifier creator list
        * `mdb_identifier_descriptions` ... identifier description list
        * `mdb_identifier_funders` ... identifier funder list
        * `mdb_identifier_licenses` ... identifier license list
        * `mdb_related_identifiers` ... related identifier list

=== "Python"

    Create a persistent identifier for a database where you are the owner (this is the case for self-created databases)
    using the HTTP API:

    ```python
    from dbrepo.RestClient import RestClient
    from python.dbrepo.api.dto import IdentifierType, CreateIdentifierFunder, CreateIdentifierDescription, \
        CreateIdentifierTitle, Identifier, CreateIdentifierCreator, CreateRelatedIdentifier, RelatedIdentifierType, \
        RelatedIdentifierRelation
    
    client = RestClient(endpoint="http://<hostname>", username="foo",
                        password="bar")
    identifier = client.create_identifier(
        database_id=1,
        type=IdentifierType.DATABASE,
        creators=[CreateIdentifierCreator(
            creator_name="Weise, Martin",
            name_identifier="https://orcid.org/0000-0003-4216-302X")],
        titles=[CreateIdentifierTitle(
            title="Danube river water measurements")],
        descriptions=[CreateIdentifierDescription(
            description="This dataset contains hourly measurements of the water \
             level in Vienna from 1983 to 2015")],
        funders=[CreateIdentifierFunder(
            funder_name="Austrian Science Fund",
            funder_identifier="https://doi.org/10.13039/100000001")],
        licenses=[Identifier(identifier="CC-BY-4.0")],
        publisher="TU Wien",
        publication_year=2024,
        related_identifiers=[CreateRelatedIdentifier(
            value="https://doi.org/10.5334/dsj-2022-004",
            type=RelatedIdentifierType.DOI,
            relation=RelatedIdentifierRelation.CITES)])
    print(f"identifier id: {identifier.id}")
    ```

## Private Database &amp; Access

A user wants a public database to be private and only give specific users access.

=== "UI"

    Login and select a database where you are the owner (this is the case for e.g. self-created databases). Click 
    the "SETTINGS" tab :material-numeric-1-circle-outline: and set the visibility to `Private`
    :material-numeric-2-circle-outline:, then click the "MODIFY VISIBILITY" button to apply the changes.

    An overview of users who have access to this database is given in :material-numeric-3-circle-outline: and can be
    changed or revoked at any time by the owner. To give a user account access, click the "GIVE ACCESS" button to open
    the dialog (c.f. Figure 28).

    <figure markdown>
    ![Database settings for visibility and access](../images/screenshots/private-database-access-step-1.png){ .img-border }
    <figcaption>Figure 28: Database settings for visibility and access.</figcaption>
    </figure>

    Give a user from the list access to the database by selecting the qualified username from the 
    list :material-numeric-1-circle-outline: and the access :material-numeric-2-circle-outline:, e.g. `read` for
    allowing users to view the private data (c.f. Figure 29).

    <figure markdown>
    ![Database acccess dialog](../images/screenshots/private-database-access-step-2.png){ .img-border }
    <figcaption>Figure 29: Database acccess dialog.</figcaption>
    </figure>

=== "Terminal"

    To change the visibility of a database where you are the owner (this is the case for self-created databases), send
    a request to the HTTP API:

    ```bash
    curl -sSL \
      -X PUT \
      -H "Content-Type: application/json" \
      -u "foo:bar" \
      -d '{"is_public": true}' \
      http://<hostname>/api/database/1/visibility
    ```

    To give a user (with id `e9bf38a0-a254-4040-87e3-92e0f09e29c8` access to this database (e.g. read access), update
    their access using the HTTP API:

    ```bash
    curl -sSL \
      -X POST \
      -H "Content-Type: application/json" \
      -u "foo:bar" \
      -d '{"type": "read"}' \
      http://<hostname>/api/database/1/access/e9bf38a0-a254-4040-87e3-92e0f09e29c8
    ```

    In case the user already has access, use the method `PUT`.

=== "JDBC"

    To change the visibility of a database as administrator with direct JDBC access to 
    the [Metadata Database](./system-databases-metadata), change the visibility directly by executing the SQL-query
    in the `fda` schema:

    ```sql
    UPDATE `fda`.`mdb_databases` SET `is_public` = TRUE;
    ```

    To give a user (with id `e9bf38a0-a254-4040-87e3-92e0f09e29c8` access to this database (e.g. read access), update
    their access using the JDBC API:

    ```sql
    INSERT INTO `fda`.`mdb_have_access` (`user_id`, `database_id`, `access_type`)
        VALUES ('e9bf38a0-a254-4040-87e3-92e0f09e29c8', 1, 'READ');
    ```

    In case the user already has access, use an `UPDATE` query.

=== "Python"

    To change the visibility of a database where you are the owner (this is the case for self-created databases), send
    a request to the HTTP API:

    ```python
    from dbrepo.RestClient import RestClient

    client = RestClient(endpoint="http://<hostname>", username="foo",
                        password="bar")
    client.update_database_visibility(<database_id>,
                                      is_public=True,
                                      is_schema_public=False,
                                      is_dashboard_enabled=True)
    ```

    To give a user (with id `e9bf38a0-a254-4040-87e3-92e0f09e29c8` access to this database (e.g. read access), update
    their access using the HTTP API:

    ```python
    from dbrepo.RestClient import RestClient
    from python.dbrepo.api.dto import AccessType
    
    client = RestClient(endpoint="http://<hostname>", username="foo",
                        password="bar")
    client.create_database_access(<database_id>,
                                  type=AccessType.READ,
                                  user_id="e9bf38a0-a254-4040-87e3-92e0f09e29c8")
    ```

    In case the user already has access, use the method `update_database_access` or revoke `delete_database_access`.
