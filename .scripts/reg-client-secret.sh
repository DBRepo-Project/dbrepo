#!/bin/bash
USERNAME=""
PASSWORD=""

fancy () {
  printf "\e[1;34m$1\e[m"
}

printf "This is a utility script to re-generate the client secret of the %s client.\n" $(fancy dbrepo-client)
fancy "Your credentials are never transmitted outside your machine!\n\n"
read -rp "Username: " USERNAME
read -rp "Password: " PASSWORD

# get admin token
ADMIN_ACCESS_TOKEN=$(curl -fsSL -X POST -d "username=${USERNAME}&password=${PASSWORD}&grant_type=password&client_id=admin-cli" http://localhost/api/auth/realms/master/protocol/openid-connect/token | jq -r .access_token)
printf "\nSuccessfully obtained admin token."

# re-generate client secret
SECRET=$(curl -fsSL -X POST -H "Authorization: Bearer ${ADMIN_ACCESS_TOKEN}" http://localhost/api/auth/admin/realms/dbrepo/clients/6b7ef364-4132-4831-b4e2-b6e9e9dc63ee/client-secret | jq -r .value)
printf "\nSuccessfully re-generated client secret: %s" $(fancy $SECRET)
