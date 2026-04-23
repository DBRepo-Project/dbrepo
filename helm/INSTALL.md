# Base

1. CertManager

    Bundle:

    ```shell
    kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.20.2/cert-manager.yaml
    ```
   
    When ready, the Let's Encrypt cluster issuer definition:
    
    ```shell
    kubectl apply -f cluster-issuers.yaml
    ```

2. Cloud Native PG

    Cluster operator:

    ```shell
    kubectl apply --server-side -f https://raw.githubusercontent.com/cloudnative-pg/cloudnative-pg/release-1.29/releases/cnpg-1.29.0.yaml
    ```

3. RabbitMQ

    Cluster operator:

    ```shell
    kubectl apply -f https://github.com/rabbitmq/cluster-operator/releases/latest/download/cluster-operator.yml
    ```

    Then the topology operator:

    ```shell
    kubectl apply -f https://github.com/rabbitmq/messaging-topology-operator/releases/latest/download/messaging-topology-operator-with-certmanager.yaml
    ```