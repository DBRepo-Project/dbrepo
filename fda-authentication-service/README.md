# Authentication Service

Attention: self-enrollment is not possible anymore (Keycloak)

## Create Users

- Visit [localhost:8443](https://localhost:8443) and login with default admin credentials `keycloak:keycloak`
- Select realm `dbrepo`
- Visit `Users` -> `Create` and set a non-temporary password

## API

### Create Token

```console
curl -X POST -H 'Content-Type: application/x-www-form-urlencoded' -d '{"client_id":"dbrepo-client","username":"ABC","password":"XYZ","client_secret":"123","grant_type":"password","scope":"openid"}'
```
