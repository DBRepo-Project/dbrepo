---
author: Martin Weise
---

## tl;dr

!!! debug "Debug Information"

    Image: [`registry.datalab.tuwien.ac.at/dbrepo/ui:1.10.4`](https://hub.docker.com/r/dbrepo/ui)

    * Ports: 3000/tcp

    To directly access in Kubernetes (for e.g. debugging), forward the svc port to your local machine:

    ```shell
    kubectl [-n namespace] port-forward svc/ui 3000:80
    ```

The User Interface is configured in the `runtimeConfig` section of the `nuxt.config.ts` file during build time. For the
runtime, you need to override those values through environment variables or by mounting a `.env` file. As a small
example, you can configure the logo :material-numeric-1-circle-outline: below. Make sure you mount the logo 
as image as well, in this example we want to mount a custom logo `my_logo.png` into the container and specify the name.

<figure markdown>
![Architecture of the UI microservice](/infrastructures/dbrepo/1.10/images/screenshots/ui-config-step-1.png)
</figure>

=== "Docker Compose"

    Text values like the title :material-numeric-2-circle-outline: can be configured as well via the Nuxt runtime 
    configuration through single environment variables or `.env` files.
    
    ```yaml title=".env"
    NUXT_PUBLIC_TITLE="My overriden title"
    NUXT_PUBLIC_LOGO="https://mydomain/my_logo.png"
    NUXT_PUBLIC_ICON="https://mydomain/my_favicon.ico"
    ...
    ```

    To work, you need to serve the `my_logo.png` and `my_favicon.ico` from a separate webserver. Note that simply
    copying the files into the Nuxt [`public/`](https://nuxt.com/docs/guide/directory-structure/public) directory will 
    not work as the content length is calculated only during build time. The development 
    team [#19263](https://github.com/nuxt/nuxt/issues/19263) does not plan to fix this.

=== "Kubernetes"

    Text values like the title :material-numeric-2-circle-outline: can be configured as well via the Nuxt runtime
    configuration through adding the logo file as `ConfigMap`.

    ```console
    kubectl [-n namespace] create configmap gateway-service-config \
      --from-file=logo.png
    ```

    Then you need to mount the configmap into the [Gateway Service](/infrastructures/dbrepo/1.10/gateway-service) under `/etc/nginx/assets/assets`.

    ```yaml title="dbrepo-ui-custom.yaml"
    gatewayservice:
      extraVolumes:
        - name: config-map
          configMap:
            name: gateway-service-config
      extraVolumeMounts:
        - name: config-map
          mountPath: /etc/nginx/assets/assets
    ```

    All files mounted that way are accessible through svc/gateway-service:80 (or ingress if you enabled it) with prefix
    `/assets`, e.g. `https://<hostname>/assets/logo.png`. Therefore, set the logo path:


    ```yaml title="values.yaml"
    ui:
      public:
        api:
          logo: "https://<hostname>/assets/logo.png"
    ...
    ```

### Architecture

The server-client architecture of the User Interface is shown below, it is supposed to help debug the
User Interface on development.

<figure markdown>
![Architecture of the UI microservice](/infrastructures/dbrepo/1.10/images/architecture-ui.svg)
</figure>

* Runtime: Node.js 22 LTS
* Builder: [Vite](https://vitejs.dev/)
* Server: [Nuxt.js 3+](https://nuxt.com/)
* Components: [Vue.js 3+](https://vuejs.org/)
* Frontend: [Vuetify 3+](https://vuetifyjs.com/en/)
* State: [Pinia](https://pinia.vuejs.org/)

### Customization

The UI supports adding pages in [Markdown]() format to describe the terms of use, policies and the repository itself.
Inject your content through the environment variables:

* `NUXT_PUBLIC_ABOUT_CONTENT` (`/about`)
* `NUXT_PUBLIC_POLICIES_CONTENT` (`/policies`)
* `NUXT_PUBLIC_TERMS_CONTENT` (`/terms`)

If any of these environment variables are not empty, they will be displayed on the navigation and the content will be
rendered.

## Limitations

* When developing locally, the `axios` module does not parse custom headers (such as `X-Count`, `X-Headers`) and/or
  blocks CORS requests wrongfully.

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](/infrastructures/dbrepo/1.10/contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

## Security

(none)
