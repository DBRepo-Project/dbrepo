from dbrepo.RestClient import RestClient
from python.dbrepo.api.dto import AccessType

client = RestClient(endpoint="https://test.dbrepo.tuwien.ac.at", username="foo",
                    password="bar")
client.delete_database_access()
