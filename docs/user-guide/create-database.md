---
author: Martin Weise
---

A user wants to create a database in DBRepo.

### UI

A database can be created by choosing a name (e.g. My Database), selecting an engine (e.g. MariaDB) and defining the
visibility, this can be [changed later](/infrastructures/dbrepo/1.10/update-visibility).

<video autoplay loop>
  <source src="/infrastructures/dbrepo/1.10/videos/create-database.webm" type="video/webm" />
  <source src="/infrastructures/dbrepo/1.10/videos/create-database.mp4" type="video/mp4" />
</video>

### Python

!!! info "Python Compatibility"

    Ensure that you use the same Python library version as the target instance. For example: if you see `1.10.1` in the
    bottom left, you need to use the `1.10.0` Python library.

List all available containers with their database engine descriptions and obtain a container id.

```python
from dbrepo.RestClient import RestClient

client = RestClient(endpoint="http://<hostname>", username="foo",
                    password="bar")
containers = client.get_containers()
print(containers)
```

Create a public database with the container id from the previous step.

```python
from dbrepo.RestClient import RestClient

client = RestClient("http://<hostname>", username="foo", password="bar")
database = client.create_database("My Database", 
                                  "6cfb3b8e-1792-4e46-871a-f3d103527203",
                                  is_public=False,
                                  is_schema_public=False)
print(f"database id: {database.id}")
```