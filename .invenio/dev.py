#!/usr/bin/env python3
import time
import uuid

from api_authentication.api.authentication_endpoint_api import AuthenticationEndpointApi
from api_authentication.api.user_endpoint_api import UserEndpointApi
from api_container.api.container_endpoint_api import ContainerEndpointApi
from api_database.api.container_database_endpoint_api import ContainerDatabaseEndpointApi
from api_analyse.api.analyse_endpoint_api import AnalyseEndpointApi

authentication = AuthenticationEndpointApi()
user = UserEndpointApi()
container = ContainerEndpointApi()
database = ContainerDatabaseEndpointApi()
analyse = AnalyseEndpointApi()

# # Create account
# response = user.register({
#     'username': 'mweise',
#     'password': 'fda',
#     'email': 'martin.weise@tuwien.ac.at'
# })
# print('Created account with username %s' % response.username)

# Create authentication
response = authentication.authenticate_user1({
    'username': 'mweise',
    'password': 'fda'
})
container.api_client.default_headers = {
    'Authorization': 'Bearer ' + response.token
}
database.api_client.default_headers = {
    'Authorization': 'Bearer ' + response.token
}

# # Create container
# response = container.create1({
#     'name': 'MIR ' + str(uuid.uuid1()),
#     'repository': 'mariadb',
#     'tag': '10.5'
# })
# cid = response.id
# print('Created container with id %d' % cid)
#
# # Start container
# response = container.modify({
#     'action': 'START'
# }, cid)
# time.sleep(5)
# print('Started container with id %d' % cid)
#
# # Create database
# response = database.create({
#     'name': 'MIR ' + str(uuid.uuid1()),
#     'description': 'Music Information Retrieval',
#     'is_public': True
# }, cid)
# dbid = response.id
# print('Created database with id %d' % dbid)

# Analyse Table
response = analyse.api_analyse_determinedt_post({
    'filepath': '/tmp/test.csv',
})
print('Determined data types')
print(response)
