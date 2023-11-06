---
author: Martin Weise
---

## TL;DR

To install DBRepo in your existing cluster, download the sample [`values.yaml`](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-deployment/-/raw/master/charts/dbrepo-core/values.yaml?inline=false)
for your deployment and update the variables, especially `hostname`. The chart depends on 
installed [Keycloak Operator](https://www.keycloak.org/operator/installation) that can be installed following the 
official guide.

```shell
helm upgrade --install dbrepo \
  -n dbrepo \
  "oci://dbrepo.azurecr.io/helm/dbrepo-core" \
  --values ./values.yaml \
  --version "0.1.3" \
  --create-namespace \
  --cleanup-on-fail
```

## Dependencies

The helm chart depends on four components:

1. [Ingress NGINX Controller](https://kubernetes.github.io/ingress-nginx/) for basic ingress.
2. [Cert-Manager Controller](https://cert-manager.io/) for TLS certificate management with Let's Encrypt.
3. [MariaDB Operator](https://github.com/mariadb-operator/mariadb-operator/) for creation of databases.
4. [Keycloak Operator](https://www.keycloak.org/operator/installation) for creation of the authentication service.

## Configuration before the installation

Define an admin user that the services can use to communicate with 
the [Authentication Service](../system-services-authentication). You will need to manually create this user later after
the installation.

## Configuration after the installation

After installing, get the initial administrator password created by the [Keycloak operator](https://www.keycloak.org/operator/basic-deployment):

```shell
kubectl -n dbrepo \
  get \
  secret \
  auth-service-initial-admin \
  -o jsonpath='{.data.password}' | base64 --decode
```

On success, the output should look like this: `1f5581a01d8e8f47f2dae08cc88f56fd` which is the initial password for the
user `admin`. This password should be considered as *temporary* and be changed immediately now! Login into 
the [authentication service](../system-services-authentication) as `admin` and:

1. Create a new user in the `master` realm.
2. Create credentials (non-temporary) for this user in the `master` realm.
3. Assign this user the role `admin`.
4. Delete the user `admin`.

Then import the DBRepo realm by clicking the dropdown "master" > Create Realm and import 
the [`dbrepo-realm.json`](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/dev/dbrepo-authentication-service/dbrepo-realm.json)
by uploading the file *or* copying the contents and click "Create".

### Backup

tbd

### Restore

tbd

## Limitations

1. MariaDB Galera does not (yet) support XA-transactions required by the authentication service (=Keycloak). Therefore
   only a single MariaDB pod can be deployed at once for the [auth database](../system-databases-auth).

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](../contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!
