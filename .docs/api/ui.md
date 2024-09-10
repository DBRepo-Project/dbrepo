---
author: Martin Weise
---

## tl;dr

!!! debug "Debug Information"

    Image: [`registry.datalab.tuwien.ac.at/dbrepo/ui:1.4.5`](https://hub.docker.com/r/dbrepo/ui)

    * Ports: 3000/tcp

The User Interface is configured in the `runtimeConfig` section of the `nuxt.config.ts` file during build time. For the
runtime, you need to override those values through environment variables or by mounting a `.env` file. As a small
example, you can configure the logo :material-numeric-1-circle-outline: in Figure 2. Make sure you mount the logo as
image as well, in this example we want to mount a custom logo `my_logo.png` into the container and specify the name.

<figure markdown>
![Architecture of the UI microservice](../images/screenshots/ui-config-step-1.png){ .img-border }
<figcaption>Figure 1: Architecture of the UI microservice</figcaption>
</figure>

=== "Docker Compose"

    Text values like the version :material-numeric-2-circle-outline: and title :material-numeric-3-circle-outline: can be
    configured as well via the Nuxt runtime configuration through single environment variables or `.env` files.
    
    ```yaml title=".env"
    NUXT_PUBLIC_TITLE="My overriden title"
    NUXT_PUBLIC_LOGO="/my_logo.png"
    NUXT_PUBLIC_ICON="/favicon.ico"
    ...
    ```

    To work, you need to mount the `my_logo.png` file into the `dbrepo-ui` container via the `docker-compose.yml` file.

    ```yaml title="docker-compose.yml"
    services:
      dbrepo-ui:
        image: registry.datalab.tuwien.ac.at/dbrepo/ui:1.4.5
        volumes:
          - ./my_logo.png:/app/.output/public/my_logo.png
          - ./favicon.ico:/app/.output/public/favicon.ico
        environment:
          ...
      ...
    ```

    If you want to override more environment variables, extend the dictionary in `environment:`

=== "Kubernetes"

    Text values like the version :material-numeric-2-circle-outline: and title :material-numeric-3-circle-outline: can be
    configured as well via the Nuxt runtime configuration through setting the variables in the `values.yaml` file.

    ```yaml title="values.yaml"
    ui:
      public:
        logo: "/my_logo.png"
        icon: "/favicon.ico"
      extraVolumes:
        - name: images-map
          configMap:
            name: ui-config
      extraVolumeMounts:
        - name: images-map
          mountPath: /static/
    ```

    To work, you need to mount the `my_logo.png` file into the dbrepo-ui deployment via a ConfigMap and Volumes. For this,
    encode the files in base64 with `cat my_logo.png | base64`.

    ```yaml title="dbrepo-ui-custom.yaml"
    apiVersion: v1
    kind: ConfigMap
    metadata:
      name: ui-config
    binaryData:
      my_logo.png: |
        <base64>
      favicon.ico: |
        <base64>
    ```

### Architecture

The server-client architecture of the User Interface is shown in [Figure 3](#fig3), it is supposed to help debug the
User Interface on development.

<figure id="fig3" markdown>
![Architecture of the UI microservice](../images/architecture-ui.svg)
<figcaption>Figure 2: Architecture of the User Interface</figcaption>
</figure>

* Runtime: [Bun 1+](https://bun.sh/) (preferred), *alternatively* Node.js 18+
* Builder: [Vite](https://vitejs.dev/)
* Server: [Nuxt.js 3+](https://nuxt.com/)
* Components: [Vue.js 3+](https://vuejs.org/)
* Frontend: [Vuetify 3+](https://vuetifyjs.com/en/)
* State: [Pinia](https://pinia.vuejs.org/)

### Example

See the [API Overview](..) page for detailed examples.

## Limitations

(none)

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](../contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

## Security

(none)
