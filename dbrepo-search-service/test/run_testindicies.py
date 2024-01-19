"""
This script spins up docker containers running  an opensearch db with predefined entries.
This is useful e.g. if you want to run tests on the functionality of   the opensearch_client.

note: The port of the test container should be 9200, but it's somehow kinda random,
and using environmet variables also doesn't really work,
so the correct port number is just saved in the .testpickle
"""

from testcontainers.opensearch import OpenSearchContainer
import pprint
import time
import os
import pickle


doc1 = {
    "author": "aaa",
    "name": "Hi! My name is",
    "description":"here's some description text",
    "created": "2023-07-27",
    "docID":1,
    "public":True,
    "details": {
        "nestedObject1": "something",
        "nestedObject2": "something else",
        "evenMoreNested": {
            "bla":"blib",
            "blob":"blub"
        }
    }
}

doc2 = {
    "author": "max",
    "name": "Bla Bla",
    "public": False,
    "description": "here's another description text, about a fictional entry with some random measurement data",
    "created": "2023-07-27",
    "docID":2,
    "details": {
            "nestedObject1": "something",
            "nestedObject2": "something else"
        }
}

doc3 = {
    "author": "mweise",
    "name": "databaseName",
    "public": True,
    "description": "here is a really old entry",
    "created":"2022-07-27",
    "docID":3,
    "details": {
            "nestedObject1": "something",
            "nestedObject2": "something else"
        }
}
placeholderDoc = {
    "blib":"blub",
    "public": False
}

with OpenSearchContainer(port_to_expose=9200) as opensearch:
    client = opensearch.get_client()
    creation_result = client.index(index="database", body=doc1)
    creation_result = client.index(index="database", body=doc2)
    creation_result = client.index(index="database", body=doc3)
    creation_result = client.index(index="user", body=placeholderDoc)
    creation_result = client.index(index="table", body=placeholderDoc)
    creation_result = client.index(index="column", body=placeholderDoc)
    creation_result = client.index(index="identifier", body=placeholderDoc)
    refresh_result = client.indices.refresh(index="database")
    search_result = client.search(index="database", body={"query": {"match_all": {}}})
    pp = pprint.PrettyPrinter(indent=1)
    config = opensearch.get_config()
    os.environ["TEST_OPENSEARCH_HOST"] = config["host"]
    os.putenv("TEST_OPENSEARCH_HOST", config["host"])
    os.environ["TEST_OPENSEARCH_PORT"] = config["port"]
    os.environ["TEST_OPENSEARCH_USERNAME"] = config["user"]
    os.environ["TEST_OPENSEARCH_PASSWORD"] = config["password"]

    pickle_info = {}
    pickle_info["port"] = config["port"]
    pickle_info["host"] = config["host"]
    with open(".testpickle", "ab") as outfile:
        pickle.dump(pickle_info, outfile)
    print(f"serving on port: {config['port']}")
    while True:
        time.sleep(1)

