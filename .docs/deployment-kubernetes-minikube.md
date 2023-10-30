---
author: Martin Weise
---

# Special Instructions for Minikube

You can use our Helm chart for deploying DBRepo in your Kubernetes Cluster
using [minikube](https://minikube.sigs.k8s.io/docs/start/) as infrastructure provider which deploys a single-node Kubernetes cluster on your machine, 
suitable for test-deployments.

## Requirements

### Virtual Machine

For this small, local, test deployment any modern hardware would suffice, we recommend a dedicated virtual machine with
the following settings. Note that most of the vCPU and RAM resources will be needed for starting the infrastructure,
this is because of Docker. During idle times, the deployment will use significantly less resources.

- 4 vCPU cores
- 16GB RAM memory
- 200GB SSD storage

### Minikube

First, install the minikube virtualization tool that provides a single-node Kubernetes environment, e.g. on a virtual
machine. We do not regularly check these instructions, they are provided on best-effort. Check 
the [official documentation](https://minikube.sigs.k8s.io/docs/start/) for up-to-date information.

For Debian:

```shell
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube_latest_amd64.deb
sudo dpkg -i minikube_latest_amd64.deb
```

Start the cluster and enable basic plugins:

```shell
minikube start --driver='docker'
minikube kubectl -- get po -A
minikube addons enable ingress
```

### NGINX

Deploy a NGINX reverse proxy on the virtual machine to reach your minikube cluster from the public Internet:

```nginx title="/etc/nginx/conf.d/dbrepo.conf"
resolver 127.0.0.11 valid=30s;

server {
    listen 80;
    server_name _;

    location / {
        proxy_set_header Host            $host;
        proxy_set_header X-Forwarded-For $remote_addr;
        proxy_pass http://CLUSTER_IP;
    }
}

server {
    listen 443 ssl;
    server_name DOMAIN_NAME;
    ssl_certificate     /etc/nginx/certificate.crt;
    ssl_certificate_key /etc/nginx/certificate.key;

    location / {
        proxy_set_header Host            $host;
        proxy_set_header X-Forwarded-For $remote_addr;
        proxy_pass https://CLUSTER_IP;
    }
}
```

Replace `CLUSTER_IP` with the result of:

    $ minikube ip
    192.168.49.2

Replace `DOMAIN_NAME` with the domain name. You will need also a valid TLS certificate with private key for TLS enabled
in the cluster. In our test deployment we obtained a certificate from Let's Encrypt.

### Fileshare

Since the Upload Service uses a shared filesystem with the [Analyst Service](../system-services-analyse),
[Metadata Service](../system-services-metadata) and
[Data Database](../system-databases-data), the dynamic provision of the *PersistentVolume* 
by the *PersistentVolumeClaim* 
of [`pvc.yaml`](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-deployment/-/blob/master/charts/dbrepo-core/templates/upload-service/pvc.yaml)
needs to happen statically. You can make use of the host's filesystem and mount it in each of those deployments.

For example, mount the *hostPath* directly in
the [`deployment.yaml`](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-deployment/-/blob/master/charts/dbrepo-core/templates/analyse-service/deployment.yaml).

```yaml title="deployment.yaml"
apiVersion: apps/v1
kind: Deployment
metadata:
  name: analyse-service
  ...
spec:
  template:
    spec:
      containers:
        - name: analyse-service
      volumeMounts:
        - name: shared
          hostPath: /path/of/host
          mountPath: /mnt/shared
      ...
```

## Deployment

To install the DBRepo Helm Chart, download and edit 
the [`values.yaml`](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-deployment/-/raw/master/charts/dbrepo-minikube/values.yaml?inline=false)
file. At minimum you need to change the values for:

* `hostname`, set to your domain, e.g. `subdomain.example.com`
* `authAdminApiUrl`, similarly but with https and the api to the keycloak server, e.g. `https://subdomain.example.com/api/auth`

It is advised to also change the usernames and passwords for all credentials. Next, install the chart using your edited
`values.yaml` file:

!!! info "Documentation of values.yaml"

    We documented all values in the `values.yaml` file [here](http://127.0.0.1:8000/deployment-helm/#chart-values) with
    default values and description for each value.

```shell
helm upgrade --install dbrepo \
  -n dbrepo \ 
  "oci://dbrepo.azurecr.io/helm/dbrepo-core" \
  --values ./values.yaml \
  --version "0.1.3" \
  --create-namespace \
  --cleanup-on-fail
```
