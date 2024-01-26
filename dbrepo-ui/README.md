# User Interface

## Prerequisites

* Node.js 14+ ([how to install](https://www.stewright.me/2021/03/install-nodejs-14-on-ubuntu-20-04/))
* Yarn 1.22.0+ ([how to install](https://classic.yarnpkg.com/lang/en/docs/install/#debian-stable))

Install the runtime dependencies into `node_modules`:

```bash
yarn install
```

## Run

Then, start a local development server at port 3001. The development server has a local proxy that rewrites the paths
and does not rely on the `gateway-service` (a NGINX-based proxy to bundle the REST API).

```bash
yarn dev
```

Visit [http://localhost:3001](http://localhost:3001) in your browser. The development server watches for changes in
`dbrepo-ui` and will reload the frontend.

## Configure

To change most display settings, modify the `dbrepo.config.json` in the root folder. Extend it for any configuration
that the user needs to do, e.g. change the title, logo, display a list of links.

## Test

TO run the unit tests:

```bash
yarn run test:unit
```

Optionally, generate a coverage report:

```bash
yarn run coverage
```