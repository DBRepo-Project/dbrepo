#!/bin/bash
USERNAME=""
PASSWORD=""

fancy () {
  printf "\e[1;34m$1\e[m"
}

printf "This is a utility script to re-generate the client secret of the %s client.\n" $(fancy dbrepo-client)
fancy "Your credentials are never transmitted outside your machine!\n\n"

read -rp "Username: " USERNAME
read -rsp "Password: " PASSWORD

# get admin token
ADMIN_ACCESS_TOKEN=$(curl -fsSL -X POST -d "username=${USERNAME}&password=${PASSWORD}&grant_type=password&client_id=admin-cli" http://localhost/api/auth/realms/master/protocol/openid-connect/token | jq -r .access_token)
if [ -z $ADMIN_ACCESS_TOKEN ]; then
  printf "\n\nFailed to obtain admin token, credentials may not be correct."
  exit 1
fi
printf "\n\nSuccessfully obtained admin token."

# re-generate client secret
SECRET=$(curl -fsSL -X POST -H "Authorization: Bearer ${ADMIN_ACCESS_TOKEN}" http://localhost/api/auth/admin/realms/dbrepo/clients/6b7ef364-4132-4831-b4e2-b6e9e9dc63ee/client-secret | jq -r .value)
if [ -z $SECRET ]; then
  printf "\n\nFailed to re-generate client secret."
  exit 1
fi
printf "\nSuccessfully re-generated client secret: %s" $(fancy $SECRET)
