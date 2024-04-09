from dbrepo.RestClient import RestClient

client = RestClient(endpoint="https://dbrepo1.ec.tuwien.ac.at", username="foo",
                    password="bar")
client.get_licenses()
